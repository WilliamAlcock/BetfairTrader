package service.simService

import akka.actor.{Actor, Props}
import domain.OrderProjection.OrderProjection
import domain.OrderStatus.OrderStatus
import domain.{InstructionReportErrorCode, OrderType, PersistenceType, _}
import service.simService.SimOrderBook._

import scala.collection.immutable.HashMap

class SimOrderBook(var markets: HashMap[String, MarketOrderBook] = HashMap.empty,
                   reportFactory: ReportFactory = ReportFactory) extends Actor {

  override def receive = {
    case PlaceOrders(marketId, instructions, customerRef) => sender ! placeOrders(marketId, instructions, customerRef)
    case CancelOrders(marketId, instructions, customerRef) => sender ! cancelOrders(marketId, instructions, customerRef)
    case ReplaceOrders(marketId, instructions, customerRef) => sender ! replaceOrders(marketId, instructions, customerRef)
    case UpdateOrders(marketId, instructions, customerRef) => sender ! updateOrders(marketId, instructions, customerRef)
    case GetOrders(betIds, marketIds, status) => sender ! getOrders(betIds, marketIds, status)
    case MatchOrders(marketBook, orderProjection) => sender ! matchOrders(marketBook, orderProjection)
    case x => throw SimException("Invalid Message " + x)
  }

  def placeOrder(marketId: String, instruction: PlaceInstruction): PlaceInstructionReport = {
    val response = markets.getOrElse(marketId, MarketOrderBook()).placeOrder(instruction)
    markets = markets + (marketId -> response.result)
    response.report
  }

  def placeOrders(marketId: String, instructions: Set[PlaceInstruction], customerRef: Option[String] = None): PlaceExecutionReportContainer = {
    reportFactory.getPlaceExecutionReportContainer(marketId, instructions.map(x => placeOrder(marketId, x)), customerRef)
  }

  def cancelOrder(marketId: String, instruction: CancelInstruction): CancelInstructionReport = markets.get(marketId) match {
    case Some(x) =>
      val response = x.cancelOrder(instruction)
      markets = markets + (marketId -> response.result)
      response.report
    case None => reportFactory.getCancelInstructionReport(
      InstructionReportStatus.FAILURE,
      Some(InstructionReportErrorCode.MARKET_NOT_OPEN_FOR_BETTING),
      instruction,
      None
    )
  }

  def cancelOrders(marketId: String, instructions: Set[CancelInstruction], customerRef: Option[String] = None): CancelExecutionReportContainer = {
    reportFactory.getCancelExecutionReportContainer(marketId, instructions.map(x => cancelOrder(marketId, x)), customerRef)
  }

  def replaceOrder(marketId: String, instruction: ReplaceInstruction): ReplaceInstructionReport = {
    val cancelInstructionReport = cancelOrder(marketId, CancelInstruction(instruction.betId))

    val placeInstructionReport = if (cancelInstructionReport.status == InstructionReportStatus.SUCCESS) {
      val id = markets(marketId).getOrderUniqueId(instruction.betId)
      val side = markets(marketId).runners(id).getOrderSide(instruction.betId).get
      val order = markets(marketId).getOrder(id, instruction.betId).get
      val selectionId = id.split("-")(0).toLong
      val handicap = id.split("-")(1).toDouble

      val placeInstruction = order.orderType match {
        case OrderType.LIMIT => PlaceInstruction(
          order.orderType, selectionId, handicap, side,
          Some(LimitOrder(order.sizeRemaining, instruction.newPrice, PersistenceType.LAPSE)), None, None
        )
        case OrderType.LIMIT_ON_CLOSE => PlaceInstruction(
          order.orderType, selectionId, handicap, side,
          None, Some(LimitOnCloseOrder(0.0, instruction.newPrice)), None // TODO fix bspLiability - should not always be 0.0 !!
        )
        case _ => throw new SimException("Cannot replace order of this type")
      }
      Some(placeOrder(marketId, placeInstruction))
    } else {
      None
    }
    reportFactory.getReplaceInstructionReport(Some(cancelInstructionReport), placeInstructionReport)
  }

  def replaceOrders(marketId: String, instructions: Set[ReplaceInstruction], customerRef: Option[String] = None): ReplaceExecutionReportContainer = {
    reportFactory.getReplaceExecutionReportContainer(marketId, instructions.map(x => replaceOrder(marketId, x)), customerRef)
  }

  def updateOrder(marketId: String, instruction: UpdateInstruction) = markets.get(marketId) match {
    case Some(x) =>
      val response = x.updateOrder(instruction)
      markets = markets + (marketId -> response.result)
      response.report
    case None => reportFactory.getUpdateInstructionReport(
      InstructionReportStatus.FAILURE,
      Some(InstructionReportErrorCode.MARKET_NOT_OPEN_FOR_BETTING),
      instruction
    )
  }

  def updateOrders(marketId: String, instructions: Set[UpdateInstruction], customerRef: Option[String] = None): UpdateExecutionReportContainer = {
    reportFactory.getUpdateExecutionReportContainer(marketId, instructions.map(x => updateOrder(marketId, x)), customerRef)
  }

  def matchOrders(listMarketBookContainer: ListMarketBookContainer, orderProjection: OrderProjection): Option[ListMarketBookContainer] = {
    Some(ListMarketBookContainer(listMarketBookContainer.result.map(marketBook =>
      if (markets.contains(marketBook.marketId)) {
        markets = markets + (marketBook.marketId -> markets(marketBook.marketId).matchOrders(marketBook))
        markets(marketBook.marketId).updateMarketBook(marketBook, orderProjection)
      } else
        marketBook
    )))
  }

  // TODO this function has been moved to the domain package, move the test and refactor this class
  def getCurrentOrderSummary(marketId: String, uniqueId: String, order: Order): CurrentOrderSummary = CurrentOrderSummary(
    betId           = order.betId,
    marketId        = marketId,
    selectionId     = uniqueId.split("-")(0).toLong,
    handicap        = uniqueId.split("-")(1).toDouble,
    priceSize       = PriceSize(order.price, order.size),
    bspLiability    = order.bspLiability,
    side            = order.side,
    status          = order.status,
    persistenceType = order.persistenceType,
    orderType       = order.orderType,
    placedDate      = order.placedDate,
    matchedDate     = None,
    averagePriceMatched = order.avgPriceMatched,
    sizeMatched     = order.sizeMatched,
    sizeRemaining   = order.sizeRemaining,
    sizeLapsed      = order.sizeLapsed,
    sizeCancelled   = order.sizeCancelled,
    sizeVoided      = order.sizeVoided,
    regulatorCode   = ""
  )

  def getMarketOrders(marketId: String, marketOrderBook: MarketOrderBook): Set[CurrentOrderSummary] = {
    marketOrderBook.getOrders.map{case (k,v) => v.map(x => getCurrentOrderSummary(marketId, k, x))}.reduce(_ ++ _)
  }

  // TODO test this
  def getOrdersByMarket(marketIds: Seq[String]): Set[CurrentOrderSummary] = {
    val orders = if (marketIds.nonEmpty) {
      // Filter by markets
      marketIds.map(id => if (markets.contains(id)) getMarketOrders(id, markets(id)) else Set.empty[CurrentOrderSummary])
    } else {
      markets.map { case (k, v) => getMarketOrders(k, v)}
    }
    orders.flatten.toSet
  }

  // TODO test this
  def getOrders(betIds: Seq[String], marketIds: Seq[String], status: OrderStatus): Set[CurrentOrderSummary] = {
    val _betIds = betIds.toSet
    if (betIds.nonEmpty) {                                                                                  // Filter by betIds
      getOrdersByMarket(marketIds).filter(x => _betIds.contains(x.betId) && x.status == status)
    } else {
      getOrdersByMarket(marketIds).filter(x => x.status == status)
    }
  }
}

object SimOrderBook {
  def props(markets: HashMap[String, MarketOrderBook] = HashMap.empty,
            reportFactory: ReportFactory = ReportFactory) = Props(new SimOrderBook(markets = markets, reportFactory = reportFactory))

  case class PlaceOrders(marketId: String, instructions: Set[PlaceInstruction], customerRef: Option[String] = None)
  case class CancelOrders(marketId: String, instructions: Set[CancelInstruction], customerRef: Option[String] = None)
  case class ReplaceOrders(marketId: String, instructions: Set[ReplaceInstruction], customerRef: Option[String] = None)
  case class UpdateOrders(marketId: String, instructions: Set[UpdateInstruction], customerRef: Option[String] = None)
  case class MatchOrders(listMarketBookContainer: ListMarketBookContainer, orderProjection: OrderProjection)
  case class GetOrders(betIds: Seq[String], marketIds: Seq[String], status: OrderStatus)
}