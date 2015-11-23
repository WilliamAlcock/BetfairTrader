package core.dataProvider.output

import domain.ListEventTypeResultContainer
import play.api.libs.json.Json

case class EventTypeData(listEventTypeResultContainer: ListEventTypeResultContainer) extends DataProviderOutput

object EventTypeData {
  implicit val formatEventTypeUpdate = Json.format[EventTypeData]
}

