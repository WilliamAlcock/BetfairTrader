package core.autotrader

import akka.actor.{ActorRef, FSM}
import backTester.BackTester
import core.api.commands.CancelOrders
import core.autotrader.TestTrader._
import core.orderManager.OrderManager.{OrderExecuted, OrderUpdated, OrderPlaced}
import domain.Side.Side
import domain._
import indicators.Indicators
import randomForest.RandomForest
import service.simService.SimOrderBook.PlaceOrders

import scala.concurrent.duration._
import scala.language.postfixOps

class TestTrader(interval: Int, speed: Int, catalogue: MarketCatalogue, selectionId: Long, numberIndicators: Int, model: RandomForest, controller: ActorRef) extends FSM[State, Data] with BackTester {

  val size = 1.0

  private def getNextIndicator(r: RunningData): RunningData = {
    val indicator = getIndicatorFromMarketBook(r.marketBookData, selectionId, r.indicators, r.intervalStartTime, catalogue.marketStartTime.get.getMillis, priceToProbabilityOfLose)
    val prediction = model.getClassification(Indicators.getFeatures(indicator).map(_.getOrElse(0.0)))
    r.copy(
      marketBookData = List.empty[MarketBookUpdate],
      indicators = indicator :: r.indicators,
      predictions = prediction :: r.predictions,
      intervalStartTime = r.intervalStartTime + interval
    )
  }

  private def getCurrentOrderSummary(result: PlaceExecutionReport): CurrentOrderSummary = {
    CurrentOrderSummary(
      betId = result.instructionReports.head.betId.get,
      marketId = result.marketId,
      selectionId = result.instructionReports.head.instruction.selectionId,
      handicap = result.instructionReports.head.instruction.handicap,
      priceSize = PriceSize(result.instructionReports.head.instruction.limitOrder.get.price, result.instructionReports.head.instruction.limitOrder.get.size),
      bspLiability = 0.0,
      side = result.instructionReports.head.instruction.side,
      status = OrderStatus.EXECUTABLE,
      persistenceType = result.instructionReports.head.instruction.limitOrder.get.persistenceType,
      orderType = result.instructionReports.head.instruction.orderType,
      placedDate = result.instructionReports.head.placedDate.get,
      matchedDate = None,
      averagePriceMatched = result.instructionReports.head.averagePriceMatched.getOrElse(0.0),
      sizeMatched = result.instructionReports.head.sizeMatched.getOrElse(0.0),
      sizeRemaining = result.instructionReports.head.instruction.limitOrder.get.size - result.instructionReports.head.sizeMatched.getOrElse(0.0),
      sizeLapsed = 0.0,
      sizeCancelled = 0.0,
      sizeVoided = 0.0,
      regulatorCode = ""
    )
  }

  private def getPosition(r: RunningData): Double = {
    r.orders.map(order => order.side match {
      case Side.BACK => -order.sizeMatched
      case Side.LAY => +order.sizeMatched
    }).sum
  }

  private def hasOrder(o: CurrentOrderSummary, r: RunningData): Boolean = {
    r.orders.exists(x => x.betId == o.betId)
  }

  private def updateOrder(o: CurrentOrderSummary, r: RunningData): RunningData = {
    if (hasOrder(o, r)) {
      val (head: List[CurrentOrderSummary], tail: List[CurrentOrderSummary]) = r.orders.span(x => x.betId != o.betId)
      r.copy(orders = head ++ (o :: tail.tail))
    } else {
      r
    }
  }

  private def cancelLastTrade(r: RunningData) = {
    // only cancel last order if it exists and is executable
    if (r.orders.nonEmpty && r.orders.head.status == OrderStatus.EXECUTABLE) {
      controller ! CancelOrders(catalogue.marketId, Set(CancelInstruction(r.orders.head.betId)))      // Cancel current order
    }
  }

  // Hit market
  private def flattenPosition(position: Double, r: RunningData) = position match {
    case x if x > 0 =>
      val price = if (r.marketBookData.nonEmpty) {
        r.marketBookData.head.data.getRunner(selectionId).backPrice.get
      } else {
        r.indicators.head.tick.book.get.availableToBack.head.price
      }
      controller ! PlaceOrders(catalogue.marketId, Set(PlaceInstruction(OrderType.LIMIT,selectionId,0.0,Side.BACK,Some(LimitOrder(size,price,PersistenceType.PERSIST)))))
    case x if x < 0 =>
      val price = if (r.marketBookData.nonEmpty) {
        r.marketBookData.head.data.getRunner(selectionId).layPrice.get
      } else {
        r.indicators.head.tick.book.get.availableToLay.head.price
      }
      controller ! PlaceOrders(catalogue.marketId, Set(PlaceInstruction(OrderType.LIMIT,selectionId,0.0,Side.LAY,Some(LimitOrder(size,price,PersistenceType.PERSIST)))))
    case x => // Do Nothing
  }

  // Join current bid/offer
  private def placeOrder(side: Side, r: RunningData) = {
    val price = if (r.marketBookData.nonEmpty) {
      side match {
        case Side.BACK  => r.marketBookData.head.data.getRunner(selectionId).layPrice.get
        case Side.LAY   => r.marketBookData.head.data.getRunner(selectionId).backPrice.get
      }
    } else {
      side match {
        case Side.BACK  => r.indicators.head.tick.book.get.availableToLay.head.price
        case Side.LAY   => r.indicators.head.tick.book.get.availableToBack.head.price
      }
    }
    controller ! PlaceOrders(catalogue.marketId, Set(PlaceInstruction(OrderType.LIMIT,selectionId,0.0,side,Some(LimitOrder(size,price,PersistenceType.PERSIST)))))
  }

  private def placeNewTrades(r: RunningData) = {
    if (r.predictions.nonEmpty && r.predictions.take(numberIndicators).count(x => x == r.predictions.head) == numberIndicators) {
      getPosition(r) match {
        case x if x > 0 => r.predictions.head match {             // currently LONG (LAY)
          case "DOWN" =>
            cancelLastTrade(r)
            flattenPosition(x, r)
            placeOrder(Side.BACK, r)
          case _ => // Do Nothing
        }
        case x if x < 0 => r.predictions.head match {             // currently SHORT (BACK)
          case "UP"   =>
            cancelLastTrade(r)
            flattenPosition(x, r)
            placeOrder(Side.LAY, r)
          case _ => // Do Nothing
        }
        case x => r.predictions.head match {                      // currently no position
          case "UP"   =>
            cancelLastTrade(r)
            placeOrder(Side.LAY, r)
          case "DOWN" =>
            cancelLastTrade(r)
            placeOrder(Side.BACK, r)
          case _ => // Do Nothing
        }
      }
    } else r
  }

  onTransition {
    case Idle -> CollectingData => setTimer("Interval", GetNextInterval, (interval / speed) millis, repeat = true)        // Interval timer
  }

  whenUnhandled {
    case Event(PlaceExecutionReportContainer(result), r: RunningData) if result.status == ExecutionReportStatus.SUCCESS =>       // Order Confirmed -> Save Id -> WaitingForOrder
      stay using r.copy(orders = getCurrentOrderSummary(result) :: r.orders)
    case Event(e: OrderPlaced, r: RunningData)    => stay using updateOrder(e.order, r)
    case Event(e: OrderUpdated, r: RunningData)   => stay using updateOrder(e.order, r)
    case Event(e: OrderExecuted, r: RunningData)  => stay using updateOrder(e.order, r)
  }

  when(Idle) {
    case Event(x: MarketBookUpdate, NoData) =>
      val intervalStartTime = getIntervalStartTime(x.timestamp.getMillis, catalogue.marketStartTime.get.getMillis, interval)
      goto(Running) using RunningData(marketBookData = List(x), intervalStartTime = intervalStartTime)
  }

  when(CollectingData) {
    case Event(x: MarketBookUpdate, r: RunningData) => goto (CollectingData) using r.copy(marketBookData = x :: r.marketBookData)
    case Event(GetNextInterval, r: RunningData) =>
      val newData = getNextIndicator(r)
      if (newData.intervalStartTime + 1200000 >= catalogue.marketStartTime.get.getMillis) {
        goto(Running) using newData
      } else {
        goto (CollectingData) using newData
      }
  }

  when(Running) {
    case Event(x: MarketBookUpdate, r: RunningData) => goto (Running) using r.copy(marketBookData = x :: r.marketBookData)
    case Event(GetNextInterval, r: RunningData) =>
      val newData = getNextIndicator(r)
      if (newData.intervalStartTime >= catalogue.marketStartTime.get.getMillis) {
        flattenPosition(getPosition(r), r)
        goto (Finished) using newData
      } else {
        placeNewTrades(newData)
        goto (Running) using newData
      }
  }

//  when(Finished)

}

object TestTrader {
  trait State

  case object Idle extends State
  case object CollectingData extends State
  case object Running extends State
  case object Finished extends State

  trait Data
  case object NoData extends Data
  case class RunningData(marketBookData: List[MarketBookUpdate] = List.empty,
                         indicators: List[Indicators] = List.empty,
                         predictions: List[String] = List.empty,
                         pendingOrder: Option[CurrentOrderSummary] = None,
                         orders: List[CurrentOrderSummary] = List.empty,
                         intervalStartTime: Long) extends Data

  case object GetNextInterval
}
