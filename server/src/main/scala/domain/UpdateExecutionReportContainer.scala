package domain

import play.api.libs.json.Json

case class UpdateExecutionReportContainer(result: UpdateExecutionReport)

object UpdateExecutionReportContainer {
  implicit val formatUpdateExecutionReportContainer = Json.format[UpdateExecutionReportContainer]
}