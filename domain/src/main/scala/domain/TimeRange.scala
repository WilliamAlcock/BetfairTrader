package domain

import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes, Reads}

case class TimeRange(from: Option[DateTime] = None, to: Option[DateTime] = None)

object TimeRange {

//  implicit val jodaDateWrites: Writes[org.joda.time.DateTime] = new Writes[org.joda.time.DateTime] {
//    def writes(d: org.joda.time.DateTime): JsValue = JsString(d.toString())
//  }

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)
  implicit val formatTimeRange = Json.format[TimeRange]
}