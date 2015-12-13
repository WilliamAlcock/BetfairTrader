package core.dataProvider.output

import domain.ListEventTypeResultContainer
import play.api.libs.json.Json

case class EventTypeDataUpdate(listEventTypeResultContainer: ListEventTypeResultContainer) extends DataProviderOutput

object EventTypeDataUpdate {
  implicit val formatEventTypeUpdate = Json.format[EventTypeDataUpdate]
}

