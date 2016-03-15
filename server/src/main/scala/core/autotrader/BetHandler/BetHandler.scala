package core.autotrader.betHandler

import akka.actor.{ActorRef, FSM, Props, Stash}
import akka.util.Timeout
import core.api.commands._
import core.autotrader.betHandler.BetHandler._
import core.dataProvider.polling.BEST
import core.orderManager.OrderManagerException
import domain._

import scala.concurrent.duration._
import scala.language.postfixOps

class BetHandler(controller: ActorRef) extends FSM[State, Data] with Stash {

  implicit val timeout = Timeout(5 seconds)

  // TODO move these functions into the domain code

  startWith(Idle, NoData)

  onTransition {
    case _ -> Idle => stateData match {
      case BetInstruction(marketId, _, _, _, _) => controller ! UnSubscribeFromMarkets(Set(marketId), BEST)
      case Bet(marketId, _, _, _, _, _) => controller ! UnSubscribeFromMarkets(Set(marketId), BEST)
      case _ => println("Hello")// Do nothing
    }
  }

  /*
    Waiting for instruction
   */
  when(Idle) {
    case Event(PlaceBet(marketId, instruction), _) =>
      controller ! SubscribeToMarkets(Set(marketId), BEST)
      controller ! PlaceOrders(marketId, Set(instruction))
      goto (Confirming) using BetInstruction(marketId, instruction, sender())
  }

  when(Confirming) {
    case Event(PlaceExecutionReportContainer(result), b: BetInstruction) if result.status == ExecutionReportStatus.SUCCESS =>
      val id = result.instructionReports.head.betId.get
      b.subscriber ! BetPlaced(b.marketId, b.instruction.selectionId, id)                                           // Order Placed -> Executing
      if (b.cancel) {
        goto (Cancelling) using Bet(b.marketId, b.instruction.selectionId, id, subscriber = b.subscriber)
      } else if (b.newPrice.isDefined) {
        goto (Replacing) using Bet(b.marketId, b.instruction.selectionId, id, subscriber = b.subscriber, newPrice = b.newPrice)
      } else {
        goto (Executing) using Bet(b.marketId, b.instruction.selectionId, id, subscriber = b.subscriber)
      }
    case Event(OrderManagerException(_), b: BetInstruction) =>
      b.subscriber ! BetFailed(b.marketId, b.instruction)                                                           // Order Failed -> Idle
      goto (Idle)
    case Event(PlaceExecutionReportContainer(_), b: BetInstruction) =>
      b.subscriber ! BetFailed(b.marketId, b.instruction)                                                           // Order Failed -> Idle
      goto (Idle)
    case Event(CancelBet, b: BetInstruction) => stay using b.copy(cancel = true)                                    // Order to be cancelled on confirmation
    case Event(ReplaceBet(newPrice), b: BetInstruction) => stay using b.copy(newPrice = Some(newPrice))             // Order to be replaced on confirmation
    case _ => stay()
  }

  /*
    Waiting for order to execute
   */
  when(Executing) {
    case Event(CancelBet, _) => goto (Cancelling)                                                             // Cancel Order
    case Event(ReplaceBet(newPrice), b: Bet) => goto (Replacing) using b.copy(newPrice = Some(newPrice))      // Replace Order

    // MarketBookUpdate
    case Event(MarketBookUpdate(_, marketBook), bet: Bet) => marketBook.getOrder(bet.selectionId, bet.id) match {
      case Some(order) if order.status == OrderStatus.EXECUTION_COMPLETE =>
        bet.subscriber ! BetExecuted(bet.marketId, bet.selectionId, order)
        goto (Idle) using bet.copy(order = Some(order))                               // Bet Executed -> Idle
      case Some(order) if bet.order.isEmpty || order != bet.order.get =>
        bet.subscriber ! BetUpdated(bet.marketId, bet.selectionId, order)
        stay using bet.copy(order = Some(order))                                      // Bet Updated -> Executing
      case _ => stay()
    }
    case _ => stay()
  }

  onTransition {
    case _ -> Cancelling =>
      val b = nextStateData.asInstanceOf[Bet]
      controller ! CancelOrders(b.marketId, Set(CancelInstruction(b.id)))
  }

  /*
    Cancelling order
   */
  when(Cancelling) {
    case Event(CancelExecutionReportContainer(result), b: Bet) => result.status match {
      case ExecutionReportStatus.SUCCESS =>
        b.subscriber ! BetCancelled(b.marketId, b.selectionId, b.id)                      // Bet Cancelled
        goto (Executing)
      case _ =>
        b.subscriber ! CancelFailed(b.marketId, b.selectionId, b.id)                      // Cancel Bet Failed
        goto (Executing)
    }
    case Event(OrderManagerException(_), b: Bet) =>
      b.subscriber ! CancelFailed(b.marketId, b.selectionId, b.id)                      // Cancel Bet Failed
      goto (Executing)
    case _ => stay()
  }

  onTransition {
    case _ -> Replacing =>
      val b = nextStateData.asInstanceOf[Bet]
      controller ! ReplaceOrders(b.marketId, Set(ReplaceInstruction(b.id, b.newPrice.get)))
  }

  /*
    Replacing order
   */
  when(Replacing) {
    case Event(ReplaceExecutionReportContainer(result), b: Bet) => result.status match {
      case ExecutionReportStatus.SUCCESS =>                                                       // Bet Replaced
        val newId = result.instructionReports.head.placeInstructionReport.get.betId.get
        b.subscriber ! BetReplaced(b.marketId, b.selectionId, b.id, b.newPrice.get, newId)
        goto (Executing) using Bet(b.marketId, b.selectionId, newId, subscriber = b.subscriber)
      case _ =>
        b.subscriber ! ReplaceFailed(b.marketId, b.selectionId, b.id, b.newPrice.get)
        goto (Executing) using b.copy(newPrice = None)
    }
    case Event(OrderManagerException(_), b: Bet) =>
      b.subscriber ! ReplaceFailed(b.marketId, b.selectionId, b.id, b.newPrice.get)           // Replace Bet Failed
      goto (Executing) using b.copy(newPrice = None)
    case _ => stay()
  }
}

object BetHandler {
  sealed trait State
  case object Idle extends State
  case object Confirming extends State
  case object Executing extends State
  case object Replacing extends State
  case object Cancelling extends State

  sealed trait Data
  case object NoData extends Data
  case class BetInstruction(marketId: String, instruction: PlaceInstruction, subscriber: ActorRef, cancel: Boolean = false, newPrice: Option[Double] = None) extends Data
  case class Bet(marketId: String, selectionId: Long, id: String, order: Option[Order] = None, subscriber: ActorRef, newPrice: Option[Double] = None) extends Data

  final case class PlaceBet(marketId: String, instruction: PlaceInstruction)
  case object CancelBet
  final case class ReplaceBet(newPrice: Double)

  final case class BetFailed(marketId: String, instruction: PlaceInstruction)
  final case class CancelFailed(marketId: String, selectionId: Long, betId: String)
  final case class ReplaceFailed(marketId: String, selectionId: Long, betId: String, newPrice: Double)

  final case class BetPlaced(marketId: String, selectionId: Long, betId: String)
  final case class BetCancelled(marketId: String, selectionId: Long, betId: String)
  final case class BetReplaced(marketId: String, selectionId: Long, betId: String, newPrice: Double, newId: String)
  final case class BetUpdated(marketId: String, selectionId: Long, order: Order)
  final case class BetExecuted(marketId: String, selectionId: Long, order: Order)

  def props(controller: ActorRef) = Props(new BetHandler(controller))
}

