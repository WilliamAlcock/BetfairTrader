package core.api

import domain.UpdateInstruction
import play.api.libs.json.Json

case class UpdateOrders(marketId: String, instructions: Set[UpdateInstruction], customerRef: Option[String] = None) extends Command

object UpdateOrders {
  implicit val formatUpdateOrders = Json.format[UpdateOrders]
}

