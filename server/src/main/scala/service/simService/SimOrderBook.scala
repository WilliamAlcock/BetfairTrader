package service.simService

import akka.actor.{Actor, Props}
import domain.{InstructionReportErrorCode, _}
import service.simService.SimOrderBook._

import scala.collection.immutable.HashMap

class SimOrderBook(var markets: HashMap[String, MarketOrderBook] = HashMap.empty,
                   reportFactory: ReportFactory = ReportFactory) extends Actor {

  override def receive = {
    case PlaceOrders(marketId, instructions, customerRef) => sender ! placeOrders(marketId, instructions, customerRef)
    case CancelOrders(marketId, instructions, customerRef) => sender ! cancelOrders(marketId, instructions, customerRef)
    case ReplaceOrders(marketId, instructions, customerRef) => sender ! replaceOrders(marketId, instructions, customerRef)
    case UpdateOrders(marketId, instructions, customerRef) => sender ! updateOrders(marketId, instructions, customerRef)
    case MatchOrders(marketBook) => sender ! matchOrders(marketBook)
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
          None, Some(LimitOnCloseOrder(order.bspLiability, instruction.newPrice)), None
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

  def matchOrders(listMarketBookContainer: ListMarketBookContainer): Option[ListMarketBookContainer] = {
    Some(ListMarketBookContainer(listMarketBookContainer.result.map(marketBook =>
      if (markets.contains(marketBook.marketId)) {
        markets = markets + (marketBook.marketId -> markets(marketBook.marketId).matchOrders(marketBook))
        markets(marketBook.marketId).updateMarketBook(marketBook)
      } else
        marketBook
    )))
  }
}

object SimOrderBook {
  def props(markets: HashMap[String, MarketOrderBook] = HashMap.empty,
            reportFactory: ReportFactory = ReportFactory) = Props(new SimOrderBook(markets = markets, reportFactory = reportFactory))

  case class PlaceOrders(marketId: String, instructions: Set[PlaceInstruction], customerRef: Option[String] = None)
  case class CancelOrders(marketId: String, instructions: Set[CancelInstruction], customerRef: Option[String] = None)
  case class ReplaceOrders(marketId: String, instructions: Set[ReplaceInstruction], customerRef: Option[String] = None)
  case class UpdateOrders(marketId: String, instructions: Set[UpdateInstruction], customerRef: Option[String] = None)
  case class MatchOrders(listMarketBookContainer: ListMarketBookContainer)
}