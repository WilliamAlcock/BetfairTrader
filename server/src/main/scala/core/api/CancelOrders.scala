package core.api

import domain.CancelInstruction
import play.api.libs.json.Json

case class CancelOrders(marketId: String, instructions: Set[CancelInstruction], customerRef: Option[String] = None) extends Command

object CancelOrders {
  implicit val formatCancelOrders = Json.format[CancelOrders]
}
