package domain

import domain.InstructionReportErrorCode.InstructionReportErrorCode
import domain.InstructionReportStatus.InstructionReportStatus
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes, Reads}

case class PlaceInstructionReport(override val status: InstructionReportStatus,
                                  override val errorCode: Option[InstructionReportErrorCode],
                                  instruction: PlaceInstruction,
                                  betId: Option[String],
                                  placedDate: Option[DateTime],
                                  averagePriceMatched: Option[Double],
                                  sizeMatched: Option[Double]) extends InstructionReport(status, errorCode)

object PlaceInstructionReport {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)

  implicit val formatPlaceInstructionReport = Json.format[PlaceInstructionReport]
}