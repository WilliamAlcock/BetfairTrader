package domain

import play.api.libs.json.Json

case class PlaceExecutionReportContainer(result: PlaceExecutionReport)

object PlaceExecutionReportContainer {
  implicit val formatPlaceExecutionReportContainer = Json.format[PlaceExecutionReportContainer]
}