package domain

import domain.OrderType.OrderType
import domain.Side.Side
import play.api.libs.json.Json

case class PlaceInstruction(orderType: OrderType,
                            selectionId: Long,
                            handicap: Double,
                            side: Side,
                            limitOrder: Option[LimitOrder] = None,
                            limitOnCloseOrder: Option[LimitOnCloseOrder] = None,
                            marketOnCloseOrder: Option[MarketOnCloseOrder] = None) {
  val uniqueId:String = selectionId.toString + "-" + handicap.toString
}

object PlaceInstruction {
  implicit val formatPlaceInstruction = Json.format[PlaceInstruction]
}