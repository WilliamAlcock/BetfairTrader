package domain

import domain.ExecutionReportErrorCode.ExecutionReportErrorCode
import domain.ExecutionReportStatus.ExecutionReportStatus
import play.api.libs.json.Json

case class UpdateExecutionReport(status: ExecutionReportStatus,
                                 marketId: String,
                                 errorCode: Option[ExecutionReportErrorCode],
                                 instructionReports: Set[UpdateInstructionReport],
                                 customerRef: Option[String])

object UpdateExecutionReport {
  implicit val formatUpdateExecutionReport = Json.format[UpdateExecutionReport]
}
