package core.api.output

import domain.UpdateExecutionReportContainer
import play.api.libs.json.Json

case class UpdateOrderResponse(updateExecutionReportContainer: UpdateExecutionReportContainer) extends Output

object UpdateOrderResponse {
  implicit val formatUpdateOrderResponse = Json.format[UpdateOrderResponse]
}