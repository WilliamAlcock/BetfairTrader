package domain

import domain.OrderStatus.OrderStatus
import domain.OrderType.OrderType
import domain.PersistenceType.PersistenceType
import domain.Side.Side
import org.joda.time.DateTime
import play.api.libs.json.Json

case class Order(betId: String,
                 orderType: OrderType,
                 status: OrderStatus,
                 persistenceType: PersistenceType,
                 side: Side,
                 price: Double,
                 size: Double,
                 bspLiability: Double,
                 placedDate: DateTime,
                 avgPriceMatched: Double,
                 sizeMatched: Double,
                 sizeRemaining: Double,
                 sizeLapsed: Double,
                 sizeCancelled: Double,
                 sizeVoided: Double) {

  override def hashCode = this.betId.hashCode

  override def equals(o: Any): Boolean = o match {
    case that: Order => that.betId equals this.betId
    case _ => false
  }
}

object Order {
  implicit val formatOrder = Json.format[Order]
}