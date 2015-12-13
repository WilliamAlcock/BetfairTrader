package core.api.commands

import core.dataProvider.polling.PollingGroup
import play.api.libs.json.Json

case class UnSubscribeFromMarkets(markets: List[String], pollingGroup: PollingGroup) extends Command

object UnSubscribeFromMarkets {
  implicit val formatSubscribeToMarkets = Json.format[UnSubscribeFromMarkets]
}