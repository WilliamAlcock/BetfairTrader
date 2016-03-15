/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import domain.ExecutionReportErrorCode.ExecutionReportErrorCode
import domain.ExecutionReportStatus.ExecutionReportStatus
import play.api.libs.json.Json

case class ReplaceExecutionReport(customerRef: Option[String],
                                  status: ExecutionReportStatus,
                                  errorCode: Option[ExecutionReportErrorCode],
                                  marketId: String,
                                  instructionReports: Set[ReplaceInstructionReport])

object ReplaceExecutionReport {
  implicit val formatReplaceExecutionReport = Json.format[ReplaceExecutionReport]
}