package domain

import play.api.libs.json.Json

case class ListEventTypeResultContainer(result: List[EventTypeResult])

object ListEventTypeResultContainer {
  implicit val formatEventTypeResultContainer = Json.format[ListEventTypeResultContainer]
}