package core.api

import play.api.libs.json.Json

case class StopPollingMarket(eventTypeId: String, eventId: String, marketId: String) extends Command

object StopPollingMarket {
  implicit val formatStopPollingMarket = Json.format[StopPollingMarket]
}
