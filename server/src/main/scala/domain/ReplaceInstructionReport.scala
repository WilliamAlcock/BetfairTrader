/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import domain.InstructionReportErrorCode.InstructionReportErrorCode
import domain.InstructionReportStatus.InstructionReportStatus
import play.api.libs.json.Json

case class ReplaceInstructionReport(override val status: InstructionReportStatus,
                                    override val errorCode: Option[InstructionReportErrorCode],
                                    cancelInstructionReport: CancelInstructionReport,
                                    placeInstructionReport: PlaceInstructionReport) extends InstructionReport(status, errorCode)

object ReplaceInstructionReport {
  implicit val formatReplaceInstructionReport = Json.format[ReplaceInstructionReport]
}