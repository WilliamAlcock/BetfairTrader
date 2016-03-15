package domain

import domain.OrderStatus.OrderStatus
import domain.OrderType.OrderType
import domain.PersistenceType.PersistenceType
import domain.Side.Side
import org.joda.time.DateTime
import play.api.libs.json.{Writes, Json, Reads}

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
                 sizeVoided: Double)

object Order {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)
  implicit val formatOrder = Json.format[Order]
}