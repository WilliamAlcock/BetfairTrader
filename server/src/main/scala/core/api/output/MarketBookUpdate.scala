package core.api.output

import domain.MarketBook
import org.joda.time.DateTime
import play.api.libs.json.Json

case class MarketBookUpdate(timestamp: DateTime, data: MarketBook) extends Output

object MarketBookUpdate {
  implicit val marketBookUpdateFormat = Json.format[MarketBookUpdate]
}