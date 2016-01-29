package core.dataProvider.polling

import akka.actor.{Actor, Props}
import core.api.output.MarketBookUpdate
import core.dataProvider.DataProviderException
import core.dataProvider.polling.MarketPoller.Poll
import core.eventBus.{EventBus, MessageEvent}
import domain.MatchProjection.MatchProjection
import domain.OrderProjection.OrderProjection
import domain.PriceProjection
import org.joda.time.DateTime
import server.Configuration
import service.BetfairService

import scala.language.postfixOps
import scala.util.{Failure, Success}

class MarketPoller(config: Configuration,
                   sessionToken: String,
                   betfairService: BetfairService,
                   eventBus: EventBus) extends Actor {

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
        case Success(Some(listMarketBookContainer)) => listMarketBookContainer.result.foreach(x =>
          eventBus.publish(MessageEvent(config.dataModelInstructions, MarketBookUpdate(DateTime.now(), x))))
        case Success(None) => println("call to service failed to return data")
          // TODO handle event where betfair returns empty response
        case Failure(error) => throw new DataProviderException("call to listMarketBook failed " + error)
      }
    case x => throw new DataProviderException("unknown call to marketPoller")
  }
}

object MarketPoller {
  def props(config: Configuration, sessionToken: String, betfairService: BetfairService, eventBus: EventBus) =
    Props(new MarketPoller(config, sessionToken, betfairService, eventBus))

  case class Poll(marketIds: Set[String], priceProjection: PriceProjection, orderProjection: OrderProjection, matchProjection: MatchProjection)
}