package dbIO

import domain._
import indicators.Indicators
import org.joda.time.DateTime
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.{JsObject, Json}
import randomForest.{DecisionTree, Instance, RandomForest}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

trait DBIO {

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

  def writeMarketCatalogue(m: MarketCatalogue): Future[UpdateWriteResult] = {
    dbConnector.getCollection(dbConnector.getDB(getDBName(m.marketStartTime.get)), marketCatalogueCollectionName).update(
      Json.obj("marketId" -> m.marketId),
      Json.toJson(m).as[JsObject],
      upsert = true
    )
  }

  def writeModel(name: String, model: RandomForest) = {
    dbConnector.getCollection(dbConnector.getDB("Models"), name).drop()         // Drop any existing collection
    val col = dbConnector.getCollection(dbConnector.getDB("Models"), name)
    // Write a record with the out of bag error and all the trees
    Await.result(col.insert(BSONDocument("oobRate" -> model.oobError)), Duration.Inf)
    model.trees.foreach(tree => Await.result(col.insert(Json.toJson(tree).as[JsObject]), Duration.Inf))
  }

  val processTrees: Iteratee[JsObject, List[DecisionTree]] = Iteratee.fold(List.empty[DecisionTree]) {(trees, tree) =>
    print("Loaded # Trees" + trees.size + "\r")
    tree.validate[DecisionTree].get :: trees
  }

  def readModel(name: String): RandomForest = {
    val col = dbConnector.getCollection(dbConnector.getDB("Models"), name)
    val oobError = Await.result(col.find(BSONDocument("oobRate" -> BSONDocument("$exists" -> true))).one[JsObject], Duration.Inf)
    val enumerator = col.find(BSONDocument("oobRate" -> BSONDocument("$exists" -> false))).cursor[JsObject]().enumerate()
    val trees = Await.result(enumerator |>>> processTrees, Duration.Inf)
    RandomForest(trees, (oobError.get \ "oobRate").get.as[Double])
  }

  // ********************

  def writeIndicators(i: Indicators, db: String, market: String, postFix: String = ""): Future[WriteResult] = {
    dbConnector.getCollection(dbConnector.getDB(db + postFix), market).insert(Json.toJson(i).as[JsObject])
  }

  def writeInstance(i: Instance, db: String, collection: String): Future[WriteResult] = {
    dbConnector.getCollection(dbConnector.getDB(db), collection).insert(Json.toJson(i).as[JsObject])
  }

  // readIndicators, readInstance
}