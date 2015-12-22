package domain

import domain.InstructionReportErrorCode.InstructionReportErrorCode
import domain.InstructionReportStatus.InstructionReportStatus
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes, Reads}

case class PlaceInstructionReport(status: InstructionReportStatus,
                                  errorCode: Option[InstructionReportErrorCode],
                                  instruction: PlaceInstruction,
                                  betId: Option[String],
                                  placedDate: Option[DateTime],
                                  averagePriceMatched: Option[Double],
                                  sizeMatched: Option[Double])

object PlaceInstructionReport {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)

  implicit val formatPlaceInstructionReport = Json.format[PlaceInstructionReport]
}