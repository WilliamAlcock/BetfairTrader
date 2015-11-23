package core.api

import domain.ReplaceInstruction
import play.api.libs.json.Json

case class ReplaceOrders(marketId: String, instructions: Set[ReplaceInstruction], customerRef: Option[String] = None) extends Command

object ReplaceOrders {
  implicit val formatReplaceOrders = Json.format[ReplaceOrders]
}

