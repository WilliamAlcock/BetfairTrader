package domain

import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes, Reads}

case class MarketBook(marketId: String,
                      isMarketDataDelayed: Boolean,
                      status: String,
                      betDelay: Int,
                      bspReconciled: Boolean,
                      complete: Boolean,
                      inplay: Boolean,
                      numberOfWinners: Int,
                      numberOfRunners: Int,
                      numberOfActiveRunners: Int,
                      lastMatchTime: Option[DateTime],
                      totalMatched: Double,
                      totalAvailable: Double,
                      crossMatching: Boolean,
                      runnersVoidable: Boolean,
                      version: Long,
                      runners: Set[Runner])

object MarketBook {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)
  implicit val formatMarketBook = Json.format[MarketBook]
}