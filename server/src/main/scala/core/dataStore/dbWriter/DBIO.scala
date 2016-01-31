package core.dataStore.dbWriter

import core.api.output.MarketBookUpdate
import core.dataModel.DataStoreException
import core.dataStore.MarketDocument
import domain.MarketCatalogue
import org.joda.time.{DateTime, Interval}
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.play.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success}

trait DBIO {

  def getCollection(name: String): BSONCollection

  private def getCollectionName(timestamp: DateTime): String = timestamp.toString("yyyyMMdd")

  private def getKey(startTime: DateTime, timestamp: DateTime): String = startTime.getMillis() - timestamp.getMillis() match {
    case x if x < 0 => new Interval(startTime, timestamp).toPeriod().toStandardMinutes.getMinutes + "a"     // after start time
    case _ => new Interval(timestamp, startTime).toPeriod().toStandardMinutes.getMinutes.toString           // before start time
  }

  // TODO convert print statements to logging

  // TODO ideally I don't want to throw an exception here but I cant recreate a WriteResult
  // Note to developer: this function will throw an exception if there is a duplicate record in the database !!!
  def writeMarketDocument(marketDocument: MarketDocument, marketStartTime: DateTime): Future[WriteResult] = {
    val promise = Promise[WriteResult]
    println("writing marketDocument to mongodb", marketDocument.marketId)
    // check a duplicate document does not exist, if not write the document
    findMarketDocument(marketDocument.marketId, marketStartTime) onComplete {
      case Success(Some(x)) => promise.complete(Failure(DataStoreException("cannot write document, duplicate record exists")))
      case _ => promise.completeWith(getCollection(getCollectionName(marketStartTime)).insert(
        Json.obj(
          "marketId" -> marketDocument.marketId,
          "marketCatalog" -> marketDocument.marketCatalogue,
          "data" -> marketDocument.data
        )
      ))
    }
    promise.future
  }

  def writeMarketBookUpdate(marketBookUpdate: MarketBookUpdate, marketStartTime: DateTime): Future[UpdateWriteResult] = {
    println("writing marketBook data to mongodb for market", marketBookUpdate.data.marketId)
    getCollection(getCollectionName(marketStartTime)).update(
      Json.obj("marketId" -> marketBookUpdate.data.marketId),
      Json.obj("$push" -> Json.obj("data." + getKey(marketStartTime, marketBookUpdate.timestamp) -> marketBookUpdate)),
      upsert = true
    )
  }

  def writeMarketCatalogue(marketCatalogue: MarketCatalogue): Future[UpdateWriteResult] = {
    println("writing marketCatalogue data to mongodb for market", marketCatalogue.marketId)
    marketCatalogue.marketStartTime match {
      case Some(x) =>
        getCollection(getCollectionName(marketCatalogue.marketStartTime.get)).update(
          Json.obj("marketId" -> marketCatalogue.marketId),
          Json.obj("$set" -> Json.obj("marketCatalogue" -> marketCatalogue, "data.dummy" -> Json.arr())),         // TODO this is a hack !! adding a dummy record to data so that "data" is created if the document doesn't exist
          upsert = true
        )
      case _ => Future.failed(throw new IllegalArgumentException("marketCatalogue.marketStartTime is not defined"))
    }
  }

  def findMarketDocument(marketId: String, date: DateTime): Future[Option[MarketDocument]] = {
    val promise = Promise[Option[MarketDocument]]
    println("finding", marketId, date)
    getCollection(getCollectionName(date)).find(Json.obj("marketId" -> marketId)).one[JsObject] onComplete {
      case Success(Some(x: JsObject)) => x.validate[MarketDocument] match {
        case JsSuccess(doc, path) => promise.complete(Success(Some(doc)))
        case JsError(err) => promise.complete(Failure(DataStoreException(err.toString())))
      }
      case x => promise.complete(Success(None))

    }
    promise.future
  }
}