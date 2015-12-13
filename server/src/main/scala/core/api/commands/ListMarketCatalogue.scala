package core.api.commands

import play.api.libs.json.Json

case class ListMarketCatalogue(marketIds: Set[String]) extends Command

object ListMarketCatalogue {
  implicit val formatListMarketCatalogue = Json.format[ListMarketCatalogue]
}
