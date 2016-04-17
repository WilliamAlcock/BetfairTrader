package core.dataProvider

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.routing.RoundRobinGroup
import core.api.commands._
import core.dataProvider.DataProvider.Tick
import core.dataProvider.polling.MarketPoller.Poll
import core.dataProvider.polling.{MarketPoller, PollingGroup, PollingGroups}
import core.eventBus.EventBus
import domain.MarketSort.MarketSort
import domain._
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

  // TODO get these values from config
  val pollingInterval: FiniteDuration = 500 millis
  val numberOfWorkers: Int = 4

  val workers: Seq[ActorRef] = List.range(0, numberOfWorkers).map(x => context.actorOf(MarketPoller.props(config, sessionToken, betfairService, eventBus), "pollingWorker" + x))

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
      sendToRouter(markets, pollingGroup.maxMarkets, (m: Set[String]) => pollingRouter ! Poll(m, pollingGroup.getPriceProjection(), config.orderProjection, config.matchProjection))}
    pollingGroups.pollingGroups.size match {
      case x if x == 0 =>
        println(DateTime.now().toString + " ticking no markets")
        cancelPolling.cancel()
        isPolling = false
      case x =>
        cancelPolling = context.system.scheduler.scheduleOnce(pollingInterval, self, Tick)     // TODO this value should come from config
        isPolling = true
    }
  }

  private def listMarketCatalogue(marketFilter: MarketFilter, sort: MarketSort, subscriber: ActorRef):Unit = {
    betfairService.listMarketCatalogue(
      sessionToken,
      marketFilter,
      List(
        MarketProjection.MARKET_START_TIME,
        //MarketProjection.RUNNER_METADATA, // TODO need to get a json reader working for runner metadata
        MarketProjection.MARKET_DESCRIPTION,
        MarketProjection.RUNNER_DESCRIPTION,
        MarketProjection.EVENT_TYPE,
        MarketProjection.EVENT,
        MarketProjection.COMPETITION
      ),
      sort,
      200
    ) onComplete {
      case Success(Some(listMarketCatalogueContainer)) => subscriber ! listMarketCatalogueContainer
      case Success(None) => // TODO handle event where betfair returns empty response
      case Failure(error) => throw new DataProviderException("call to listMarketCatalogue failed")
    }
  }

  private def subscribeToMarkets(markets: Set[String], pollingGroup: PollingGroup, subscriber: ActorRef) = {
    markets.foreach(x => pollingGroups = pollingGroups.addSubscriber(x, subscriber.toString(), pollingGroup))
    if (!isPolling) {
      cancelPolling = context.system.scheduler.scheduleOnce(pollingInterval, self, Tick)     // TODO this value should come from config
      isPolling = true
    }
  }

  private def unSubscribeFromMarkets(markets: Set[String], pollingGroup: PollingGroup, subscriber: ActorRef) =
    markets.foreach(x => pollingGroups = pollingGroups.removeSubscriber(x, subscriber.toString(), pollingGroup))

  def receive = {
    case ListMarketCatalogue(marketFilter, sort)        => listMarketCatalogue(marketFilter, sort, sender())
    case SubscribeToMarkets(markets, pollingGroup)      => subscribeToMarkets(markets, pollingGroup, sender())
    case UnSubscribeFromMarkets(markets, pollingGroup)  => unSubscribeFromMarkets(markets, pollingGroup, sender())
    case UnSubscribe                                    => pollingGroups = pollingGroups.removeSubscriber(sender().toString())
    case Tick                                           => tick()
    case _ => throw new DataProviderException("unknown call to data provider")
  }
}

object DataProvider {
  def props(config: Configuration, sessionToken: String, betfairService: BetfairService, eventBus: EventBus) =
    Props(new DataProvider(config, sessionToken, betfairService, eventBus))

  case object Tick
}