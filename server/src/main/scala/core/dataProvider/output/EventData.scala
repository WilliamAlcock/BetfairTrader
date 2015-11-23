package core.dataProvider.output

import domain.ListEventResultContainer
import play.api.libs.json.Json

case class EventData(eventTypeId: String, listEventResultContainer: ListEventResultContainer) extends DataProviderOutput

object EventData {
  implicit val formatEventUpdate = Json.format[EventData]
}
