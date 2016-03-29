package core.autotrader

import akka.actor.{ActorRef, FSM, Props, Stash}
import core.api.commands._
import core.autotrader.Runner._
import core.dataProvider.polling.BEST
import core.orderManager.OrderManager.{OrderExecuted, OrderPlaced, OrderUpdated}
import core.orderManager.OrderManagerException
import domain.Side._
import domain.{Side, _}
import org.joda.time.DateTime

import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

class Runner(controller: ActorRef) extends FSM[State, Data] with Stash {

  def getTimeTill(time: DateTime): FiniteDuration = Math.max(1, time.getMillis - DateTime.now().getMillis) millis

  // The following three functions have been wrapped so calling them can be tested

  def unstashAllMsg(): Unit = unstashAll()

  def stashMsg(): Unit = stash()

  def resendMarketBook(s: Strategy): Unit = if (s.marketBook.isDefined) self ! s.marketBook.get

  startWith(Idle, NoData)

  onTransition {
    case x -> y => println("Changing from state ", x, "to", y, "with", stateData, " to ", nextStateData)
  }

  whenUnhandled {
    case Event(e, s) =>
      println("Unhandled event", stateName, e, s)
      stay()
  }

  when(Idle) {
    case Event(Init(strategy), NoData) =>
      controller ! SubscribeToMarkets(Set(strategy.marketId), BEST)
      controller ! SubscribeToOrderUpdates(Some(strategy.marketId), Some(strategy.selectionId))
      controller ! ListMarketBook(Set(strategy.marketId))
      setTimer("StartTimer", Start, getTimeTill(strategy.startTime.getOrElse(DateTime.now())))
      goto (Initialised) using strategy
  }

  when(Initialised) {
    case Event(e: MarketBookUpdate, strategy: Strategy) => stay using strategy.copy(marketBook = Some(e))

    case Event(Start, s: Strategy) =>
      // If a stopTime is defined set a timer
      if (s.stopTime.isDefined) {
        setTimer("StopTimer", Stop, getTimeTill(s.stopTime.get))
      }
      controller ! PlaceOrders(s.marketId, Set(s.instructions.head))
      goto (ConfirmingOrder) using s
  }

  when(ConfirmingOrder) {
    case Event(PlaceExecutionReportContainer(result), s: Strategy) if result.status == ExecutionReportStatus.SUCCESS =>       // Order Confirmed -> Save Id -> WaitingForOrder
      unstashAllMsg()
      resendMarketBook(s)
      goto (WaitingForOrder) using s.copy(
        instructions = s.instructions.tail,
        orders = OrderData(result.instructionReports.head.betId.get) :: s.orders
      )
    case Event(OrderManagerException(_), s: Strategy) =>                  // Order Failed
      controller ! PlaceOrders(s.marketId, Set(s.instructions.head))
      stay()
    case Event(PlaceExecutionReportContainer(_), s: Strategy) =>          // Order Failed
      controller ! PlaceOrders(s.marketId, Set(s.instructions.head))
      stay()
    case Event(x: MarketBookUpdate, strategy: Strategy) => stay using strategy.copy(marketBook = Some(x))        // Update internal state of the marketBook
    case Event(e, s) =>                 // Stash any other changes until confirmation has been received
      println("Stashing", e, s)
      stashMsg()
      stay()
  }

  def isStopTriggered(s: Strategy, m: MarketBook): Boolean = {
    val backPrice = m.getRunner(s.selectionId).backPrice
    val layPrice = m.getRunner(s.selectionId).layPrice
    (s.stopSide, backPrice, layPrice) match {
      case (Side.BACK, Some(_backPrice), _) if _backPrice <= s.stopPrice  => true
      case (Side.LAY, _, Some(_layPrice))   if _layPrice >= s.stopPrice   => true
      case _ => false
    }
  }

  when (WaitingForOrder) {
    case Event(e: OrderPlaced, s: Strategy) if e.order.betId == s.orders.head.id  => stay using s.copy(orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail)
    case Event(e: OrderUpdated, s: Strategy) if e.order.betId == s.orders.head.id => stay using s.copy(orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail)
    case Event(e: OrderExecuted, s: Strategy) if e.order.betId == s.orders.head.id =>
      if (s.instructions.isEmpty) {
        goto (Finished) using s.copy(orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail)
      } else {
        controller ! PlaceOrders(s.marketId, Set(s.instructions.head))
        goto (ConfirmingOrder) using s.copy(orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail)
      }

    case Event(e: MarketBookUpdate, s: Strategy) =>
      if (!s.stoppingOut && isStopTriggered(s, e.data)) {
        cancelTimer("StopTimer")
        controller ! CancelOrders(s.marketId, Set(CancelInstruction(s.orders.head.id)))
        goto (CancellingOrder) using s.copy(marketBook = Some(e))
      } else {
        stay using s.copy(marketBook = Some(e))
      }
    case Event(Stop, s: Strategy) =>
      cancelTimer("StopTimer")
      controller ! CancelOrders(s.marketId, Set(CancelInstruction(s.orders.head.id)))
      goto (CancellingOrder)                                                             // Timer triggers stop
  }

  def getPosition(orders: List[OrderData]): Double = orders.map(_.order match {
    case Some(x) if x.side == Side.LAY => x.sizeMatched
    case Some(x) if x.side == Side.BACK => -x.sizeMatched
    case _ => 0.0
  }).sum

  def getStopInstruction(s: Strategy): Option[PlaceInstruction] = getPosition(s.orders) match {
    case x if x > 0 => Some(PlaceInstruction(OrderType.LIMIT, s.selectionId, 0.0, Side.BACK, Some(LimitOrder(Math.abs(x), 1.01, PersistenceType.PERSIST))))
    case x if x < 0 => Some(PlaceInstruction(OrderType.LIMIT, s.selectionId, 0.0, Side.LAY, Some(LimitOrder(Math.abs(x), 1000, PersistenceType.PERSIST))))
    case _ => None
  }

  when(CancellingOrder) {
    case Event(CancelExecutionReportContainer(result), _) if result.status == ExecutionReportStatus.SUCCESS =>          // Order Cancelled, Wait for Execution Report
      stay()
    case Event(OrderManagerException(_), s: Strategy)          =>         // Cancel Failed
      controller ! CancelOrders(s.marketId, Set(CancelInstruction(s.orders.head.id)))
      stay()
    case Event(CancelExecutionReportContainer(_), s: Strategy) =>         // Cancel Failed
      controller ! CancelOrders(s.marketId, Set(CancelInstruction(s.orders.head.id)))
      stay()
    case Event(e: MarketBookUpdate, s: Strategy) => stay using s.copy(marketBook = Some(e))

    case Event(e: OrderExecuted, s: Strategy) if e.order.betId == s.orders.head.id =>
      val orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail
      val instruction = getStopInstruction(s.copy(orders = orders))

      if (instruction.isDefined) {
        controller ! PlaceOrders(s.marketId, Set(instruction.get))
        goto (ConfirmingOrder) using s.copy(orders = orders, instructions = List(instruction.get))
      } else {
        goto (Finished) using s.copy(orders = orders)
      }
  }

  onTransition {
    case _ -> Finished =>
      cancelTimer("StartTimer")
      cancelTimer("StopTimer")
  }

  when(Finished) {
    case Event(_, _) => stay()
  }

  onTermination {
    case StopEvent(_, _, _) =>
      println("STRATEGY STOPPED")
  }
}

object Runner {
  sealed trait State
  case object Idle extends State
  case object Initialised extends State
  case object ConfirmingOrder extends State
  case object WaitingForOrder extends State
  case object CancellingOrder extends State
  case object Finished extends State

  case object Stop
  case object Start
  case class Init(data: Strategy)
  case object Quit

  case class OrderData(id: String, order: Option[CurrentOrderSummary] = None)

  trait Data

  case object NoData extends Data

  case class Strategy(marketId: String,
                      selectionId: Long,
                      marketBook: Option[MarketBookUpdate] = None,
                      instructions: List[PlaceInstruction] = List.empty,
                      orders: List[OrderData] = List.empty,
                      stopPrice: Double,
                      stopSide: Side,
                      stoppingOut: Boolean = false,
                      startTime: Option[DateTime] = None,
                      stopTime: Option[DateTime] = None) extends Data

  def props(controller: ActorRef) = Props(new Runner(controller))
}