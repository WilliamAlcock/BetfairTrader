package core.dataProvider

import akka.actor.{Actor, ActorRef, Props}

import core.api._
import core.dataProvider.MarketPoller._
import core.dataProvider.output.{MarketCatalogueData, EventData, EventTypeData}
import core.eventBus.{MessageEvent, EventBus}
import domain.{MarketProjection, MarketSort, MarketFilter}
import server.Configuration
import service.BetfairServiceNG

import scala.collection.immutable.HashMap
import scala.util.{Failure, Success}

sealed case class MarketActorCounter(count: Int, actor: ActorRef) {
  def increment: MarketActorCounter = {
    if (this.count == 0) this.actor ! StartPolling
    this.copy(count = this.count + 1)
  }
  def decrement: MarketActorCounter = {
    if (this.count == 1) this.actor ! StopPolling
    this.copy(count = Math.max(0, this.count - 1))    // count should never be negative
  }
  def stop: MarketActorCounter = {
    if (this.count > 0) this.actor ! StopPolling
    this.copy(count = 0)
  }
}

// TODO use config to shape calls to betfairServiceNG
class DataProvider(config: Configuration,
                   sessionToken: String,
                   betfairServiceNG: BetfairServiceNG,
                   eventBus: EventBus) extends Actor {

  import context._

  // TODO get these from config
  private val DATA_PROVIDER_OUTPUT_CHANNEL = "dataProviderOutput"

  var marketActors: HashMap[String, MarketActorCounter] = HashMap[String, MarketActorCounter]()

  private def getNewMarketActorCounter(eventTypeId: String, eventId: String, marketId: String): MarketActorCounter = {
    val newMarketPoller = new MarketPoller(
      config,
      sessionToken,
      betfairServiceNG,
      eventBus,
      eventTypeId,
      eventId,
      marketId
    )
    MarketActorCounter(0, context.actorOf(Props(newMarketPoller)))
  }

  def receive = {
    case ListEventTypes() =>
      betfairServiceNG.listEventTypes(sessionToken, new MarketFilter()) onComplete {
        case Success(Some(listEventTypeResultContainer)) =>
          eventBus.publish(MessageEvent(DATA_PROVIDER_OUTPUT_CHANNEL, EventTypeData(listEventTypeResultContainer)))
        case Success(None) =>
          // TODO handle event where betfair returns empty response
        case Failure(error) => throw new DataProviderException("call to listEventTypes failed")
      }
    case ListEvents(eventTypeId) =>
      betfairServiceNG.listEvents(sessionToken, new MarketFilter(eventTypeIds = Set(eventTypeId.toInt))) onComplete {
        case Success(Some(listEventResultContainer)) =>
          eventBus.publish(MessageEvent(DATA_PROVIDER_OUTPUT_CHANNEL, EventData(eventTypeId, listEventResultContainer)))
        case Success(None) =>
          // TODO handle event where betfair returns empty response
        case Failure(error) => throw new DataProviderException("call to listEvents failed")
      }
    case ListMarketCatalogue(eventTypeId, eventId) =>
      // Get all the markets
      betfairServiceNG.listMarketCatalogue(
        sessionToken,
        new MarketFilter(eventTypeIds = Set(eventTypeId.toInt), eventIds = Set(eventId.toInt)),
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
          eventBus.publish(MessageEvent(DATA_PROVIDER_OUTPUT_CHANNEL, MarketCatalogueData(eventTypeId, listMarketCatalogueContainer)))
        case Success(None) =>
          // TODO handle event where betfair returns empty response
        case Failure(error) => throw new DataProviderException("call to listMarketCatalogue failed")
      }
    case StartPollingMarket(eventTypeId: String, eventId: String, marketId: String) =>
      this.marketActors.get(marketId) match {
        case Some(x: MarketActorCounter) => marketActors = marketActors + (marketId -> x.increment)
        case None =>
          marketActors = marketActors + (marketId -> getNewMarketActorCounter(eventTypeId, eventId, marketId).increment)
      }
    case StopPollingMarket(eventTypeId: String, eventId: String, marketId: String) =>
      this.marketActors.get(marketId) match {
        case Some(x: MarketActorCounter) => marketActors = marketActors + (marketId -> x.decrement)
        case None => throw new DataProviderException("cannot stop polling market that has not been started " + marketId)
      }
    case StopPollingAllMarkets() =>
      // for each market actor stopPollingMarket
      this.marketActors = marketActors.map {case(id: String, counter: MarketActorCounter) => id -> counter.stop}
    case _ => throw new DataProviderException("unknown call to data provider")
  }
}
