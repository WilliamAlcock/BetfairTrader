package dbIO

import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

import backTester.PnLPredictor
import indicators.Indicators
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}
import randomForest.{DecisionTree, ExtremelyRandomForest, Instance, RandomForest}
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.reflect.io.{Directory, Path}

trait DataUtils extends DBIO with PnLPredictor {

  val _Instance = Instance
  val _CSVData = CSVData

  private def onMarkets(year: String, marketOpp: (String, String) => Unit) = {
    Await.result(listMarkets(year), Duration.Inf) match {
      case x =>
        println("#markets: " + x.size)
        // TODO extend this line to exclude any collection that doesn't match the format MM_dd_
        x.dropWhile(Set("system.indexes").contains).foreach(market => marketOpp(year, market))
    }
  }

  def readFileToDB(path: String) = {
    var succeeded = 0
    var failed = 0
    scala.io.Source.fromFile(path).getLines().foreach { x =>
      Await.result(writeCSVData(_CSVData.fromCSVString(x)), Duration.Inf) match {
        case x: WriteResult if x.ok => succeeded += 1
        case _ => failed += 1
      }
      print("writing Lines - #Success: " + succeeded + " #Failed: " + failed + "\r")
    }
  }

  def buildIndexes(dbName: String) = {
    var succeeded = 0
    var failed = 0
    onMarkets(dbName, (year: String, market: String) => {
      val col = getCollection(year, market)
      Await.result(Future.sequence(List(
        createIndex(col, Index(Seq("timestamp" -> IndexType.Ascending))),
        createIndex(col, Index(Seq("selectionId" -> IndexType.Ascending)))
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
      Await.result(getCollection(year, market).find(BSONDocument()).cursor[CSVData]().collect[List](), Duration.Inf) match {
        case csvData: List[CSVData] => Await.result(writeMarketCatalogue(CSVData.getMarketCatalogue(csvData)), Duration.Inf) match {
          case x: WriteResult => succeeded += 1
          case _ => failed += 1
        }
        case _ => noData += 1
      }
      print("Writing Catalogues - #Success: " + succeeded + " #Failed: " + failed + " #No Data: " + noData + "\r")
    })
  }

  def getCollectionPostFix(interval: Long, postfix: String): String = "_i_" + interval + "_" + postfix

  /*
  Examples:
  price,        output
  1.1           1 - 0.9091  = 0.0909
  1.5           1 - 0.66    = 0.33
  3             1 - 0.33    = 0.66
  10            1 - 0.10    = 0.90


  normalised price is the 100 - percentage probability
  normalised price will range 0 -> 100
 */
  def priceToProbabilityOfLose(price: Double): Double = if (price == 0) price else 1 - (1/price)

  def getIntervalStartTime(timestamp: Long, raceStartTime: Long, interval: Long) = {
    timestamp - raceStartTime match {
      case x if x > 0 => timestamp - (x % interval)                   // after (inplay)
      case x if x < 0 => timestamp - (x % interval) - interval        // before (pre-race)
      case _ => timestamp
    }
  }

  def writeIntervals(year: String, interval: Long /* Milliseconds */, postfix: String) = {
    var totalSelections = 0
    var totalIntervals = 0
    var totalMarkets = 0
    println("I am here")
    onMarkets(year, (year: String, market: String) => {
      val col = getCollection(year, market)
      // read market data from mongodb, sort by timestamp
      val csvData = Await.result(col.find(BSONDocument()).sort(BSONDocument("timestamp" -> 1)).cursor[CSVData]().collect[List](), Duration.Inf)

      var marketSelections = 0
      var marketIntervals = 0

      csvData.groupBy(_.selectionId).foreach { case (selectionId, data) => {
        marketSelections += 1
        Await.result(Future.sequence(CSVData.toIndicators(
          data,
          getIntervalStartTime(data.head.timestamp.getMillis, data.head.startTime.getMillis, interval),
          interval,
          priceToProbabilityOfLose
        ).map(x => writeIndicators(x, year, market, getCollectionPostFix(interval, postfix)))), Duration.Inf) match {
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

  private def isIntervalSelected(i: Indicators, inplay: Boolean, minsBefore: Option[Int]): Boolean = (inplay, minsBefore) match {
    case (true, Some(x)) => Indicators.getFeatures(i).forall(_.isDefined) && i.timestamp >= (i.startTime - (x * 60000))
    case (false, Some(x)) => Indicators.getFeatures(i).forall(_.isDefined) && i.timestamp >= (i.startTime - (x * 60000)) && i.timestamp < i.startTime
    case (false, None) => Indicators.getFeatures(i).forall(_.isDefined) && i.timestamp < i.startTime
    case (true, None) => Indicators.getFeatures(i).forall(_.isDefined)
  }

  def buildDataSet(db: String, collectionsPrefix: String, outputName: String, inplay: Boolean, minsBefore: Option[Int]): Unit = {
    var totalMarkets = 0
    var totalSelections = 0
    var totalIntervals = 0

    Await.result(listMarkets(db), Duration.Inf).filter(_.startsWith(collectionsPrefix)).map(market => {
      var marketSelections = 0
      var marketIntervals = 0

      Await.result(getCollection(db, market).find(BSONDocument()).sort(BSONDocument("timestamp" -> -1)).cursor[JsObject]().collect[List]().map(x => {
        x.map(_.validate[Indicators].get).filter(isIntervalSelected(_, inplay, minsBefore)).groupBy(_.selectionId)
      }), Duration.Inf).foreach{case (selection, selectionIndicators) =>
        marketSelections += 1
        val instances = _Instance.fromIndicators(selectionIndicators)
        marketIntervals += instances.size
        println("Writing Indicators for market: " + market + " selection: " + selection + " SUCCEEDED intervals: " + instances.size)
        instances.map(x => writeInstance(x, db, outputName))
      }
      println("MARKET: " + market + " #selections: " + marketSelections + " #intervals " + marketIntervals)
      totalSelections += marketSelections
      totalIntervals += marketIntervals
      totalMarkets += 1
    })
    println("DATASET: " + outputName + " #markets " + totalMarkets + " #selections " +  totalSelections + " #totalIntervals " + totalIntervals)
  }

  private def loadDataSet(db: String, col: String) = {
    Await.result(getCollection(db, col).find(BSONDocument()).cursor[JsObject]().collect[List](), Duration.Inf).map(_.as[Instance])
  }

  def saveModel(modelName: String, model: RandomForest): Unit = {
    val p = Path(modelName)
    p.createDirectory()
    model.trees.zipWithIndex.foreach{ case (tree, index: Int) =>
      print("writing tree: ", index, "\r")
      val oos = new ObjectOutputStream(new FileOutputStream(modelName + "/" + index + ".tree"))
      oos.writeObject(Json.toJson(tree).toString())
      oos.close()
    }
    val oos = new ObjectOutputStream(new FileOutputStream(modelName + "/oobError"))
    oos.writeObject(model.oobError)
    oos.close()
  }

  def loadModel(modelName: String): RandomForest = {
    val d = Directory(modelName)
    val trees = d.list.filter(x => x.extension == "tree").map(x => {
      print("loading tree: ", x.toString(), "\r")
      val oos = new ObjectInputStream(new FileInputStream(x.toString()))
      val tree = Json.parse(oos.readObject().asInstanceOf[String]).validate[DecisionTree].get
      oos.close()
      tree
    }).toList
    val oos = new ObjectInputStream(new FileInputStream(modelName + "/oobError"))
    val oobError = oos.readObject().asInstanceOf[Double]
    RandomForest(trees, oobError)
  }

  def trainClassifier(dbName: String, trainingCollection: String, modelName: String, leafSize: Int, features: Int, numberOfTrees: Int) = {
    println("#trees: " + numberOfTrees + ", leafSize: " + leafSize + ", #features: " + features + ", startTime: " + DateTime.now())
    val model = new ExtremelyRandomForest().trainForest(numberOfTrees, leafSize, features, loadDataSet(dbName, trainingCollection))
    saveModel(modelName, model)
    println("endTime " + DateTime.now())
  }

  def testClassifier(dbName: String, testingCollection: String, modelName: String) = {
    val model = loadModel(modelName)
    val testingSet = loadDataSet(dbName, testingCollection)
    var completed = 0

    println(testingSet.foldLeft(Map.empty[String, Map[String, Int]])((output, instance) => {
      val current = output.getOrElse(instance.label, Map.empty[String, Int])
      val classification = model.getClassification(instance.features)
      completed += 1
      print("Tested instances ", completed, "\r")
      output + (instance.label -> (current + (classification -> (current.getOrElse(classification, 0) + 1))))
    }))
  }

  def testClassifierPnL(dbName: String, collectionsPrefix: String, modelName: String, inplay: Boolean, minsBefore: Option[Int], numIndicators: Int): List[Double] = {
    val model = loadModel(modelName)
    var pnl = List.empty[Double]

    Await.result(listMarkets(dbName), Duration.Inf).filter(_.startsWith(collectionsPrefix)).map(market => {
      val marketPnL = testMarket(
        Await.result(getCollection(dbName, market).find(BSONDocument()).sort(BSONDocument("timestamp" -> -1)).cursor[JsObject]().collect[List]().map(x => {
          x.map(_.validate[Indicators].get).filter(isIntervalSelected(_, inplay, minsBefore))
        }), Duration.Inf),
        model,
        numIndicators
      )
      pnl = marketPnL :: pnl
      println("Market: " + market + "PnL: " + marketPnL)
    })
    println("Total Pnl: " + pnl.sum)
    println("Highest Win: " + pnl.max)
    println("Number of Wins: " + pnl.count(x => x > 0))
    println("Highest Loss: " + pnl.min)
    println("Number of Losses: " + pnl.count(x => x < 0))
    println("Number of scratches: " + pnl.count(x => x == 0))
    pnl
  }
}