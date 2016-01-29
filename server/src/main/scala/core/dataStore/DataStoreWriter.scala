//package core.dataStore
//
//import akka.actor.{Actor, ActorRef, Props}
//import core.api.commands._
//import core.api.output.{EventUpdate, MarketBookUpdate, MarketCatalogueUpdate}
//import domain.{MarketBook, MarketCatalogue, TimeRange}
//import org.joda.time.DateTime
//import play.api.libs.json.Json
//import play.modules.reactivemongo.json._
//import play.modules.reactivemongo.json.collection.JSONCollection
//import reactivemongo.api.MongoDriver
//import server.Configuration
//
//import scala.collection.immutable.HashMap
//
//class DataStoreWriter(config: Configuration, controller: ActorRef) extends Actor {
//
//  import context._
//
//  // TODO get these values from config
//
//  val COLLECTION_NAME_FORMAT = "YYYY_MM_DD"
//
//  val driver = new MongoDriver()
//  val connection = driver.connection(List("localhost"))
//  val db = connection("BetfairTrader")
//
//  var marketStartTimes = HashMap.empty[String, DateTime]
//  var currentDay: String = _
//  var collection: JSONCollection = _
//
//  private def getDay(timestamp: DateTime): String = timestamp.toString(COLLECTION_NAME_FORMAT)
//
//  private def getCollection(timestamp: DateTime): JSONCollection = (currentDay, getDay(timestamp)) match {
//    case (x, y) if x != y =>
//      currentDay = y
//      collection = db.collection[JSONCollection](currentDay)
//      collection
//    case _ => collection
//  }
//
//  private def getMinutesToStart(marketStartTime: DateTime, timestamp: DateTime): Int = {
//    val delta = new DateTime(marketStartTime.getMillis - DateTime.now().getMillis)
//
//    require(delta.getYear == 1970)
//    require(delta.getMonthOfYear == 1)
//    require(delta.getDayOfMonth == 1)
//    //require(delta.getHourOfDay == 1)
//
//    ((delta.getHourOfDay - 1) * 60 ) + delta.getMinuteOfHour
//  }
//
//  private def writeMarketBook(marketBook: MarketBook, timestamp: DateTime) = {
//    println("writing marketBook data to mongodb for market", marketBook.marketId)
//    marketStartTimes.get(marketBook.marketId) match {
//      case Some(x) => getCollection(timestamp).update(
//        Json.obj("marketId" -> marketBook.marketId),
////        Json.obj("$push" -> Json.obj("data" -> Json.obj(getMinutesToStart(x, timestamp).toString -> Entry(timestamp, marketBook)))),
//        Json.obj("$push" -> Json.obj("data" -> Json.obj("1" -> Entry(timestamp, marketBook)))),
//        upsert = true
//      )
//      case _ => println("cannot store marketBook as no marketCatalogue is stored")
//    }
//  }
//
//  private def writeMarketCatalogue(marketCatalogue: MarketCatalogue, timestamp: DateTime = DateTime.now()) = {
//    println("writing marketCatalogue data to mongodb for market", marketCatalogue.marketId)
//    marketCatalogue.marketStartTime match {
//      case Some(x) =>
////        controller ! SubscribeToMarkets(markets = List(marketCatalogue.marketId), BEST)
//        marketStartTimes = marketStartTimes + (marketCatalogue.marketId -> marketCatalogue.marketStartTime.get)
//        getCollection(timestamp).update(
//          Json.obj("marketId" -> marketCatalogue.marketId),
//          Json.obj("$set" -> Json.obj("marketCatalogue" -> Some(marketCatalogue))),
//          upsert = true
//        )
//      case _ => println("cannot store marketCatalogue as there is no marketStartTime")
//    }
//  }
//
//  private def startup(timestamp: DateTime) = {
//    // subscribe to event data
//    controller ! SubscribeToEventData
//    // request all GB & IE horse racing events for the current day
//    controller ! ListEvents("7", Set("GB", "IE"), Some(TimeRange(None, Some(timestamp.plusDays(1).withTimeAtStartOfDay()))))
//  }
//
//  private def getMarketsForEvent(eventId: String) = {
//    // subscribe to market data
//    controller ! SubscribeToMarketCatalogueData
//    // request all the market catalogues for each event
//    controller ! ListMarketCatalogue(eventIds = Set(eventId))
//  }
//
//  override def preStart() = startup(DateTime.now())
//
//  override def receive = {
//    case x: MarketBookUpdate => writeMarketBook(x.data, x.timestamp)
//    case x: MarketCatalogueUpdate => writeMarketCatalogue(x.data)
//    case x: EventUpdate => getMarketsForEvent(x.data.event.id)
//    case x => println("dataStore received unhandled object", x)
//  }
//}
//
//object DataStoreWriter {
//  def props(config: Configuration, controller: ActorRef) = Props(new DataStoreWriter(config, controller))
//}
