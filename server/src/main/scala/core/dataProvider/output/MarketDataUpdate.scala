package core.dataProvider.output

import domain.ListMarketBookContainer
import play.api.libs.json.Json

case class MarketDataUpdate(listMarketBookContainer: ListMarketBookContainer) extends DataProviderOutput

object MarketDataUpdate {
  implicit val formatMarketUpdate = Json.format[MarketDataUpdate]
}


