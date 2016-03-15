package domain

import play.api.libs.json.Json

case class CurrentOrderSummaryReport(currentOrders: Set[CurrentOrderSummary], moreAvailable: Boolean)

object CurrentOrderSummaryReport {
  implicit val formatCurrentOrderSummaryReport = Json.format[CurrentOrderSummaryReport]
}
