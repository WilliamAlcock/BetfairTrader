package domain

import domain.OrderStatus.OrderStatus
import domain.OrderType.OrderType
import domain.PersistenceType.PersistenceType
import domain.Side.Side
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes, Reads}

case class CurrentOrderSummary(betId: String,
                               marketId: String,
                               selectionId: Long,
                               handicap: Double,
                               priceSize: PriceSize,
                               bspLiability: Double,
                               side: Side,
                               status: OrderStatus,
                               persistenceType: PersistenceType,
                               orderType: OrderType,
                               placedDate: DateTime,
                               matchedDate: Option[DateTime] = None,
                               averagePriceMatched: Double,
                               sizeMatched: Double,
                               sizeRemaining: Double,
                               sizeLapsed: Double,
                               sizeCancelled: Double,
                               sizeVoided: Double,
                               regulatorCode: String)

object CurrentOrderSummary {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)
  implicit val formatCurrentOrderSummary = Json.format[CurrentOrderSummary]

  def fromOrder(marketId: String, uniqueId: String, order: Order): CurrentOrderSummary = CurrentOrderSummary(
    betId           = order.betId,
    marketId        = marketId,
    selectionId     = uniqueId.split("-")(0).toLong,
    handicap        = uniqueId.split("-")(1).toDouble,
    priceSize       = PriceSize(order.price, order.size),
    bspLiability    = order.bspLiability,
    side            = order.side,
    status          = order.status,
    persistenceType = order.persistenceType,
    orderType       = order.orderType,
    placedDate      = order.placedDate,
    matchedDate     = None,
    averagePriceMatched = order.avgPriceMatched,
    sizeMatched     = order.sizeMatched,
    sizeRemaining   = order.sizeRemaining,
    sizeLapsed      = order.sizeLapsed,
    sizeCancelled   = order.sizeCancelled,
    sizeVoided      = order.sizeVoided,
    regulatorCode   = ""
  )
}