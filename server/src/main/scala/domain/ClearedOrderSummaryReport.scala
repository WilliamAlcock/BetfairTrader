/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class ClearedOrderSummaryReport(currentOrders: Set[ClearedOrderSummary], moreAvailable: Boolean)

object ClearedOrderSummaryReport {
  implicit val formatClearedOrderSummaryReport = Json.format[ClearedOrderSummaryReport]
}