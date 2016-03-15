package core.orderManager

import akka.actor.{Actor, Props}
import core.api.commands._
import core.eventBus.EventBus
import org.joda.time.DateTime
import server.Configuration
import service.BetfairService

import scala.util.Success

// TODO should orderManager make sure that any markets being operated on are being polled ?
class OrderManager(config: Configuration, sessionToken: String, betfairService: BetfairService, eventBus: EventBus) extends Actor {

  import context._

  // TODO orderManager should log number of orders placed/cancelled/replaced/updated to ensure systems acts within limits
  val startTime = DateTime.now()

  def receive = {
    case PlaceOrders(marketId, instructions, customerRef) =>
      betfairService.placeOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) => sender() ! x                                                   // On success send the response to the subscriber
        case _ => sender() ! OrderManagerException("Market " + marketId + " placeOrders failed!")
      }
    case CancelOrders(marketId, instructions, customerRef) =>
      betfairService.cancelOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) => sender() ! x
        case _ => sender() ! OrderManagerException("Market " + marketId + " cancelOrders failed!")
      }
    case ReplaceOrders(marketId, instructions, customerRef) =>
      betfairService.replaceOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) => sender() ! x
        case _ => sender() ! OrderManagerException("Market " + marketId + " replaceOrders failed!")
      }
    case UpdateOrders(marketId, instructions, customerRef) =>
      betfairService.updateOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) => sender() ! x
        case _ => sender() ! OrderManagerException("Market " + marketId + " placeOrders failed!")
      }
  }
}

object OrderManager {
  def props(config: Configuration, sessionToken: String, betfairService: BetfairService, eventBus: EventBus) =
    Props(new OrderManager(config, sessionToken, betfairService, eventBus))

}

