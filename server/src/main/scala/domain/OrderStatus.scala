package domain

import play.api.libs.json.{Writes, Reads}

object OrderStatus extends Enumeration {
  type OrderStatus = Value
  val EXECUTION_COMPLETE, EXECUTABLE = Value

  implicit val enumReads: Reads[OrderStatus] = EnumUtils.enumReads(OrderStatus)

  implicit def enumWrites: Writes[OrderStatus] = EnumUtils.enumWrites
}