package domain

import play.api.libs.json.{Writes, Reads}

object InstructionReportStatus extends Enumeration {
  type InstructionReportStatus = Value
  val SUCCESS, FAILURE, TIMEOUT = Value

  implicit def enumReads: Reads[InstructionReportStatus] = EnumUtils.enumReads(InstructionReportStatus)

  implicit def enumWrites: Writes[InstructionReportStatus] = EnumUtils.enumWrites
}