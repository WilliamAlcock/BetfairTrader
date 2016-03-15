package domain

import domain.InstructionReportErrorCode.InstructionReportErrorCode
import domain.InstructionReportStatus.InstructionReportStatus
import play.api.libs.json.Json

case class UpdateInstructionReport(status: InstructionReportStatus,
                                   errorCode: Option[InstructionReportErrorCode],
                                   instruction: UpdateInstruction)

object UpdateInstructionReport {
  implicit val formatUpdateInstructionReport = Json.format[UpdateInstructionReport]
}