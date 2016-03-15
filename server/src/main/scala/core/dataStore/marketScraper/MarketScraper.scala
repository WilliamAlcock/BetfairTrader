//package core.dataStore.marketScraper
//
//import akka.actor._
//import core.api.commands.{ListEvents, ListMarketCatalogue, SubscribeToMarkets}
//import core.dataProvider.polling.BEST_AND_TRADED
//import core.dataStore.marketScraper.MarketScraper.{InitDay, SubscribeToMarket}
//import dbIO.DBIO
//import domain._
//import org.joda.time.{DateTime, Interval}
//
//import scala.collection.immutable.HashMap
//import scala.concurrent.duration._
//import scala.language.postfixOps
//
//class MarketScraper(controller: ActorRef) extends Actor {
//
//  import context._
//
//  val dbIO: DBIO = new DBIO()
//
//  val scheduler = context.system.scheduler
//
//  var marketStartTimes = HashMap.empty[String, DateTime]
//  var scheduledScraping = HashMap.empty[String, Cancellable]
//  var initDayCancel: Cancellable = _
//
//  override def preStart() = {
//    initDay()
//  }
//
//  override def postStop() = {
//    initDayCancel.cancel()
//    scheduledScraping.foreach{case(k,v) => v.cancel()}
//  }
//
//  private def initDay() = {
//    val now = DateTime.now()
//    fetchEvents(TimeRange(Some(now.withTimeAtStartOfDay()), Some(now.plusDays(1).withTimeAtStartOfDay())))
//    val timeTillTomorrow = getFiniteDuration(now, now.plusDays(1).withTimeAtStartOfDay())
//    initDayCancel = scheduler.scheduleOnce(timeTillTomorrow, self, InitDay)
//  }
//
//  private def fetchEvents(timeRange: TimeRange) = {
//    controller ! ListEvents(MarketFilter(marketStartTime = Some(timeRange)), sender = self)
//  }
//
//  private def fetchMarketCatalogues(eventIds: Set[String]) = {
//    controller ! ListMarketCatalogue(MarketFilter(eventIds = eventIds), MarketSort.FIRST_TO_START, sender = self)
//  }
//
//  private def getFiniteDuration(x: DateTime, y: DateTime): FiniteDuration = Duration(new Interval(x, y).toDurationMillis, "millis")
//
//  private def subscribeToMarket(marketId: String) = {
//    scheduledScraping = scheduledScraping - marketId
//    controller ! SubscribeToMarkets(markets = Set(marketId), BEST_AND_TRADED, self)
//  }
//
//  private def scheduleScraping(marketCatalogue: MarketCatalogue) = {
//    val startTime = marketCatalogue.marketStartTime.get
//    val now = DateTime.now()
//    startTime.getMillis - now.getMillis match {
//      case x if x > 1200000 =>
//        scheduledScraping = scheduledScraping + (marketCatalogue.marketId -> scheduler.scheduleOnce(
//          getFiniteDuration(now, startTime.minusMinutes(20)),
//          self,
//          SubscribeToMarket(marketCatalogue.marketId)
//        ))
//      case x if x > 0 => subscribeToMarket(marketCatalogue.marketId)
//      case _ => // cant scrape, already past start time !!
//    }
//  }
//
//  override def receive = {
//    case ListEventResultContainer(x) => fetchMarketCatalogues(x.map(_.event.id).toSet)                  // List of events
//    case ListMarketCatalogueContainer(x) => x.filter(_.marketStartTime.isDefined).foreach{catalog =>
//      dbIO.writeMarketCatalogue(catalog)                                                              // Write the catalogue to the database
//      marketStartTimes = marketStartTimes + (catalog.marketId -> catalog.marketStartTime.get)         // Cache the start time
//      scheduleScraping(catalog)
//    }
//    case SubscribeToMarket(x) => subscribeToMarket(x)
//    case x: MarketBookUpdate => marketStartTimes.get(x.data.marketId) match {
//      case Some(startTime) => dbIO.writeMarketBookUpdate(x)
//      case None =>  // cant write data as there is no start time
//    }
//    case InitDay => initDay()
//    case x => println("unknown command to market scrapper " + x)
//  }
//}
//
//object MarketScraper {
//  def props(controller: ActorRef) = Props(new MarketScraper(controller))
//
//  case class SubscribeToMarket(marketId: String)
//  case object InitDay
//}