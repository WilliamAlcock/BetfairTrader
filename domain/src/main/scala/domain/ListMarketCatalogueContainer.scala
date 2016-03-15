package domain

import play.api.libs.json.Json

case class ListMarketCatalogueContainer(result: List[MarketCatalogue])

object ListMarketCatalogueContainer {

  implicit val formatListMarketCatalogueContainer = Json.format[ListMarketCatalogueContainer]
}