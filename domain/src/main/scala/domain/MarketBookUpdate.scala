package domain

import org.joda.time.DateTime
import play.api.libs.json.Json

case class MarketBookUpdate(timestamp: DateTime, data: MarketBook)

object MarketBookUpdate {
  implicit val marketBookUpdateFormat = Json.format[MarketBookUpdate]
}