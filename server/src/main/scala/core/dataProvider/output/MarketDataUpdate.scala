package core.dataProvider.output

import domain.ListMarketBookContainer
import org.joda.time.DateTime
import play.api.libs.json.Json

case class MarketDataUpdate(listMarketBookContainer: ListMarketBookContainer) extends DataProviderOutput {
  val timestamp = DateTime.now()
}

object MarketDataUpdate {
  implicit val formatMarketUpdate = Json.format[MarketDataUpdate]
}


