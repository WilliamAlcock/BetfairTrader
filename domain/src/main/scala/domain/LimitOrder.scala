package domain

import domain.PersistenceType.PersistenceType
import play.api.libs.json.Json

case class LimitOrder(size: Double, price: Double, persistenceType: PersistenceType)

object LimitOrder {
  implicit val formatLimitOrder = Json.format[LimitOrder]
}