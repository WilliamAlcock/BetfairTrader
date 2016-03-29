package csvReader

import catalogueBuilder.MarketDocumentUtils
import dbIO.DBIO
import indicators.Indicators
import org.joda.time.DateTime
import play.api.libs.json.JsObject
import randomForest.{ExtremelyRandomForest, Instance, CrossValidator}
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class CSVReader extends CSVDataUtils with MarketDocumentUtils with IntervalFactory with CrossValidator {

  val dbIO = new DBIO()

  private def onMarkets(year: String, marketOpp: (String, String) => Unit) = {
    Await.result(dbIO.listMarkets(year), Duration.Inf) match {
      case x =>
        println("#markets: " + x.size)
        // TODO extend this line to exclude any collection that doesn't match the format MM_dd_
        x.dropWhile(Set("system.indexes").contains).foreach(market => marketOpp(year, market))
    }
  }

  def readFile(path: String) = {
    var succeeded = 0
    var failed = 0
    scala.io.Source.fromFile(path).getLines().foreach { x =>
      Await.result(dbIO.writeCSVData(fromCSVString(x)), Duration.Inf) match {
        case x: WriteResult if x.ok => succeeded += 1
        case _ => failed += 1
      }
      print("writing Lines - #Success: " + succeeded + " #Failed: " + failed + "\r")
    }
  }

  // TODO this function should be called after readFile
  def buildIndexes(dbName: String) = {
    var succeeded = 0
    var failed = 0
    onMarkets(dbName, (year: String, market: String) => {
      val col = dbIO.getCollection(year, market)
      Await.result(Future.sequence(List(
        dbIO.createIndex(col, Index(Seq("timestamp" -> IndexType.Ascending))),
        dbIO.createIndex(col, Index(Seq("selectionId" -> IndexType.Ascending)))
      )), Duration.Inf) match {
        case x if x.forall(_.ok) => succeeded += 1
        case _ => failed += 1
      }
      print("Building Indexes - #Success: " + succeeded + " #Failed: " + failed + "\r")
    })
  }

  def buildCatalogues(dbName: String) = {
    var succeeded = 0
    var failed = 0
    var noData = 0
    onMarkets(dbName, (year: String, market: String) => {
      val col = dbIO.getCollection(year, market)
      dbIO.getMarketCatalogue(col) match {
        case Some(catalogue) => Await.result(dbIO.writeMarketCatalogue(catalogue), Duration.Inf) match {
          case x: WriteResult => succeeded += 1
          case _ => failed += 1
        }
        case _ => noData += 1
      }
      print("Writing Catalogues - #Success: " + succeeded + " #Failed: " + failed + " #No Data: " + noData + "\r")
    })
  }

  def getPostFix(interval: Long): String = "_i_" + interval + "_ticks_w"

  def writeIndicators(year: String, interval: Long /* Milliseconds */) = {
    var totalSelections = 0
    var totalIntervals = 0
    var totalMarkets = 0
    val lookup = getLookup()

    onMarkets(year, (year: String, market: String) => {
      if (lookup.contains(getMarket(market))) {
        val col = dbIO.getCollection(year, market)
        val csvData = Await.result(col.find(BSONDocument()).sort(BSONDocument("timestamp" -> 1)).cursor[CSVData]().collect[List](), Duration.Inf)

        var marketSelections = 0
        var marketIntervals = 0

        csvData.groupBy(_.selectionId).foreach { case (selectionId, data) => {
          if (lookup(getMarket(market)).contains(selectionId)) {
            marketSelections += 1
            Await.result(Future.sequence(indicatorsFromCSVData(
              data,
              getIntervalStartTime(data.head.timestamp.getMillis, data.head.startTime.getMillis, interval),
              interval,
              getTicks
            ).map(x => dbIO.writeIndicators(x, year, market, getPostFix(interval)))), Duration.Inf) match {
              case x if x.forall(_.ok) =>
                marketIntervals += x.size
                println("Writing Indicators for market: " + market + " selection: " + selectionId + " SUCCEEDED intervals: " + x.size)
              case _ => println("Writing Indicators for market: " + market + " selection: " + selectionId + " FAILED")
            }
          }
        }}
        println("MARKET: " + market + " #selections: " + marketSelections)
        totalSelections += marketSelections
        totalIntervals += marketIntervals
        totalMarkets += 1
      }
    })
    println("TOTAL MARKETS: " + totalMarkets + " TOTAL SELECTIONS: " + totalSelections + " TOTAL INTERVALS: " + totalIntervals)
  }

  private def isInplay(i: Indicators): Boolean = i.timestamp >= i.startTime

  private def allFeaturesDefined(i: Indicators): Boolean = Indicators.getFeatures(i).forall(_.isDefined)

  private def isValid(i: Indicators, inplay: Boolean, minsBefore: Option[Int]): Boolean = (inplay, minsBefore) match {
    case (true, Some(x)) => allFeaturesDefined(i) && i.timestamp >= (i.startTime - (x * 60000))
    case (false, Some(x)) => allFeaturesDefined(i) && i.timestamp >= (i.startTime - (x * 60000)) && !isInplay(i)
    case (false, None) => allFeaturesDefined(i) && !isInplay(i)
    case (true, None) => allFeaturesDefined(i)
  }

  def buildDataSet(db: String, markets: List[String], saveName: String, inplay: Boolean, minsBefore: Option[Int]): Unit = {
    var totalMarkets = 0
    var totalSelections = 0
    var totalIntervals = 0
    markets.map(market => {
      var marketSelections = 0
      var marketIntervals = 0

      val indicators: Map[Long, List[Indicators]] = Await.result(dbIO.getCollection(db, market).find(BSONDocument()).sort(BSONDocument("timestamp" -> -1)).cursor[JsObject]().collect[List]().map(x => {
        x.map(_.validate[Indicators].get).filter(isValid(_, inplay, minsBefore)).groupBy(_.selectionId)
      }), Duration.Inf)

      indicators.foreach{case (selection, indicators) =>
        marketSelections += 1
        val instances = indicatorsToInstances(indicators)
        marketIntervals += instances.size
        println("Writing Indicators for market: " + market + " selection: " + selection + " SUCCEEDED intervals: " + instances.size)
        instances.map(x => {
          dbIO.writeInstance(x, db, saveName)
        })
      }
      println("MARKET: " + market + " #selections: " + marketSelections + " #intervals " + marketIntervals)
      totalSelections += marketSelections
      totalIntervals += marketIntervals
      totalMarkets += 1
    })
    println("DATASET: " + saveName + " #markets " + totalMarkets + " #selections " +  totalSelections + " #totalIntervals " + totalIntervals)
  }

  def getMarket(s: String): String = s.substring(6)

  def buildDataSet(db: String, markets: List[String], saveName: String, inplay: Boolean, minsBefore: Option[Int], lookup: Map[String, Set[Long]]): Unit = {
    var totalMarkets = 0
    var totalSelections = 0
    var totalIntervals = 0

    markets.filter(x => lookup.contains(getMarket(x))).map(market => {
      var marketSelections = 0
      var marketIntervals = 0

      val selections = lookup(getMarket(market))

      val indicators: Map[Long, List[Indicators]] = Await.result(dbIO.getCollection(db, market).find(BSONDocument()).sort(BSONDocument("timestamp" -> -1)).cursor[JsObject]().collect[List]().map(x => {
        x.map(_.validate[Indicators].get).filter(x => selections.contains(x.selectionId) && isValid(x, inplay, minsBefore)).groupBy(_.selectionId)
      }), Duration.Inf)

      indicators.foreach{case (selection, _indicators) =>
        marketSelections += 1
        val instances = indicatorsToInstances(_indicators)
        marketIntervals += instances.size
        println("Writing Indicators for market: " + market + " selection: " + selection + " SUCCEEDED intervals: " + instances.size)
        instances.map(x => {
          dbIO.writeInstance(x, db, saveName)
        })
      }
      println("MARKET: " + market + " #selections: " + marketSelections + " #intervals " + marketIntervals)
      totalSelections += marketSelections
      totalIntervals += marketIntervals
      totalMarkets += 1
    })
    println("DATASET: " + saveName + " #markets " + totalMarkets + " #selections " +  totalSelections + " #totalIntervals " + totalIntervals)
  }


  def getLookup(): Map[String, Set[Long]] = {
    val catalogues = dbIO.getCollection("2014", "runner_catalogues")

    val query = BSONDocument("runner.totalMatched" -> BSONDocument("$gte" -> 50000))

    val data = Await.result(catalogues.find(query).cursor[JsObject]().collect[List](), Duration.Inf)

    data.map(x =>
      ((x \ "marketId").get.as[String], (x \ "runner" \ "selectionId").get.as[Long])
    ).groupBy(x => x._1).mapValues(x => x.map(_._2).toSet)
  }

  def buildTrainingAndTestSets(db: String, trainingStr: String, testingStr: String, inplay: Boolean, minutesBefore: Option[Int] = None) = {
    val markets = Await.result(dbIO.listMarkets(db), Duration.Inf)

    buildDataSet(db, markets.filter(_.startsWith(trainingStr)), "training", inplay, minutesBefore)
    buildDataSet(db, markets.filter(_.startsWith(testingStr)), "testing", inplay, minutesBefore)
  }

  def trainClassifier(dbName: String, collectionName: String, leafSize: Int, features: Int, numberOfTrees: Int) = {
    val col = dbIO.getCollection(dbName, collectionName)
    val data = Await.result(col.find(BSONDocument()).cursor[JsObject]().collect[List](), Duration.Inf).map(_.as[Instance])

    println("#trees: " + numberOfTrees + ", leafSize: " + leafSize + ", #features: " + features + ", startTime: " + DateTime.now())
    crossValidateForest(numberOfTrees = numberOfTrees, leafSize = leafSize, numberOfFeatures = features, new ExtremelyRandomForest(), data)
    println("endTime " + DateTime.now())
  }
}

//object CSVReader extends App with CrossValidator {
//  new CSVReader().writeIndicators("2014", 60000)

//  new CSVReader().buildTrainingAndTestSets("2014_i_60000_io", "10_", "11_", inplay = false, Some(20))
//  val dbIO = new DBIO()
//
//  val markets = Await.result(dbIO.listMarkets("2014"), Duration.Inf)

//  val col = dbIO.getCollection("2014_i_60000_io", "training")
//  val data = Await.result(col.find(BSONDocument()).cursor[JsObject]().collect[List](), Duration.Inf).map(_.as[Instance])
//
//  List(500, 600, 700, 800, 900, 1000).foreach(numberOfTrees => {
//    println("#trees: " + numberOfTrees + ", leafSize: 2, #features: 3, startTime: " + DateTime.now())
//    crossValidateForest(numberOfTrees = numberOfTrees, leafSize = 2, numberOfFeatures = 3, new ExtremelyRandomForest(), data)
//    println("endTime " + DateTime.now())
//  })
//}