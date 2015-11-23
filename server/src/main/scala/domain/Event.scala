package domain

import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes, Reads}

case class Event(id: String,
                 name: String,
                 countryCode: Option[String] = None,
                 timezone: String,
                 venue: Option[String] = None,
                 openDate: DateTime)

object Event {

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)
  implicit val formatEvent = Json.format[Event]
}