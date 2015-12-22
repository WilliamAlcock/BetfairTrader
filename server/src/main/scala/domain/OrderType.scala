package domain

import play.api.libs.json.{Writes, Reads}

object OrderType extends Enumeration {
  type OrderType = Value
  val LIMIT, LIMIT_ON_CLOSE, MARKET_ON_CLOSE = Value

  implicit def enumReads: Reads[OrderType] = EnumUtils.enumReads(OrderType)

  implicit def enumWrites: Writes[OrderType] = EnumUtils.enumWrites
}
