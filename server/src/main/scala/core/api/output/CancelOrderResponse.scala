package core.api.output

import domain.CancelExecutionReportContainer
import play.api.libs.json.Json

case class CancelOrderResponse(cancelExecutionReportContainer: CancelExecutionReportContainer) extends Output

object CancelOrderResponse {
  implicit val formatCancelOrderResponse = Json.format[CancelOrderResponse]
}