package core.dataProvider

import akka.actor.{Actor, Cancellable}
import core.dataProvider.MarketPoller._
import core.dataProvider.output.MarketData
import core.eventBus.{MessageEvent, EventBus}
import domain.{PriceData, MatchProjection, OrderProjection, PriceProjection}
import server.Configuration
import service.BetfairServiceNG

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
 * Created by Alcock on 18/10/2015.
 */

// TODO use config to shape calls to betfairServiceNG
class MarketPoller(config: Configuration,
                   sessionToken: String,
                   betfairService: BetfairServiceNG,
                   eventBus: EventBus,
                   eventTypeId: String,
                   eventId: String,
                   marketId: String) extends Actor {

  // TODO get these from config
  private val DATA_PROVIDER_OUTPUT_CHANNEL = "dataProviderOutput"

  import context._

  var cancelPolling: Cancellable = _
  var isPolling: Boolean = false

  def receive = {
    case StartPolling =>
      // Start updates
      isPolling = true
      cancelPolling = system.scheduler.scheduleOnce(1 seconds, self, Update)
    case StopPolling =>
      // Stop further updates
      isPolling = false
      cancelPolling.cancel()
    case Update =>
      betfairService.listMarketBook(
        sessionToken,
        Set(marketId),
        priceProjection = Some(("priceProjection", PriceProjection(priceData = Set(PriceData.EX_BEST_OFFERS, PriceData.EX_TRADED)))),
        orderProjection = Some(("orderProjection", OrderProjection.EXECUTABLE)),
        matchProjection = Some(("matchProjection", MatchProjection.ROLLED_UP_BY_PRICE))
      ) onComplete {
        case Success(Some(listMarketBookContainer)) =>
          eventBus.publish(MessageEvent(DATA_PROVIDER_OUTPUT_CHANNEL, MarketData(eventTypeId, eventId, listMarketBookContainer)))
          if (isPolling)
            cancelPolling = system.scheduler.scheduleOnce(1 seconds, self, Update)
        case Success(None) =>
          // TODO handle event where betfair returns empty response
        case Failure(error) => throw new DataProviderException("call to listMarketBook " + marketId + "failed")
      }
    case x => throw new DataProviderException("unknown call to marketPoller " + marketId)
  }
}

object MarketPoller {
  case object StartPolling
  case object StopPolling
  case object Update
}