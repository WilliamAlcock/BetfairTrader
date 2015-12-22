package core.dataProvider

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.routing.RoundRobinGroup
import core.api.commands._
import core.dataProvider.DataProvider.Tick
import core.dataProvider.commands.{StartPollingMarkets, StopPollingMarkets, UnSubscribe}
import core.dataProvider.output.{EventDataUpdate, EventTypeDataUpdate, MarketCatalogueDataUpdate}
import core.dataProvider.polling.MarketPoller.Poll
import core.dataProvider.polling.{MarketPoller, PollingGroup, PollingGroups}
import core.eventBus.{EventBus, MessageEvent}
import domain.{MatchProjection, OrderProjection, _}
import org.joda.time.DateTime
import server.Configuration
import service.BetfairService

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

// TODO use config to shape calls to betfairServiceNG
class DataProvider(config: Configuration,
                   sessionToken: String,
                   betfairService: BetfairService,
                   eventBus: EventBus) extends Actor {

  import context._

  // TODO implement update navigation data
  // TODO get these from config
  private val DATA_PROVIDER_OUTPUT_CHANNEL = "dataProviderOutput"
  private val MAX_MARKET_CATALOGUE = 200
  val ORDER_PROJECTION = OrderProjection.EXECUTABLE
  val MATCH_PROJECTION = MatchProjection.ROLLED_UP_BY_PRICE

  val workers: Seq[ActorRef] = List[ActorRef](
    context.actorOf(MarketPoller.props(config, sessionToken, betfairService, eventBus), "pollingWorker1"),
    context.actorOf(MarketPoller.props(config, sessionToken, betfairService, eventBus), "pollingWorker2"),
    context.actorOf(MarketPoller.props(config, sessionToken, betfairService, eventBus), "pollingWorker3"),
    context.actorOf(MarketPoller.props(config, sessionToken, betfairService, eventBus), "pollingWorker4")
  )

  var pollingRouter: ActorRef = context.actorOf(RoundRobinGroup(workers.map {x=> x.path.toString}.toList).props(), "router")
  var pollingGroups = PollingGroups()
  var cancelPolling: Cancellable = _
  var isPolling: Boolean = false

  // Split the markets into sets of 40 and action them
  def sendToRouter(markets: Set[String], maxMarkets: Int, action: (Set[String]) => Unit): Unit = markets.size match {
    case x if x > maxMarkets =>
      val (a, b) = markets splitAt maxMarkets
      sendToRouter(a, maxMarkets, action)
      sendToRouter(b, maxMarkets, action)
    case x =>
      action(markets)
  }

  def tick() = {
    pollingGroups.pollingGroups.foreach{ case (pollingGroup: PollingGroup, markets: Set[String]) =>
      sendToRouter(markets, pollingGroup.maxMarkets, (m: Set[String]) =>
        pollingRouter ! Poll(m, pollingGroup.getPriceProjection(), ORDER_PROJECTION, MATCH_PROJECTION)
      )}
    pollingGroups.pollingGroups.size match {
      case x if x == 0 =>
        println(DateTime.now().toString + " ticking no markets")
        cancelPolling.cancel()
        isPolling = false
      case x =>
        cancelPolling = context.system.scheduler.scheduleOnce(500 milliseconds, self, Tick)
        isPolling = true
    }
  }

  def listMarketCatalogue(marketIds: Set[String]):Unit = {
    betfairService.listMarketCatalogue(
      sessionToken,
      new MarketFilter(marketIds = marketIds),
      List(
        MarketProjection.MARKET_START_TIME,
        //MarketProjection.RUNNER_METADATA, // TODO need to get a json reader working for runner metadata
        MarketProjection.RUNNER_DESCRIPTION,
        MarketProjection.EVENT_TYPE,
        MarketProjection.EVENT,
        MarketProjection.COMPETITION
      ),
      MarketSort.FIRST_TO_START,
      200
    ) onComplete {
      case Success(Some(listMarketCatalogueContainer)) =>
        eventBus.publish(MessageEvent(DATA_PROVIDER_OUTPUT_CHANNEL, MarketCatalogueDataUpdate(listMarketCatalogueContainer)))
      case Success(None) =>
      // TODO handle event where betfair returns empty response
      case Failure(error) => throw new DataProviderException("call to listMarketCatalogue failed")
    }
  }

  def receive = {
    case ListEventTypes =>
      betfairService.listEventTypes(sessionToken, new MarketFilter()) onComplete {
        case Success(Some(listEventTypeResultContainer)) =>
          eventBus.publish(MessageEvent(DATA_PROVIDER_OUTPUT_CHANNEL, EventTypeDataUpdate(listEventTypeResultContainer)))
        case Success(None) =>
          // TODO handle event where betfair returns empty response
        case Failure(error) => throw new DataProviderException("call to listEventTypes failed")
      }
    case ListEvents(eventTypeId) =>
      betfairService.listEvents(sessionToken, new MarketFilter(eventTypeIds = Set(eventTypeId))) onComplete {
        case Success(Some(listEventResultContainer)) =>
          eventBus.publish(MessageEvent(DATA_PROVIDER_OUTPUT_CHANNEL, EventDataUpdate(listEventResultContainer)))
        case Success(None) =>
          // TODO handle event where betfair returns empty response
        case Failure(error) => throw new DataProviderException("call to listEvents failed")
      }
    case ListMarketCatalogue(marketIds) => sendToRouter(marketIds, MAX_MARKET_CATALOGUE, listMarketCatalogue)
    case StartPollingMarkets(marketIds, subscriber, pollingGroup) =>
      marketIds.foreach(x => pollingGroups = pollingGroups.addSubscriber(x, subscriber, pollingGroup))
      if (!isPolling) {
        cancelPolling = context.system.scheduler.scheduleOnce(500 milliseconds, self, Tick)
        isPolling = true
      }
    case StopPollingMarkets(marketIds, subscriber, pollingGroup) =>
      marketIds.foreach(x => pollingGroups = pollingGroups.removeSubscriber(x, subscriber, pollingGroup))
    case UnSubscribe(subscriber) => pollingGroups = pollingGroups.removeSubscriber(subscriber)
    case StopPollingAllMarkets => pollingGroups = pollingGroups.removeAllSubscribers()
    case Tick => tick()
    case _ => throw new DataProviderException("unknown call to data provider")
  }
}

object DataProvider {
  def props(config: Configuration, sessionToken: String, betfairService: BetfairService, eventBus: EventBus) =
    Props(new DataProvider(config, sessionToken, betfairService, eventBus))

  case object Tick
}