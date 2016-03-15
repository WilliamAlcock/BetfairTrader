package core.autotrader.layTheDraw

import akka.actor.{ActorRef, FSM}
import core.api.commands.SubscribeToMarkets
import core.autotrader.betHandler.BetHandler
import core.autotrader.betHandler.BetHandler._
import core.autotrader.layTheDraw.LayTheDraw.{Data, Idle, State, _}
import core.dataProvider.polling.BEST
import domain.Side.Side
import domain._
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.language.postfixOps

class LayTheDraw(controller: ActorRef, config: Config) extends FSM[State, Data] {

  val betHandler: ActorRef = context.actorOf(BetHandler.props(controller))

  controller ! SubscribeToMarkets(Set(config.marketCatalogue.marketId), BEST)

  // Returns true if the timestamp is within the trading window, false otherwise
  // If the trading window is None this will always return true
  private def isInTimeRange(timestamp: DateTime, tradingWindow: Option[TimeRange]): Boolean = tradingWindow match {
    case Some(x) => timestamp.getMillis >= x.from.getOrElse(timestamp).getMillis && timestamp.getMillis <= x.to.getOrElse(timestamp).getMillis
    case _ => true
  }

  // TODO need to check the selection Id exists in the market catalogue
  private def getRunner(marketBook: MarketBook, selectionId: Long): Runner = marketBook.runners.find(_.selectionId == selectionId).get

  private def getPlaceInstruction(selectionId: Long, side: Side, size: Double, price: Double): PlaceInstruction = {
    PlaceInstruction(OrderType.LIMIT, selectionId, 0.0, side, limitOrder = Some(LimitOrder(size, price, PersistenceType.PERSIST)))
  }

  private def timeTillStop(stopTime: DateTime): Long = Math.max(1, stopTime.getMillis - DateTime.now().getMillis)

  startWith(Idle, NoOrders)

  when(Idle) {
    case Event(MarketBookUpdate(timestamp, marketBook), _) if isInTimeRange(timestamp, config.entryTime) =>
      betHandler ! PlaceBet(config.marketCatalogue.marketId, getPlaceInstruction(config.selectionId, Side.LAY, config.size, config.entryPrice))     // Place Lay bet with handler
      if (config.stopTime.isDefined) setTimer("stopOut", StopOut, timeTillStop(config.stopTime.get) millis)                                         // If a stopTime is defined set a timer
      goto (WaitingForLayBet) using NoOrders
  }

  when(WaitingForLayBet) {
    case Event(BetFailed(_, _), _) => goto (Idle) using NoOrders                                                        // Lay Bet Failed
    case Event(BetPlaced(_, _, betId), _) => goto (WaitingForLayBet) using Orders(betId)                                // Lay Bet Placed
    case Event(BetUpdated(_, _, order), o: Orders) => goto (WaitingForLayBet) using o.copy(layOrder = Some(order))      // Lay Bet Updated
    case Event(BetExecuted(_, _, order), o: Orders) => goto (WaitingForBackBet) using o.copy(layOrder = Some(order))    // Lay Bet Executed

    // Stops
    case Event(MarketBookUpdate(_, marketBook), _) => getRunner(marketBook, config.selectionId).backPrice match {
      case Some(x) if x <= config.stopPrice => goto (CancelingBet)                                                      // Back Price goes triggers stop
      case _ => stay()
    }
    case Event(StopOut, _) => goto (CancelingBet)                                                                       // Timer triggers stop
  }

  onTransition {
    case _ -> WaitingForBackBet => nextStateData match {
      case Orders(_, layOrder, _, _, _, _) if layOrder.isDefined =>
        betHandler ! PlaceBet(config.marketCatalogue.marketId, getPlaceInstruction(config.selectionId, Side.BACK, layOrder.get.sizeMatched, config.entryPrice))     // Place Back bet with handler
      case _ => // This should never happen
    }
  }

  when(WaitingForBackBet) {
    case Event(BetFailed(_, _), _) => goto (WaitingForBackBet)                                                          // Back Bet Failed
    case Event(BetPlaced(_, _, betId), o: Orders) => stay () using o.copy(backId = Some(betId))                         // Back Bet Placed
    case Event(BetUpdated(_, _, order), o: Orders) => stay() using o.copy(backOrder = Some(order))                      // Back Bet Updated
    case Event(BetExecuted(_, _, order), o: Orders) => goto (Exit) using o.copy(backOrder = Some(order))                // Back Bet Executed

    // Stops
    case Event(MarketBookUpdate(_, marketBook), _) => getRunner(marketBook, config.selectionId).backPrice match {
      case Some(x) if x <= config.stopPrice => goto (CancelingBet)                                                      // Back Price goes triggers stop
      case _ => stay()
    }
    case Event(StopOut, _) => goto (CancelingBet)                                                                       // Timer triggers stop
  }

  onTransition {
    case _ -> CancelingBet =>
      cancelTimer("stopOut")
      betHandler ! CancelBet
  }

  when(CancelingBet) {
    case Event(CancelFailed(_, _, _), _) => goto (CancelingBet)
    case Event(BetUpdated(_, _, order), o: Orders) => order.side match {
      case Side.BACK => stay using o.copy(backOrder = Some(order))                                            // Bet Updated
      case Side.LAY => stay using o.copy(layOrder = Some(order))
    }
    case Event(BetExecuted(_, _, order), o: Orders) => order.side match {
      case Side.BACK => goto (StoppingOut) using o.copy(backOrder = Some(order))                              // Bet Cancelled
      case Side.LAY => goto (StoppingOut) using o.copy(layOrder = Some(order))
    }
  }

  when(StoppingOut) {
    case Event(MarketBookUpdate(_, marketBook), o: Orders) =>
      val runner = getRunner(marketBook, config.selectionId)
      val position = Position.fromOrders(Some(Set(o.layOrder, o.backOrder).filter(_.isDefined).map(_.get)))
      val size = Runner.getHedgeStake(position.backReturn - position.layLiability, runner.layPrice, runner.backPrice)
      val price = Runner.getHedge(size, position.sumBacked, position.sumLaid)
      betHandler ! PlaceBet(config.marketCatalogue.marketId, getPlaceInstruction(config.selectionId, Side.BACK, size, price))
      goto (WaitingForStop)
  }

  // TODO handle stop not being triggered

  when(WaitingForStop) {
    case Event(BetFailed(_, _), _) => goto (StoppingOut)                                                                // Stop Bet Failed
    case Event(BetPlaced(_, _, betId), o: Orders) => goto (WaitingForStop) using o.copy(stopId = Some(betId))           // Stop Bet Placed
    case Event(BetUpdated(_, _, order), o: Orders) => goto (WaitingForStop) using o.copy(stopOrder = Some(order))       // Stop Bet Updated
    case Event(BetExecuted(_, _, order), o: Orders) => goto (Exit) using o.copy(stopOrder = Some(order))                // Stop Bet Executed
    case Event(MarketBookUpdate(timestamp, marketBook), o: Orders) if o.stopOrder.isDefined =>
      val runner = getRunner(marketBook, config.selectionId)
      if (runner.backPrice.isDefined && runner.backPrice.get < o.stopOrder.get.price) betHandler ! ReplaceBet(runner.backPrice.get)
      stay()
  }
}

object LayTheDraw {
  sealed trait State
  case object Idle extends State
  case object WaitingForLayBet extends State
  case object WaitingForBackBet extends State
  case object CancelingBet extends State
  case object StoppingOut extends State
  case object WaitingForStop extends State
  case object Exit extends State

  sealed trait Data
  case object NoOrders extends Data
  case class Orders(layId: String, layOrder: Option[Order] = None, backId: Option[String] = None, backOrder: Option[Order] = None,
                    stopId: Option[String] = None, stopOrder: Option[Order] = None) extends Data

  case object StopOut
  final case class Config(marketCatalogue: MarketCatalogue, selectionId: Long, entryPrice: Double, entryTime: Option[TimeRange],
                                    size: Double, exitPrice: Double, stopPrice: Double, stopTime: Option[DateTime]) {
    require(marketCatalogue.runners.get.exists(_.selectionId == selectionId), "selectionId must exist in marketCatalogue")
  }
}