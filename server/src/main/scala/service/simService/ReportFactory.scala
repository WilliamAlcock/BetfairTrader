package service.simService

import domain.ExecutionReportErrorCode
import domain.ExecutionReportErrorCode._
import domain.ExecutionReportStatus
import domain.ExecutionReportStatus._
import domain.InstructionReportErrorCode.InstructionReportErrorCode
import domain.InstructionReportStatus
import domain.InstructionReportStatus._
import domain._
import org.joda.time.DateTime

// TODO add test suite

trait ReportFactory {
  def getStatus[T](reportStatus: Map[InstructionReportStatus, T]): ExecutionReportStatus = reportStatus match {
    case x if x.contains(InstructionReportStatus.FAILURE) && x.contains(InstructionReportStatus.SUCCESS) => ExecutionReportStatus.PROCESSED_WITH_ERRORS
    case x if x.contains(InstructionReportStatus.SUCCESS) => ExecutionReportStatus.SUCCESS
    case x => ExecutionReportStatus.FAILURE
  }

  def getErrorCode(status: ExecutionReportStatus): Option[ExecutionReportErrorCode] = status match {
    case ExecutionReportStatus.PROCESSED_WITH_ERRORS => Some(ExecutionReportErrorCode.PROCESSED_WITH_ERRORS)
    case _ => None
  }

  def getPlaceExecutionReportContainer(marketId: String,
                                       reports: Set[PlaceInstructionReport],
                                       customerRef: Option[String] = None): PlaceExecutionReportContainer = {
    val status = getStatus[Set[PlaceInstructionReport]](reports.groupBy(x => x.status))
    PlaceExecutionReportContainer(PlaceExecutionReport(status, marketId, getErrorCode(status), reports, customerRef))
  }

  def getCancelExecutionReportContainer(marketId: String,
                                        reports: Set[CancelInstructionReport],
                                        customerRef: Option[String] = None): CancelExecutionReportContainer = {
    val status = getStatus[Set[CancelInstructionReport]](reports.groupBy(x => x.status))
    CancelExecutionReportContainer(CancelExecutionReport(status, marketId, getErrorCode(status), reports, customerRef))
  }

  def getUpdateExecutionReportContainer(marketId: String,
                                        reports: Set[UpdateInstructionReport],
                                        customerRef: Option[String] = None): UpdateExecutionReportContainer = {
    val status = getStatus[Set[UpdateInstructionReport]](reports.groupBy(x => x.status))
    UpdateExecutionReportContainer(UpdateExecutionReport(status, marketId, getErrorCode(status), reports, customerRef))
  }

  def getReplaceExecutionReportContainer(marketId: String,
                                         reports: Set[ReplaceInstructionReport],
                                         customerRef: Option[String] = None): ReplaceExecutionReportContainer = {
    val status = getStatus[Set[ReplaceInstructionReport]](reports.groupBy(x => x.status))
    ReplaceExecutionReportContainer(ReplaceExecutionReport(customerRef,status, getErrorCode(status), marketId, reports))
  }

  def getPlaceInstructionReport(instruction: PlaceInstruction, order: Order): PlaceInstructionReport = PlaceInstructionReport(
    InstructionReportStatus.SUCCESS, None, instruction,
    Some(order.betId), Some(order.placedDate), Some(order.avgPriceMatched), Some(order.sizeMatched)
  )

  def getCancelInstructionReport(status: InstructionReportStatus,
                                 errorCode: Option[InstructionReportErrorCode],
                                 instruction: CancelInstruction,
                                 sizeToCancel: Option[Double]): CancelInstructionReport = CancelInstructionReport(
    status, errorCode, instruction, sizeToCancel, if (status == InstructionReportStatus.SUCCESS) Some(DateTime.now()) else None
  )

  def getUpdateInstructionReport(status: InstructionReportStatus,
                                 errorCode: Option[InstructionReportErrorCode],
                                 instruction: UpdateInstruction): UpdateInstructionReport =
    UpdateInstructionReport(status, errorCode, instruction)

  def getReplaceInstructionReport(cancelInstructionReport: Option[CancelInstructionReport],
                                  placeInstructionReport: Option[PlaceInstructionReport]): ReplaceInstructionReport = {

    val status = if (cancelInstructionReport.isDefined && cancelInstructionReport.get.status == InstructionReportStatus.SUCCESS)
      cancelInstructionReport.get.status else placeInstructionReport.get.status

    val errorCode = if (status == InstructionReportStatus.SUCCESS)
      None
    else if (cancelInstructionReport.getOrElse(status, InstructionReportStatus.FAILURE) == InstructionReportStatus.SUCCESS)
      Some(InstructionReportErrorCode.CANCELLED_NOT_PLACED)
    else
      Some(InstructionReportErrorCode.INVALID_BET_ID)

    ReplaceInstructionReport(status, errorCode, cancelInstructionReport, placeInstructionReport)
  }
}

object ReportFactory extends ReportFactory {}

