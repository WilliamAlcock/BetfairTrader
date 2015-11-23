/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes, Reads}

case class ItemDescription(eventTypeDesc: String,
                           eventDesc: String,
                           marketDesc: String,
                           marketType: String,
                           marketStartTime: DateTime,
                           runnerDesc: String,
                           numberOfWinners: Int,
                           eachWayDivisor: Double)

object ItemDescription {

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)

  implicit val formatItemDescription = Json.format[ItemDescription]
}