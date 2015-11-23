package domain

import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes, Reads}

case class Runner(selectionId: Long,
                  handicap: Double,
                  status: String,
                  adjustmentFactor: Option[Double] = None,
                  lastPriceTraded: Option[Double] = None,
                  totalMatched: Option[Double] = None,
                  removalDate: Option[DateTime] = None,
                  sp: Option[StartingPrices] = None,
                  ex: Option[ExchangePrices] = None,
                  orders: Option[Set[Order]] = None,
                  matches: Option[Set[Match]] = None)

object Runner {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.optionWithNull(Writes.jodaDateWrites(dateFormat))
  implicit val formatRunner = Json.format[Runner]
}