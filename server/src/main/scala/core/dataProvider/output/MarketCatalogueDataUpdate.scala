package core.dataProvider.output

import domain.ListMarketCatalogueContainer
import play.api.libs.json.Json

case class MarketCatalogueDataUpdate(listMarketCatalogueContainer: ListMarketCatalogueContainer) extends DataProviderOutput

object MarketCatalogueDataUpdate {
  implicit val formatMarketCatalogueUpdate = Json.format[MarketCatalogueDataUpdate]
}
