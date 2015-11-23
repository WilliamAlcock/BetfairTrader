package core.api

import play.api.libs.json.Json

case class ListEvents(eventTypeId: String) extends Command

object ListEvents {
  implicit val formatListEvents = Json.format[ListEvents]
}
