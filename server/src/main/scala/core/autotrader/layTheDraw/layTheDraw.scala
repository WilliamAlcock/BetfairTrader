package core.autotrader.layTheDraw

import akka.actor.{ActorRef, FSM, Props, Stash}
import core.api.commands._
import core.autotrader.AutoTrader.AutoTraderException
import core.autotrader.layTheDraw.LayTheDraw.{Data, Idle, State, _}
import core.autotrader.{Strategy, StrategyConfig}
import core.dataProvider.polling.BEST
import core.orderManager.OrderManager.{OrderExecuted, OrderPlaced, OrderUpdated}
import core.orderManager.OrderManagerException
import domain.Side.Side
import domain._
import org.joda.time.DateTime
import play.api.libs.json.Json

import scala.language.postfixOps
import scala.concurrent.duration._

final case class LayTheDrawConfig(marketId: String,
                                  selectionId: Long,
                                  startTime: Option[DateTime],
                                  size: Double,
                                  layPrice: Double,
                                  backPrice: Double,
                                  stopPrice: Double,
                                  stopTime: Option[DateTime]
                                   ) extends StrategyConfig

object LayTheDrawConfig {
  implicit val formatLayTheDrawConfig = Json.format[LayTheDrawConfig]
}

trait LayTheDrawUtils {
  // Returns true if the timestamp is within the trading window, false otherwise
  // If the trading window is None this will always return true
  def isInTimeRange(timestamp: DateTime, tradingWindow: Option[TimeRange]): Boolean = tradingWindow match {
    case Some(x) => timestamp.getMillis >= x.from.getOrElse(timestamp).getMillis && timestamp.getMillis <= x.to.getOrElse(timestamp).getMillis
    case _ => true
  }

  def getTimeTill(time: DateTime): Long = Math.max(1, time.getMillis - DateTime.now().getMillis)
}

class LayTheDraw(controller: ActorRef, config: LayTheDrawConfig) extends FSM[State, Data] with Stash with Strategy with LayTheDrawUtils {

  controller ! SubscribeToMarkets(Set(config.marketId), BEST)
  controller ! SubscribeToOrderUpdates(Some(config.marketId), Some(config.selectionId))
  controller ! ListMarketBook(Set(config.marketId))

  startWith(Idle, Data())

  onTransition {
    case _ -> Idle =>
      config.startTime match {
        case Some(startTime) => setTimer("StartTimer", Start, getTimeTill(config.startTime.get) millis)
        case _ => self ! Start
      }
    case x -> y => println("Changing from state ", x, "to", y, "with", stateData, " to ", nextStateData)
  }

  whenUnhandled {
    case Event(e, s) =>
      println("Unhandled event", stateName, e, s)
      stay()
  }

  when(Idle) {
    case Event(x: MarketBookUpdate, data) => stay using data.copy(currentMarketBook = Some(x))

    case Event(Start, data) =>
      if (config.stopTime.isDefined) setTimer("stopTimeOut", StopTimeOut, getTimeTill(config.stopTime.get) millis)         // If a stopTime is defined set a timer
      goto (ConfirmingOrder) using data.copy(
        instructions = List(Lay(config.size, config.layPrice), Back(config.size, config.backPrice))                     // TODO these can come from config
      )
  }

  onTransition {
    case _ -> ConfirmingOrder => nextStateData.instructions.headOption match {
      case Some(instruction) => controller ! PlaceOrders(config.marketId, Set(instruction.getPlaceInstruction(config.selectionId)))
      case None => throw new AutoTraderException("Cannot transition to Confirming Order with no instructions")
    }
  }

  when(ConfirmingOrder) {
    case Event(PlaceExecutionReportContainer(result), data) if result.status == ExecutionReportStatus.SUCCESS =>       // Order Confirmed -> Save Id -> WaitingForLayBet
      unstashAll()
      goto (WaitingForOrder) using data.copy(
        instructions = data.instructions.tail,
        orders = OrderData(result.instructionReports.head.betId.get) :: data.orders
      )
    case Event(OrderManagerException(_), _)         => goto (ConfirmingOrder)                                        // Order Failed
    case Event(PlaceExecutionReportContainer(_), _) => goto (ConfirmingOrder)                                        // Order Failed
    case Event(x: MarketBookUpdate, data) =>                                                                            // Update internal state of the marketBook
      stay using data.copy(currentMarketBook = Some(x))
    case Event(e, s) =>                                                                                                 // Stash any other changes until confirmation has been received
      println("Stashing", e, s)
      stash()
      stay()
  }

  onTransition {
    case _ -> WaitingForOrder => if (nextStateData.currentMarketBook.isDefined) self ! nextStateData.currentMarketBook.get
  }

  when (WaitingForOrder) {
    case Event(OrderPlaced(marketId, selectionId, handicap, order), data) if order.betId == data.orders.head.id =>      // Order Placed -> Update the OrderData on the top of the orders list
      stay using data.copy(orders = data.orders.head.copy(order = Some(order)) :: data.orders.tail)
    case Event(OrderUpdated(marketId, selectionId, handicap, order), data) if order.betId == data.orders.head.id =>     // Order Updated -> Update the OrderData on the top of the order list
      stay using data.copy(orders = data.orders.head.copy(order = Some(order)) :: data.orders.tail)
    case Event(OrderExecuted(marketId, selectionId, handicap, order), data) if order.betId == data.orders.head.id =>    // Order Executed -> Place Back Order
      val newData = data.copy(
        orders = data.orders.head.copy(order = Some(order)) :: data.orders.tail,
        instructions = data.instructions.tail
      )
      data.instructions.isEmpty match {
        case true   => goto (Finished) using newData
        case false  => goto (ConfirmingOrder) using newData
      }

    // Stops
    case Event(x: MarketBookUpdate, data) =>
      (data.instructions.head, x.data.getRunner(config.selectionId).backPrice) match {
        case (StopOut(_, _, _), _) => stay using data.copy(currentMarketBook = Some(x))
        case (_, Some(backPrice)) if backPrice <= config.stopPrice =>                                                   // TODO this needs to work for layFirst and backFirst
          goto (CancellingOrder) using data.copy(
            currentMarketBook = Some(x),
            instructions = List.empty
          )
        case _ => stay using data.copy(currentMarketBook = Some(x))

      }
    case Event(StopTimeOut, data) => goto (CancellingOrder) using data.copy(instructions = List.empty)                      // Timer triggers stop
  }

  onTransition {
    case _ -> CancellingOrder =>
      cancelTimer("stopTimeOut")
      controller ! CancelOrders(config.marketId, Set(CancelInstruction(nextStateData.orders.head.id)))
  }

  when(CancellingOrder) {
    case Event(CancelExecutionReportContainer(result), _) if result.status == ExecutionReportStatus.SUCCESS =>          // Order Cancelled, Wait for Execution Report
      stay()
    case Event(OrderManagerException(_), _) =>                                                                          // Cancel Failed
      goto(CancellingOrder)
    case Event(CancelExecutionReportContainer(_), _) =>                                                                 // Cancel Failed
      goto(CancellingOrder)

    case Event(MarketBookUpdate(timestamp, marketBook), data) =>
      stay() using data.copy(currentMarketBook = Some(MarketBookUpdate(timestamp, marketBook)))
    case Event(e, s) => // Stash any other changes until confirmation has been received
      println("Stashing", e, s)
      stash()
      stay()

    case Event(OrderExecuted(marketId, selectionId, handicap, order), data) =>                                          // Order Executed
      val newData = data.copy(orders = data.orders.head.copy(order = Some(order)) :: data.orders.tail)
      newData.getPosition match {
        case x if x > 0 => goto (ConfirmingOrder) using newData.copy(instructions = List(StopOut(x, Side.BACK)))
        case x if x < 0 => goto (ConfirmingOrder) using newData.copy(instructions = List(StopOut(x, Side.LAY)))
        case _ =>
          unstashAll()
          goto (Finished) using newData
      }
  }

  when(Finished)
}

object LayTheDraw {
  sealed trait State
  case object Idle extends State
  case object ConfirmingOrder extends State
  case object WaitingForOrder extends State
  case object CancellingOrder extends State
  case object Finished extends State

  sealed trait AutoTraderInstruction {
    val size: Double
    val price: Double
    val side: Side

    def getPlaceInstruction(selectionId: Long): PlaceInstruction = {
      PlaceInstruction(OrderType.LIMIT, selectionId, 0.0, side, limitOrder = Some(LimitOrder(size, price, PersistenceType.PERSIST)))
    }
  }

  case class Back(size: Double, price: Double) extends AutoTraderInstruction {val side = Side.BACK}
  case class Lay(size: Double, price: Double) extends AutoTraderInstruction {val side = Side.LAY}
  case class StopOut(size: Double, side: Side) extends AutoTraderInstruction {
    val price = side match {
      case Side.BACK => 1.01
      case Side.LAY  => 1000
    }
  }

  case object StopTimeOut
  case object Start
  case object Quit

  case class OrderData(id: String, order: Option[CurrentOrderSummary] = None) {
    def getPosition: Double = order match {
      case Some(x) if x.side == Side.LAY => x.sizeRemaining
      case Some(x) if x.side == Side.BACK => -x.sizeRemaining
      case _ => 0.0
    }
  }

  case class Data(currentMarketBook: Option[MarketBookUpdate] = None, instructions: List[AutoTraderInstruction] = List.empty, orders: List[OrderData] = List.empty) {
    def getPosition: Double = orders.map(_.getPosition).sum
  }

  def props(controller: ActorRef, config: LayTheDrawConfig) = Props(new LayTheDraw(controller, config))
}