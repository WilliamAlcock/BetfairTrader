package dbIO

import csvReader.CSVData
import domain._
import indicators.Indicators
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}
import randomForest.Instance
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class DBIO {

  val dbConnector: DBConnector = DBConnector

  def getDBName(timestamp: DateTime): String = timestamp.toString("yyyy")

  def getCollectionName(timestamp: DateTime, marketId: String): String = timestamp.toString("MM_dd_") + marketId

  // TODO get these values from config
  private val marketCatalogueCollectionName = "catalogues"

  // TODO filter out all names that don't match the format MM_dd_
  def listMarkets(db: String): Future[List[String]] = {
    dbConnector.getDB(db).collectionNames.map(x => x.filter(!Set("system.indexes", "distribution", "runner_catalogues", "runner_catalogues_top2").contains(_)))
  }

  def writeCSVData(d: CSVData): Future[WriteResult] = {
    dbConnector.getCollection(dbConnector.getDB(getDBName(d.startTime)), getCollectionName(d.startTime, d.marketId)).insert(Json.toJson(d).as[JsObject])
  }

  def getCollection(db: String, collection: String): BSONCollection = dbConnector.getCollection(dbConnector.getDB(db), collection)

  def createIndex(col: BSONCollection, index: Index): Future[WriteResult] = col.indexesManager.create(index)

  def getRunners(col: BSONCollection): Future[List[RunnerCatalog]] = {
    import col.BatchCommands.AggregationFramework.{Group, Max}
    col.aggregate(
      Group(BSONDocument("selectionId" -> "$selectionId", "runnerName" -> "$runnerName"))("totalMatched" -> Max("totalMatched"))
    ).map(_.documents.map(runner => RunnerCatalog(
      selectionId = runner.getAs[BSONDocument]("_id").get.getAs[Int]("selectionId").get,
      runnerName = runner.getAs[BSONDocument]("_id").get.getAs[String]("runnerName").get,
      handicap = 0.0,
      totalMatched = Some(runner.getAs[Double]("totalMatched").get)
    )))
  }

  def getMarketCatalogue(col: BSONCollection): Option[MarketCatalogue] = {
    // get the first record
    Await.result(col.find(BSONDocument()).one[CSVData], Duration.Inf) match {
      case Some(csvData: CSVData) =>
        val runners = Await.result(getRunners(col), Duration.Inf)
        Some(MarketCatalogue(
          marketId = csvData.marketId,
          marketName = csvData.getMarketName,
          marketStartTime = Some(csvData.startTime),
          description = None,
          totalMatched = runners.map(_.totalMatched.get).sum,
          runners = Some(runners),
          eventType = Some(EventType("7", csvData.getEventType)), // TODO get eventType Id from the name
          competition = None,
          event = Event(csvData.eventId, csvData.getEventName, Some(csvData.getCountryCode), "", None, csvData.startTime)
        ))
      case _ => None
    }
  }

  def writeMarketCatalogue(m: MarketCatalogue): Future[UpdateWriteResult] = {
    dbConnector.getCollection(dbConnector.getDB(getDBName(m.marketStartTime.get)), marketCatalogueCollectionName).update(
      Json.obj("marketId" -> m.marketId),
      Json.toJson(m).as[JsObject],
      upsert = true
    )
  }

  // ********************

  def writeIndicators(i: Indicators, db: String, market: String, postFix: String = ""): Future[WriteResult] = {
    dbConnector.getCollection(dbConnector.getDB(db + postFix), market).insert(Json.toJson(i).as[JsObject])
  }

  def writeInstance(i: Instance, db: String, collection: String): Future[WriteResult] = {
    dbConnector.getCollection(dbConnector.getDB(db), collection).insert(Json.toJson(i).as[JsObject])
  }

  // readIndicators , read Instance
}