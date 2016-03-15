package domain

import play.api.libs.json.Json

case class EventTypeResult(eventType: EventType, marketCount: Int)

object EventTypeResult {
  implicit val formatEventTypeResult = Json.format[EventTypeResult]
}