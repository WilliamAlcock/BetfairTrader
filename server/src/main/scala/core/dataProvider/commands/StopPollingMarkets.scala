package core.dataProvider.commands

import core.dataProvider.polling.PollingGroup
import play.api.libs.json.Json

case class StopPollingMarkets(marketIds: List[String], subscriber: String, pollingGroup: PollingGroup) extends DataProviderCommand

object StopPollingMarkets {
  implicit val formatStopPollingMarket = Json.format[StopPollingMarkets]
}
