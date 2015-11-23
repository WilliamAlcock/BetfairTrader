package domain

import domain.InstructionReportErrorCode.InstructionReportErrorCode
import domain.InstructionReportStatus.InstructionReportStatus
import play.api.libs.json.Json

case class UpdateInstructionReport(override val status: InstructionReportStatus,
                                   override val errorCode: Option[InstructionReportErrorCode],
                                   instruction: UpdateInstruction) extends InstructionReport(status, errorCode)

object UpdateInstructionReport {
  implicit val formatUpdateInstructionReport = Json.format[UpdateInstructionReport]
}