package core.api.commands

import core.dataProvider.polling.PollingGroup
import play.api.libs.json.Json

case class SubscribeToMarkets(markets: List[String], pollingGroup: PollingGroup) extends Command

object SubscribeToMarkets {
  implicit val formatSubscribeToMarkets = Json.format[SubscribeToMarkets]
}