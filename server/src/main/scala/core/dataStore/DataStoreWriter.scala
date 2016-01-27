package core.dataStore

import akka.actor.{Actor, Props}
import core.dataProvider.output.MarketDataUpdate
import domain.MarketBook
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.MongoDriver
import server.Configuration

class DataStoreWriter(config: Configuration) extends Actor {

  import context._

  val driver = new MongoDriver()
  val connection = driver.connection(List("localhost"))
  val db = connection("BetfairTrader")
  var collection = db.collection[JSONCollection]("test")

//  var collections = HashMap.empty[String, JSONCollection]
//
//  private def getCollection(marketId: String): JSONCollection = collections.get(marketId) match {
//    case Some(x) => x
//    case _ =>
//      collections = collections + (marketId -> db.collection[JSONCollection](marketId))
//      collections(marketId)
//  }

  private def getMinutesToStart(marketStartTime: DateTime, timestamp: DateTime): Int = {
    val delta = new DateTime(marketStartTime.getMillis - DateTime.now().getMillis)

    require(delta.getYear == 1970)
    require(delta.getMonthOfYear == 1)
    require(delta.getDayOfMonth == 1)
    //require(delta.getHourOfDay == 1)

    ((delta.getHourOfDay - 1) * 60 ) + delta.getMinuteOfHour
  }


  private def writeMarketBook(marketBook: MarketBook) = {
    println("writing marketBook data to mongodb")
    collection.update(
      Json.obj("marketId" -> marketBook.marketId),
      Json.obj("$push" -> Json.obj("data" -> Entry(DateTime.now(), marketBook))),
      upsert = true
    )
//    collection.update(
//      Json.obj("marketId" -> JsString(marketBook.marketId)),
//      Json.obj("$push" -> Json.obj("data" -> Entry(DateTime.now(), "marketBook", Json.toJson(marketBook))))
//    )
  }

  def receive = {
    case x: MarketDataUpdate => x.listMarketBookContainer.result.foreach(y => writeMarketBook(y))
    case x => println("dataStore received unhandled object " + x)
  }
}

object DataStoreWriter {
  def props(config: Configuration) = Props(new DataStoreWriter(config))
}
