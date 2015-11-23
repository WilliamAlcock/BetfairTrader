package domain

import play.api.libs.json.Json

case class EventResult(event: Event, marketCount: Int)

object EventResult {
  implicit val formatEventResult = Json.format[EventResult]
}

