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

  def getPosition(orders: List[OrderData]): Double = orders.map(_.order match {
    case Some(x) if x.side == Side.LAY => x.sizeRemaining
    case Some(x) if x.side == Side.BACK => -x.sizeRemaining
    case _ => 0.0
  }).sum

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
    case Event(e: MarketBookUpdate, strategy: Strategy) =>
      stay using strategy.copy(marketBook = Some(e))

    case Event(Start, strategy: Strategy) =>
      // If a stopTime is defined set a timer
      if (strategy.stopTime.isDefined) {
        setTimer("StopTimer", Stop, getTimeTill(strategy.stopTime.get))
      }
      goto (ConfirmingOrder) using strategy
  }

  onTransition {
    case _ -> ConfirmingOrder =>
      nextStateData match {
        case x: Strategy => controller ! PlaceOrders(x.marketId, Set(x.instructions.head))
      }
  }

  when(ConfirmingOrder) {
    case Event(PlaceExecutionReportContainer(result), strategy: Strategy) if result.status == ExecutionReportStatus.SUCCESS =>       // Order Confirmed -> Save Id -> WaitingForOrder
      unstashAll()
      goto (WaitingForOrder) using strategy.copy(
        instructions = strategy.instructions.tail,
        orders = OrderData(result.instructionReports.head.betId.get) :: strategy.orders
      )
    case Event(OrderManagerException(_), strategy: Strategy)          => goto (ConfirmingOrder)       // Order Failed
    case Event(PlaceExecutionReportContainer(_), strategy: Strategy)  => goto (ConfirmingOrder)       // Order Failed
    case Event(x: MarketBookUpdate, strategy: Strategy)               => stay using strategy.copy(marketBook = Some(x))        // Update internal state of the marketBook
    case Event(e, s) =>                 // Stash any other changes until confirmation has been received
      println("Stashing", e, s)
      stash()
      stay()
  }

  onTransition {
    case _ -> WaitingForOrder =>
      // Replay the last version of the marketBook received as it may trigger the stop
      nextStateData match {
        case s: Strategy => if (s.marketBook.isDefined) self ! s.marketBook.get
      }
  }

  when (WaitingForOrder) {
    case Event(e: OrderPlaced, s: Strategy) if e.order.betId == s.orders.head.id  => stay using s.copy(orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail)
    case Event(e: OrderUpdated, s: Strategy) if e.order.betId == s.orders.head.id => stay using s.copy(orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail)
    case Event(e: OrderExecuted, s: Strategy) if e.order.betId == s.orders.head.id =>
      if (s.instructions.isEmpty) {
        stop using s.copy(orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail, instructions = s.instructions.tail)
      } else {
        goto (ConfirmingOrder) using s.copy(orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail, instructions = s.instructions.tail)
      }

    case Event(e: MarketBookUpdate, s: Strategy) =>
      (s.stopSide, e.data.getRunner(s.selectionId).backPrice, e.data.getRunner(s.selectionId).layPrice) match {
        case (Side.BACK, Some(backPrice), _) if backPrice <= s.stopPrice  => goto (ConfirmingStopOrder) using s.copy(marketBook = Some(e))
        case (Side.LAY, _, Some(layPrice))   if layPrice >= s.stopPrice   => goto (ConfirmingStopOrder) using s.copy(marketBook = Some(e))
        case _ => stay using s.copy(marketBook = Some(e))
      }
    case Event(Stop, s: Strategy) => goto (CancellingOrder)                                                             // Timer triggers stop
  }

  onTransition {
    case _ -> CancellingOrder =>
      nextStateData match {
        case s: Strategy =>
          cancelTimer("StopTimer")
          controller ! CancelOrders(s.marketId, Set(CancelInstruction(s.orders.head.id)))
      }
  }

  def getStopInstruction(s: Strategy, size: Double): PlaceInstruction = {
    val price = s.stopSide match {
      case Side.BACK => 1.01
      case Side.LAY  => 1000
    }
    PlaceInstruction(OrderType.LIMIT, s.selectionId, 0.0, s.stopSide, Some(LimitOrder(size, price, PersistenceType.PERSIST)))
  }

  when(CancellingOrder) {
    case Event(CancelExecutionReportContainer(result), _) if result.status == ExecutionReportStatus.SUCCESS =>          // Order Cancelled, Wait for Execution Report
      stay()
    case Event(OrderManagerException(_), _)          => goto(CancellingOrder)         // Cancel Failed
    case Event(CancelExecutionReportContainer(_), _) => goto(CancellingOrder)         // Cancel Failed

    case Event(e: MarketBookUpdate, s: Strategy) => stay using s.copy(marketBook = Some(e))
    case Event(e, s) =>               // Stash any other changes until confirmation has been received
      println("Stashing", e, s)
      stash()
      stay()

    case Event(e: OrderExecuted, s: Strategy) if e.order.betId == s.orders.head.id =>
      val orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail
      val position = Math.abs(getPosition(orders))
      if (position > 0) {
        goto (ConfirmingOrder) using s.copy(instructions = List(getStopInstruction(s, position)))
      } else {
        stop using s.copy(orders = orders)
      }
  }

  onTransition {
    case _ -> ConfirmingOrder =>
      nextStateData match {
        case x: Strategy => controller ! PlaceOrders(x.marketId, Set(x.instructions.head))
      }
  }

  when(ConfirmingStopOrder) {
    case Event(PlaceExecutionReportContainer(result), s: Strategy) if result.status == ExecutionReportStatus.SUCCESS =>       // Order Confirmed
      unstashAll()
      goto (WaitingForStopOrder) using s.copy(
        instructions = s.instructions.tail,
        orders = OrderData(result.instructionReports.head.betId.get) :: s.orders
      )
    case Event(OrderManagerException(_), s: Strategy)          => goto (ConfirmingOrder)       // Order Failed
    case Event(PlaceExecutionReportContainer(_), s: Strategy)  => goto (ConfirmingOrder)       // Order Failed
    case Event(e: MarketBookUpdate, s: Strategy)               => stay using s.copy(marketBook = Some(e))        // Update internal state of the marketBook
    case Event(e, s) =>                 // Stash any other changes until confirmation has been received
      println("Stashing", e, s)
      stash()
      stay()
  }

  when(WaitingForStopOrder) {
    case Event(e: OrderPlaced, s: Strategy) if e.order.betId == s.orders.head.id  => stay using s.copy(orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail)
    case Event(e: OrderUpdated, s: Strategy) if e.order.betId == s.orders.head.id => stay using s.copy(orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail)
    case Event(e: OrderExecuted, s: Strategy) if e.order.betId == s.orders.head.id =>
      stop using s.copy(orders = s.orders.head.copy(order = Some(e.order)) :: s.orders.tail, instructions = s.instructions.tail)

    case Event(e: MarketBookUpdate, s: Strategy) => stay using s.copy(marketBook = Some(e))
  }

  onTermination {
    case StopEvent(FSM.Normal, state, data) => println("STRATEGY STOPPED")
  }
}

object Runner {
  sealed trait State
  case object Idle extends State
  case object Initialised extends State
  case object ConfirmingOrder extends State
  case object WaitingForOrder extends State
  case object CancellingOrder extends State
  case object ConfirmingStopOrder extends State
  case object WaitingForStopOrder extends State

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
                      instructions: List[PlaceInstruction],
                      orders: List[OrderData] = List.empty,
                      stopPrice: Double,
                      stopSide: Side,
                      startTime: Option[DateTime],
                      stopTime: Option[DateTime]) extends Data

  def props(controller: ActorRef) = Props(new Runner(controller))
}