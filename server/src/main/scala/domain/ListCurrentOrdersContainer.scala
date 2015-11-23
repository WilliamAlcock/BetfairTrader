package domain

import play.api.libs.json.Json

case class ListCurrentOrdersContainer(result: CurrentOrderSummaryReport)

object ListCurrentOrdersContainer {
  implicit val formatCurrentOrdersContainer = Json.format[ListCurrentOrdersContainer]
}