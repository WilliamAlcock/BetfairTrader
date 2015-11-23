package core.api

import domain.PlaceInstruction
import play.api.libs.json.Json

case class PlaceOrders(marketId: String, instructions: Set[PlaceInstruction], customerRef: Option[String] = None) extends Command

object PlaceOrders {
  implicit val formatPlaceOrders = Json.format[PlaceOrders]
}
