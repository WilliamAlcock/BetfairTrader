package core.dataProvider.polling

import akka.actor.{Actor, Props}
import core.dataProvider.DataProviderException
import core.dataProvider.output.MarketDataUpdate
import core.dataProvider.polling.MarketPoller.Poll
import core.eventBus.{EventBus, MessageEvent}
import domain.MatchProjection.MatchProjection
import domain.OrderProjection.OrderProjection
import domain.PriceProjection
import server.Configuration
import service.BetfairServiceNG

import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
 * Created by Alcock on 18/10/2015.
 */

class MarketPoller(config: Configuration,
                   sessionToken: String,
                   betfairService: BetfairServiceNG,
                   eventBus: EventBus) extends Actor {

  // TODO get these from config
  private val DATA_PROVIDER_OUTPUT_CHANNEL = "dataProviderOutput"

  import context._

  override def receive = {
    case Poll(marketIds, priceProjection, orderProjection, matchProjection) =>
      println("     Polling " + marketIds.size + " markets")
      betfairService.listMarketBook(
        sessionToken,
        marketIds,
        priceProjection = Some(("priceProjection", priceProjection)),
        orderProjection = Some(("orderProjection", orderProjection)),
        matchProjection = Some(("matchProjection", matchProjection))
      ) onComplete {
        case Success(Some(listMarketBookContainer)) =>
          println (listMarketBookContainer.result.head.runners.head.ex.get.availableToBack)
          eventBus.publish(MessageEvent(DATA_PROVIDER_OUTPUT_CHANNEL, MarketDataUpdate(listMarketBookContainer)))
        case Success(None) =>
          // TODO handle event where betfair returns empty response
        case Failure(error) => throw new DataProviderException("call to listMarketBook failed " + error)
      }
    case x => throw new DataProviderException("unknown call to marketPoller")
  }
}

object MarketPoller {
  def props(config: Configuration, sessionToken: String, betfairService: BetfairServiceNG, eventBus: EventBus) =
    Props(new MarketPoller(config, sessionToken, betfairService, eventBus))

  case class Poll(marketIds: Set[String], priceProjection: PriceProjection, orderProjection: OrderProjection, matchProjection: MatchProjection)
}