package domain

import play.api.libs.json.Json

case class ListEventResultContainer(result: List[EventResult])

object ListEventResultContainer {
  implicit val formatEventResultContainer = Json.format[ListEventResultContainer]
}