package core.dataProvider.output

import domain.ListMarketBookContainer
import play.api.libs.json.Json

case class MarketData(eventTypeId: String, eventId: String, listMarketBookContainer: ListMarketBookContainer) extends DataProviderOutput

object MarketData {
  implicit val formatMarketUpdate = Json.format[MarketData]
}


