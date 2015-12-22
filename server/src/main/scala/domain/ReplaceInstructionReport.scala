/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import domain.InstructionReportErrorCode.InstructionReportErrorCode
import domain.InstructionReportStatus.InstructionReportStatus
import play.api.libs.json.Json

case class ReplaceInstructionReport(status: InstructionReportStatus,
                                    errorCode: Option[InstructionReportErrorCode],
                                    cancelInstructionReport: Option[CancelInstructionReport],
                                    placeInstructionReport: Option[PlaceInstructionReport])

object ReplaceInstructionReport {
  implicit val formatReplaceInstructionReport = Json.format[ReplaceInstructionReport]
}