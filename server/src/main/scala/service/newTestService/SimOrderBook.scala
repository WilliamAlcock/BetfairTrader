package service.newTestService

import akka.actor.Actor
import domain.ExecutionReportErrorCode.ExecutionReportErrorCode
import domain.ExecutionReportStatus.ExecutionReportStatus
import domain.InstructionReportStatus.InstructionReportStatus
import domain._

import scala.collection.immutable.HashMap

class SimOrderBook() extends Actor {

  var markets = HashMap.empty[String, MarketOrderBook]

  override def receive = {
    case x =>
  }

  def _placeOrder(marketId: String, instruction: PlaceInstruction): PlaceInstructionReport = {
    val report = markets.getOrElse(marketId, MarketOrderBook()).placeOrder(instruction)
    markets = markets + (marketId -> report.result)
    report.report
  }

  def getStatus[T](reportStatus: Map[InstructionReportStatus, T]): ExecutionReportStatus = reportStatus match {
    case x if x.contains(InstructionReportStatus.FAILURE) && x.contains(InstructionReportStatus.SUCCESS) => ExecutionReportStatus.PROCESSED_WITH_ERRORS
    case x if x.contains(InstructionReportStatus.SUCCESS) => ExecutionReportStatus.SUCCESS
    case x => ExecutionReportStatus.FAILURE
  }

  def getErrorCode(status: ExecutionReportStatus): Option[ExecutionReportErrorCode] = status match {
    case ExecutionReportStatus.PROCESSED_WITH_ERRORS => Some(ExecutionReportErrorCode.PROCESSED_WITH_ERRORS)
    case _ => None
  }

  def placeOrders(marketId: String, instructions: Set[PlaceInstruction], customerRef: Option[String] = None): PlaceExecutionReportContainer = {
    val reports = instructions.map(x => _placeOrder(marketId, x))
    val status = getStatus[Set[PlaceInstructionReport]](reports.groupBy(x => x.status))
    PlaceExecutionReportContainer(PlaceExecutionReport(status, marketId, getErrorCode(status), reports, customerRef))
  }

  def _cancelOrder(marketId: String, instruction: CancelInstruction): CancelInstructionReport = {
    markets.get(marketId) match {
      case Some(x) =>
        val report = x.cancelOrder(instruction)
        markets = markets + (marketId -> report.result)
        report.report
      case None =>
        CancelInstructionReport(
          InstructionReportStatus.FAILURE,
          Some(InstructionReportErrorCode.MARKET_NOT_OPEN_FOR_BETTING),
          instruction,
          None,
          None
        )
    }
  }

  def cancelOrders(marketId: String, instructions: Set[CancelInstruction], customerRef: Option[String] = None): CancelExecutionReportContainer = {
    val reports = instructions.map(x => _cancelOrder(marketId, x))
    val status = getStatus[Set[CancelInstructionReport]](reports.groupBy(x => x.status))
    CancelExecutionReportContainer(CancelExecutionReport(status, marketId, getErrorCode(status), reports, customerRef))
  }

  def replaceOrders(marketId: String, instructions: Set[ReplaceInstruction], customerRef: Option[String] = None) = {
  }

  def _updateOrder(marketId: String, instruction: UpdateInstruction) = {
    markets.get(marketId) match {
      case Some(x) =>
        val report = x.updateOrder(instruction)
        markets = markets + (marketId -> report.result)
        report.report
      case None =>
        UpdateInstructionReport(
          InstructionReportStatus.FAILURE,
          Some(InstructionReportErrorCode.MARKET_NOT_OPEN_FOR_BETTING),
          instruction
        )
    }
  }

  def updateOrders(marketId: String, instructions: Set[UpdateInstruction], customerRef: Option[String] = None): UpdateExecutionReportContainer = {
    val reports = instructions.map(x => _updateOrder(marketId, x))
    val status = getStatus[Set[UpdateInstructionReport]](reports.groupBy(x => x.status))
    UpdateExecutionReportContainer(UpdateExecutionReport(status, marketId, getErrorCode(status), reports, customerRef))
  }
}

