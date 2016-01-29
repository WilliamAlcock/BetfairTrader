package core.orderManager

import akka.actor.{Props, Actor}
import core.api.commands._
import core.api.output._
import core.eventBus.{EventBus, MessageEvent}
import org.joda.time.DateTime
import server.Configuration
import service.BetfairService

import scala.util.Success

// TODO should orderManager make sure that any markets being operated on are being polled ?


class OrderManager(config: Configuration,
                   sessionToken: String,
                   betfairService: BetfairService,
                   eventBus: EventBus) extends Actor {

  import context._

  // TODO get this from config
  private val ORDER_MANAGER_OUTPUT_CHANNEL = "orderManagerOutput"

  // TODO orderManager should log number of orders placed/cancelled/replaced/updated to ensure systems acts within limits
  val startTime = DateTime.now()

  private def getOutputChannel(marketId: String): String = ORDER_MANAGER_OUTPUT_CHANNEL + "/" + marketId

  def receive = {
    case PlaceOrders(marketId, instructions, customerRef, subscriber) =>
      // Send orders to exchange for test purposes assume they are placed
      betfairService.placeOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case x => println("message ", x)
//        case Success(Some(x)) =>
//          // TODO update the state of this object
//          eventBus.publish(MessageEvent(getOutputChannel(marketId), PlaceOrderResponse(x)))
//
//        case _ => throw new OrderManagerException("Market " + marketId + " placeOrders failed!")
      }
    case CancelOrders(marketId, instructions, customerRef, subscriber) =>
      betfairService.cancelOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) =>
          // TODO update the state of this object
          eventBus.publish(MessageEvent(getOutputChannel(marketId), CancelOrderResponse(x)))
        case _ => throw new OrderManagerException("Market " + marketId + " cancelOrders failed!")
      }
    case ReplaceOrders(marketId, instructions, customerRef, subscriber) =>
      betfairService.replaceOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) =>
          // TODO update the state of this object
          eventBus.publish(MessageEvent(getOutputChannel(marketId), ReplaceOrderResponse(x)))
        case _ => throw new OrderManagerException("Market " + marketId + " replaceOrders failed!")
      }
    case UpdateOrders(marketId, instructions, customerRef, subscriber) =>
      betfairService.updateOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) =>
          // TODO update the state of this object
          eventBus.publish(MessageEvent(getOutputChannel(marketId), UpdateOrderResponse(x)))
        case _ => throw new OrderManagerException("Market " + marketId + " placeOrders failed!")
      }
  }
}

object OrderManager {
  def props(config: Configuration, sessionToken: String, betfairService: BetfairService, eventBus: EventBus) =
    Props(new OrderManager(config, sessionToken, betfairService, eventBus))

}

