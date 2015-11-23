package domain

import domain.Side.Side
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes, Reads}

case class Match(betId: String,
                 matchId: String,
                 side: Side,
                 price: Double,
                 size: Double,
                 matchDate: DateTime)

object Match {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)
  implicit val formatMatch = Json.format[Match]
}