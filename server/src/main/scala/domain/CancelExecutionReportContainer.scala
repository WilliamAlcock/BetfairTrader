package domain

import play.api.libs.json.Json

case class CancelExecutionReportContainer(result: CancelExecutionReport)

object CancelExecutionReportContainer {
  implicit val formatCancelExecutionReportContainer = Json.format[CancelExecutionReportContainer]
}