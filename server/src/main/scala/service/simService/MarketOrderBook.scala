package service.simService

import domain.OrderProjection.OrderProjection
import domain._

import scala.collection.immutable.HashMap

case class MarketOrderBook(runners: HashMap[String, RunnerOrderBook] = HashMap.empty,
                           reportFactory: ReportFactory = ReportFactory,
                           utils: Utils = Utils) {

  def placeOrder(instruction: PlaceInstruction): PlaceOrderResponse[MarketOrderBook] = {
    val response = runners.getOrElse(instruction.uniqueId, new RunnerOrderBook()).placeOrder(instruction)
    PlaceOrderResponse(
      this.copy(runners = runners + (instruction.uniqueId -> response.result)),
      response.report
    )
  }

  def cancelOrder(instruction: CancelInstruction): CancelOrderResponse[MarketOrderBook] = {
    runners.find(x => x._2.hasBetId(instruction.betId)) match {
      case Some((id: String, runnerBook: RunnerOrderBook)) =>
        runners(id).cancelOrder(instruction) match {
          case x => CancelOrderResponse(this.copy(runners = runners + (id -> x.result)), x.report)
        }
      case None => CancelOrderResponse(
        this,
        reportFactory.getCancelInstructionReport(InstructionReportStatus.FAILURE, Some(InstructionReportErrorCode.INVALID_BET_ID), instruction, None)
      )   // Invalid BetId
    }
  }

  def updateOrder(instruction: UpdateInstruction): UpdateOrderResponse[MarketOrderBook] = {
    runners.find(x => x._2.hasBetId(instruction.betId)) match {
      case Some((id: String, runnerBook: RunnerOrderBook)) =>
        runners(id).updateOrder(instruction) match {
          case x => UpdateOrderResponse(this.copy(runners = runners + (id -> x.result)), x.report)
        }
      case None => UpdateOrderResponse(
        this,
        reportFactory.getUpdateInstructionReport(InstructionReportStatus.FAILURE, Some(InstructionReportErrorCode.INVALID_BET_ID), instruction)
      )   // Invalid BetId
    }
  }

  def matchOrders(marketBook: MarketBook): MarketOrderBook = {
    marketBook.runners.foldLeft[MarketOrderBook](this)((acc: MarketOrderBook, runner: Runner) =>
      runner.ex match {
        case Some(x) if acc.runners.isDefinedAt(runner.uniqueId) =>
          acc.copy(runners = acc.runners + (runner.uniqueId -> acc.runners(runner.uniqueId).matchOrders(
            x.availableToBack.sortBy(x => -x.price).head.price,
            x.availableToLay.sortBy(x => x.price).head.price
          )))
        case _ => acc
      }
    )
  }

  // TODO test
  def filterOrdersByProjection(orders: Set[Order], orderProjection: OrderProjection): Set[Order] = orderProjection match {
    case OrderProjection.EXECUTABLE => orders.filter(_.status == OrderStatus.EXECUTABLE)
    case OrderProjection.EXECUTION_COMPLETE => orders.filter(_.status == OrderStatus.EXECUTION_COMPLETE)
    case _ => orders
  }

  def updateMarketBook(marketBook: MarketBook, orderProjection: OrderProjection): MarketBook = {
    marketBook.copy(runners = marketBook.runners.map(runner =>
      if (runners.contains(runner.uniqueId)) {
        val orders = runners(runner.uniqueId).getOrders
        runner.copy(
          orders = Some(filterOrdersByProjection(orders, orderProjection)),
          matches = Some(runners(runner.uniqueId).getMatches),
          ex = utils.updateExchangePrices(runner.ex, orders)
        )
      } else {
        runner
      }
    ))
  }

  def getOrder(uniqueId: String, betId: String): Option[Order] = {
    if (runners.contains(uniqueId)) runners(uniqueId).getOrders.find(x => x.betId == betId) else None
  }

  // TODO test
  def getOrders: Map[String, Set[Order]] = runners.mapValues(_.getOrders)

  // TODO test
  def getOrderUniqueId(betId: String): String = runners.filter(_._2.hasBetId(betId)).keys.head
}
