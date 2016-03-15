package domain

import domain.ExecutionReportErrorCode.ExecutionReportErrorCode
import domain.ExecutionReportStatus.ExecutionReportStatus
import play.api.libs.json.Json

case class PlaceExecutionReport(status: ExecutionReportStatus,
                                marketId: String,
                                errorCode: Option[ExecutionReportErrorCode],
                                instructionReports: Set[PlaceInstructionReport],
                                customerRef: Option[String])

object PlaceExecutionReport {
  implicit val formatPlaceExecutionReport = Json.format[PlaceExecutionReport]
}
