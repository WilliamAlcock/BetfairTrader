package core.dataProvider.output

import domain.ListEventResultContainer
import play.api.libs.json.Json

case class EventDataUpdate(listEventResultContainer: ListEventResultContainer) extends DataProviderOutput

object EventDataUpdate {
  implicit val formatEventUpdate = Json.format[EventDataUpdate]
}
