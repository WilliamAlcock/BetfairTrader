package domain

import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes, Reads}

case class MarketCatalogue(marketId: String,
                           marketName: String,
                           marketStartTime: Option[DateTime],
                           description: Option[MarketDescription] = None,
                           totalMatched: Double,
                           runners: Option[List[RunnerCatalog]],
                           eventType: Option[EventType],
                           competition: Option[Competition] = None,
                           event: Event)

object MarketCatalogue {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)
  implicit val formatMarketCatalogue = Json.format[MarketCatalogue]
}