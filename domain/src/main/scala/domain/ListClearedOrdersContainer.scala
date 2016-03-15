/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class ListClearedOrdersContainer(result: ClearedOrderSummaryReport)

object ListClearedOrdersContainer {
  implicit val formatClearedOrdersContainer = Json.format[ListClearedOrdersContainer]
}