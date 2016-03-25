package core.autotrader.layTheDraw

import akka.actor.{ActorRef, FSM, Props, Stash}
import core.api.commands.{PlaceOrders, SubscribeToMarkets, SubscribeToOrderUpdates}
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

final case class LayTheDrawConfig(marketId: String,
                                  selectionId: Long,
                                  entryPrice: Double,
                                  entryTime: Option[TimeRange],
                                  size: Double,
                                  exitPrice: Double
//                                  stopPrice: Double,
//                                  stopTime: Option[DateTime]
                                   ) extends StrategyConfig

object LayTheDrawConfig {
  implicit val formatLayTheDrawConfig = Json.format[LayTheDrawConfig]
}

class LayTheDraw(controller: ActorRef, config: LayTheDrawConfig) extends FSM[State, Data] with Stash with Strategy {

  controller ! SubscribeToMarkets(Set(config.marketId), BEST)
  controller ! SubscribeToOrderUpdates(Some(config.marketId), Some(config.selectionId))

  // Returns true if the timestamp is within the trading window, false otherwise
  // If the trading window is None this will always return true
  private def isInTimeRange(timestamp: DateTime, tradingWindow: Option[TimeRange]): Boolean = tradingWindow match {
    case Some(x) => timestamp.getMillis >= x.from.getOrElse(timestamp).getMillis && timestamp.getMillis <= x.to.getOrElse(timestamp).getMillis
    case _ => true
  }

  private def getPlaceInstruction(selectionId: Long, side: Side, size: Double, price: Double): PlaceInstruction = {
    PlaceInstruction(OrderType.LIMIT, selectionId, 0.0, side, limitOrder = Some(LimitOrder(size, price, PersistenceType.PERSIST)))
  }

  private def timeTillStop(stopTime: DateTime): Long = Math.max(1, stopTime.getMillis - DateTime.now().getMillis)

  startWith(Idle, NoOrders)

  onTransition {
    case x -> y => println("Changing from state ", x, "to", y, "with", stateData, " to ", nextStateData)
  }

  whenUnhandled {
    case Event(e, s) =>
      println("Unhandled event", stateName, e, s)
      stay()
  }

  when(Idle) {
    case Event(MarketBookUpdate(timestamp, marketBook), _) if isInTimeRange(timestamp, config.entryTime) =>
      println("I AM THE LAY THE DRAW STRATEGY", self)
      controller ! PlaceOrders(config.marketId, Set(getPlaceInstruction(config.selectionId, Side.LAY, config.size, config.entryPrice)))     // Place Lay Order
//      if (config.stopTime.isDefined) setTimer("stopOut", StopOut, timeTillStop(config.stopTime.get) millis)                                                 // If a stopTime is defined set a timer
      goto (ConfirmingLayBet) using NoOrders
  }

  when(ConfirmingLayBet) {
    case Event(PlaceExecutionReportContainer(result), NoOrders) if result.status == ExecutionReportStatus.SUCCESS =>       // Order Confirmed -> Save Id -> WaitingForLayBet
      unstashAll()
      goto (WaitingForLayBet) using LayId(result.instructionReports.head.betId.get)
    case Event(OrderManagerException(_), NoOrders) =>                                                      // Order Failed
      goto (Idle) using NoOrders
    case Event(PlaceExecutionReportContainer(_), NoOrders) =>                                              // Order Failed
      goto (Idle) using NoOrders
    case Event(e, s) =>                                                                                    // Stash any other changes until confirmation has been received
      println("Stashing", e, s)
      stash()
      stay()
  }

  when(WaitingForLayBet) {
    case Event(OrderPlaced(marketId, selectionId, handicap, order), LayId(id)) if order.betId == id =>                    // Lay Order Placed -> Save Order
      stay using LayOrder(order)
    case Event(OrderUpdated(marketId, selectionId, handicap, order), LayOrder(o)) if order.betId == o.betId =>            // Lay Order Updated -> Update Order
      stay using LayOrder(order)
    case Event(OrderExecuted(marketId, selectionId, handicap, order), LayOrder(o)) if order.betId == o.betId =>           // Lay Order Executed -> Place Back Order
      println("LAY BET EXECUTED", order)
      controller ! PlaceOrders(config.marketId, Set(getPlaceInstruction(config.selectionId, Side.BACK, order.sizeMatched, config.exitPrice)))
      goto (ConfirmingBackBet) using LayOrder(order)

//    // Stops
//    case Event(MarketBookUpdate(_, marketBook), _) => getRunner(marketBook, config.selectionId).backPrice match {
//      case Some(x) if x <= config.stopPrice => goto (CancelingBet)                                                      // Back Price goes triggers stop
//      case _ => stay()
//    }
//    case Event(StopOut, _) => goto (CancelingBet)                                                                       // Timer triggers stop
  }

  when(ConfirmingBackBet) {
    case Event(PlaceExecutionReportContainer(result), LayOrder(o)) =>          // Order Confirmed -> Save Id -> WaitingForBackBet
      unstashAll()
      goto (WaitingForBackBet) using LayOrderBackId(o, result.instructionReports.head.betId.get)
    case Event(OrderManagerException(_), LayOrder(o)) =>
      controller ! PlaceOrders(config.marketId, Set(getPlaceInstruction(config.selectionId, Side.BACK, o.sizeMatched, config.exitPrice)))       // Order Failed
      stay()
    case Event(PlaceExecutionReportContainer(_), LayOrder(o)) =>
      controller ! PlaceOrders(config.marketId, Set(getPlaceInstruction(config.selectionId, Side.BACK, o.sizeMatched, config.exitPrice)))       // Order Failed
      stay()
    case Event(e, s) =>                     // Stash any other changes until confirmation has been received
      println("Stashing", e, s)
      stash()
      stay()
  }

  when(WaitingForBackBet) {
    case Event(OrderPlaced(marketId, selectionId, handicap, order), LayOrderBackId(o, id)) if order.betId == id =>                // Back Order Placed -> Save Order
      stay using LayOrderBackOrder(o, order)
    case Event(OrderUpdated(marketId, selectionId, handicap, order), LayOrderBackOrder(lo, bo)) if order.betId == bo.betId =>     // Back Order Updated -> Update Order
      stay using LayOrderBackOrder(lo, order)
    case Event(OrderExecuted(marketId, selectionId, handicap, order), LayOrderBackOrder(lo, bo)) if order.betId == bo.betId =>    // Back Order Executed -> Finished
      stop using LayOrderBackOrder(lo, order)

//    // Stops
//    case Event(MarketBookUpdate(_, marketBook), _) => getRunner(marketBook, config.selectionId).backPrice match {
//      case Some(x) if x <= config.stopPrice => goto (CancelingBet)                                                      // Back Price goes triggers stop
//      case _ => stay()
//    }
//    case Event(StopOut, _) => goto (CancelingBet)                                                                       // Timer triggers stop
  }

//  onTransition {
//    case _ -> CancelingBet =>
//      cancelTimer("stopOut")
//      betHandler ! CancelBet
//  }
//
//  when(CancelingBet) {
//    case Event(CancelFailed(_, _, _), _) => goto (CancelingBet)
//    case Event(BetUpdated(_, _, order), o: Orders) => order.side match {
//      case Side.BACK => stay using o.copy(backOrder = Some(order))                                            // Bet Updated
//      case Side.LAY => stay using o.copy(layOrder = Some(order))
//    }
//    case Event(BetExecuted(_, _, order), o: Orders) => order.side match {
//      case Side.BACK => goto (StoppingOut) using o.copy(backOrder = Some(order))                              // Bet Cancelled
//      case Side.LAY => goto (StoppingOut) using o.copy(layOrder = Some(order))
//    }
//  }
//
//  when(StoppingOut) {
//    case Event(MarketBookUpdate(_, marketBook), o: Orders) =>
//      val runner = getRunner(marketBook, config.selectionId)
//      val position = Position.fromOrders(Some(Set(o.layOrder, o.backOrder).filter(_.isDefined).map(_.get)))
//      val size = Runner.getHedgeStake(position.backReturn - position.layLiability, runner.layPrice, runner.backPrice)
//      val price = Runner.getHedge(size, position.sumBacked, position.sumLaid)
//      betHandler ! PlaceBet(config.marketCatalogue.marketId, getPlaceInstruction(config.selectionId, Side.BACK, size, price))
//      goto (WaitingForStop)
//  }
//
//  // TODO handle stop not being triggered
//
//  when(WaitingForStop) {
//    case Event(BetFailed(_, _), _) => goto (StoppingOut)                                                                // Stop Bet Failed
//    case Event(BetPlaced(_, _, betId), o: Orders) => goto (WaitingForStop) using o.copy(stopId = Some(betId))           // Stop Bet Placed
//    case Event(BetUpdated(_, _, order), o: Orders) => goto (WaitingForStop) using o.copy(stopOrder = Some(order))       // Stop Bet Updated
//    case Event(BetExecuted(_, _, order), o: Orders) => goto (Exit) using o.copy(stopOrder = Some(order))                // Stop Bet Executed
//    case Event(MarketBookUpdate(timestamp, marketBook), o: Orders) if o.stopOrder.isDefined =>
//      val runner = getRunner(marketBook, config.selectionId)
//      if (runner.backPrice.isDefined && runner.backPrice.get < o.stopOrder.get.price) betHandler ! ReplaceBet(runner.backPrice.get)
//      stay()
//  }
}

object LayTheDraw {
  sealed trait State
  case object Idle extends State
  case object ConfirmingLayBet extends State
  case object WaitingForLayBet extends State
  case object ConfirmingBackBet extends State
  case object WaitingForBackBet extends State

//  case object CancelingBet extends State
//  case object StoppingOut extends State
//  case object WaitingForStop extends State
//  case object Exit extends State

  sealed trait Data
  case object NoOrders extends Data
  case class LayId(layId: String) extends Data
  case class LayOrder(layOrder: CurrentOrderSummary) extends Data
  case class LayOrderBackId(layOrder: CurrentOrderSummary, backId: String) extends Data
  case class LayOrderBackOrder(layOrder: CurrentOrderSummary, backOrder: CurrentOrderSummary) extends Data

//  case object StopOut

  def props(controller: ActorRef, config: LayTheDrawConfig) = Props(new LayTheDraw(controller, config))
}