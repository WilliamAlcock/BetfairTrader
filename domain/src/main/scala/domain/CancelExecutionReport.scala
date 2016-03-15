package domain

import domain.ExecutionReportErrorCode.ExecutionReportErrorCode
import domain.ExecutionReportStatus.ExecutionReportStatus
import play.api.libs.json.Json

case class CancelExecutionReport(status: ExecutionReportStatus,
                                 marketId: String,
                                 errorCode: Option[ExecutionReportErrorCode],
                                 instructionReports: Set[CancelInstructionReport],
                                 customerRef: Option[String])

object CancelExecutionReport {
  implicit val formatCancelExecutionReport = Json.format[CancelExecutionReport]
}
