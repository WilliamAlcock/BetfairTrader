package core.api.output

import domain.PlaceExecutionReportContainer
import play.api.libs.json.Json

case class PlaceOrderResponse(placeExecutionReportContainer: PlaceExecutionReportContainer) extends Output

object PlaceOrderResponse {
  implicit val formatPlaceOrderResponse = Json.format[PlaceOrderResponse]
}
