package core.orderManager

import core.api._
import akka.actor.Actor
import core.eventBus.{MessageEvent, EventBus}
import org.joda.time.DateTime
import server.Configuration
import service.BetfairServiceNG

import scala.util.Success

// TODO should orderManager make sure that any markets being operated on are being polled ?


class OrderManager(config: Configuration,
                   sessionToken: String,
                   betfairServiceNG: BetfairServiceNG,
                   eventBus: EventBus) extends Actor {

  import context._

  // TODO get this from config
  private val ORDER_MANAGER_OUTPUT_CHANNEL = "orderManagerOutput"

  // TODO orderManager should log number of orders placed/cancelled/replaced/updated to ensure systems acts within limits
  val startTime = DateTime.now()

  private def getOutputChannel(marketId: String): String = ORDER_MANAGER_OUTPUT_CHANNEL + "/" + marketId

  def receive = {
    case PlaceOrders(marketId, instructions, customerRef) =>
      // Send orders to exchange for test purposes assume they are placed
      betfairServiceNG.placeOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) =>
          // TODO update the state of this object
          eventBus.publish(MessageEvent(getOutputChannel(marketId), PlaceOrderResponse(x)))
        case _ => throw new OrderManagerException("Market " + marketId + " placeOrders failed!")
      }
    case CancelOrders(marketId, instructions, customerRef) =>
      betfairServiceNG.cancelOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) =>
          // TODO update the state of this object
          eventBus.publish(MessageEvent(getOutputChannel(marketId), CancelOrderResponse(x)))
        case _ => throw new OrderManagerException("Market " + marketId + " cancelOrders failed!")
      }
    case ReplaceOrders(marketId, instructions, customerRef) =>
      betfairServiceNG.replaceOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) =>
          // TODO update the state of this object
          eventBus.publish(MessageEvent(getOutputChannel(marketId), ReplaceOrderResponse(x)))
        case _ => throw new OrderManagerException("Market " + marketId + " replaceOrders failed!")
      }
    case UpdateOrders(marketId, instructions, customerRef) =>
      betfairServiceNG.updateOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) =>
          // TODO update the state of this object
          eventBus.publish(MessageEvent(getOutputChannel(marketId), UpdateOrderResponse(x)))
        case _ => throw new OrderManagerException("Market " + marketId + " placeOrders failed!")
      }
  }
}

