package core.dataProvider.output

import domain.ListMarketCatalogueContainer
import play.api.libs.json.Json

case class MarketCatalogueData(eventTypeId: String, listMarketCatalogueContainer: ListMarketCatalogueContainer) extends DataProviderOutput

object MarketCatalogueData {
  implicit val formatMarketCatalogueUpdate = Json.format[MarketCatalogueData]
}
