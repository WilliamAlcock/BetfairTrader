package core.api

import play.api.libs.json.Json

case class ListMarketCatalogue(eventTypeId: String, eventId: String) extends Command

object ListMarketCatalogue {
  implicit val formatListMarketCatalogue = Json.format[ListMarketCatalogue]
}
