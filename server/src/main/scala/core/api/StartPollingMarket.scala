package core.api

import play.api.libs.json.Json

case class StartPollingMarket(eventTypeId: String, eventId: String, marketId: String) extends Command

object StartPollingMarket {
  implicit val formatPollMarket = Json.format[StartPollingMarket]
}
