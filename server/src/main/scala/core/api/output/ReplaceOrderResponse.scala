package core.api.output

import domain.ReplaceExecutionReportContainer
import play.api.libs.json.Json

case class ReplaceOrderResponse(replaceExecutionReportContainer: ReplaceExecutionReportContainer) extends Output

object ReplaceOrderResponse {
  implicit val formatReplaceOrderResponse = Json.format[ReplaceOrderResponse]
}