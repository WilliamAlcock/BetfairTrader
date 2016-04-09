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

  def getPostFix(interval: Long, postfix: String): String = "_i_" + interval + "_" + postfix

  def writeIndicators(year: String, interval: Long /* Milliseconds */, postfix: String) = {
    var totalSelections = 0
    var totalIntervals = 0
    var totalMarkets = 0

    onMarkets(year, (year: String, market: String) => {
      val col = dbIO.getCollection(year, market)
      // read market data from mongodb, sort by timestamp
      val csvData = Await.result(col.find(BSONDocument()).sort(BSONDocument("timestamp" -> 1)).cursor[CSVData]().collect[List](), Duration.Inf)

      var marketSelections = 0
      var marketIntervals = 0

      csvData.groupBy(_.selectionId).foreach { case (selectionId, data) => {
        marketSelections += 1
        Await.result(Future.sequence(indicatorsFromCSVData(
          data,
          getIntervalStartTime(data.head.timestamp.getMillis, data.head.startTime.getMillis, interval),
          interval,
          probLose//(x: Double) => x
        ).map(x => dbIO.writeIndicators(x, year, market, getPostFix(interval, postfix)))), Duration.Inf) match {
          case x if x.forall(_.ok) =>
            marketIntervals += x.size
            println("Writing Indicators for market: " + market + " selection: " + selectionId + " SUCCEEDED intervals: " + x.size)
          case _ => println("Writing Indicators for market: " + market + " selection: " + selectionId + " FAILED")
        }
      }}
      println("MARKET: " + market + " #selections: " + marketSelections)
      totalSelections += marketSelections
      totalIntervals += marketIntervals
      totalMarkets += 1
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

  def getLookup(): Map[String, Set[Long]] = {
    val catalogues = dbIO.getCollection("2014", "runner_catalogues")

    val query = BSONDocument("runner.totalMatched" -> BSONDocument("$gte" -> 50000))

    val data = Await.result(catalogues.find(query).cursor[JsObject]().collect[List](), Duration.Inf)

    data.map(x =>
      ((x \ "marketId").get.as[String], (x \ "runner" \ "selectionId").get.as[Long])
    ).groupBy(x => x._1).mapValues(x => x.map(_._2).toSet)
  }

  def buildTrainingAndTestSets(db: String, trainingStr: String, testingStr: String, postfix: String, inplay: Boolean, minutesBefore: Option[Int] = None) = {
    val markets = Await.result(dbIO.listMarkets(db), Duration.Inf)

    println("Settings: inplay = ", inplay, " minsBefore = ", minutesBefore)

    buildDataSet(db, markets.filter(_.startsWith(trainingStr)), "training" + postfix, inplay, minutesBefore)
    buildDataSet(db, markets.filter(_.startsWith(testingStr)), "testing" + postfix, inplay, minutesBefore)
  }

  private def loadDataSet(db: String, col: String) = {
    Await.result(dbIO.getCollection(db, col).find(BSONDocument()).cursor[JsObject]().collect[List](), Duration.Inf).map(_.as[Instance])
  }

  def crossValidateClassifier(dbName: String, collectionName: String, leafSize: Int, features: Int, numberOfTrees: Int) = {
    val data = loadDataSet(dbName, collectionName)

 //   List(9, 3, 4, 5, 6, 7, 8).foreach(_leafSize => {
      println("#trees: " + numberOfTrees + ", leafSize: " + leafSize + ", #features: " + features + ", startTime: " + DateTime.now())
      crossValidateForest(numberOfTrees = numberOfTrees, leafSize = leafSize, numberOfFeatures = features, new ExtremelyRandomForest(), data)
      println("endTime " + DateTime.now())
   // })
  }

  def trainAndTestClassifier(dbName: String, trainingCollection: String, testingCollection: String, leafSize: Int, features: Int, numberOfTrees: Int) = {

//    List(2, 3, 4, 5, 6, 7).foreach(_features => {
      println("#trees: " + numberOfTrees + ", leafSize: " + leafSize + ", #features: " + features + ", startTime: " + DateTime.now())
      val forest = new ExtremelyRandomForest().trainForest(numberOfTrees, leafSize, features, loadDataSet(dbName, trainingCollection))
      println(testForest(forest, loadDataSet(dbName, testingCollection)))
      println("endTime " + DateTime.now())
//    })
//    List(50, 100, 200, 500, 1000).foreach(_trees => {
//      println("#trees: " + _trees + ", leafSize: " + leafSize + ", #features: " + features + ", startTime: " + DateTime.now())
//      val forest = new ExtremelyRandomForest().trainForest(_trees, leafSize, features, loadDataSet(dbName, trainingCollection))
//      println(testForest(forest, loadDataSet(dbName, testingCollection)))
//      println("endTime " + DateTime.now())
//    })
//    val forest = new ExtremelyRandomForest().trainForest(numberOfTrees, leafSize, features, loadDataSet(dbName, trainingCollection))
    //
  }

}
