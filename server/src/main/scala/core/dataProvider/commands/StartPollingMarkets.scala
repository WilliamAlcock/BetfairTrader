package core.dataProvider.commands

import core.dataProvider.polling.PollingGroup
import play.api.libs.json.Json

case class StartPollingMarkets(marketIds: List[String], subscriber: String, pollingGroup: PollingGroup) extends DataProviderCommand

object StartPollingMarkets {
  implicit val formatPollMarket = Json.format[StartPollingMarkets]
}
