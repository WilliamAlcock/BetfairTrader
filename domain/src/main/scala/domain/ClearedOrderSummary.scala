/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import domain.OrderType.OrderType
import domain.PersistenceType.PersistenceType
import domain.Side.Side
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class ClearedOrderSummary(eventTypeId: String,
                               eventId: String,
                               marketId: String,
                               selectionId: Long,
                               handicap: Double,
                               betId: String,
                               placeDate: DateTime,
                               persistenceType: PersistenceType,
                               orderType: OrderType,
                               side: Side,
                               itemDescription: ItemDescription,
                               betOutcome: String,
                               priceRequested: Double,
                               settledDate: DateTime,
                               lastMatchedDate: DateTime,
                               betCount: Int,
                               commission: Double,
                               priceMatched: Double,
                               priceReduced: Boolean,
                               sizeSettled: Double,
                               profit: Double,
                               sizeCancelled: Double)

object ClearedOrderSummary {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val placeDateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val placeDateTimeWrites = Writes.jodaDateWrites(dateFormat)
  val clearedOrderSummaryPt1: OFormat[(String, String)] = ((__ \ "eventTypeId").format[String] ~ (__ \ "eventId").format[String]).tupled
  val clearedOrderSummaryPt2: OFormat[(String, Long, Double, String, DateTime, PersistenceType, OrderType, Side, ItemDescription,
                                       String, Double, DateTime, DateTime, Int, Double, Double, Boolean, Double, Double, Double)] =
  ((__ \ "marketId").format[String] ~
   (__ \ "selectionId").format[Long] ~
   (__ \ "handicap").format[Double] ~
   (__ \ "betId").format[String] ~
   (__ \ "placeDate").format[DateTime] ~
   (__ \ "persistenceType").format[PersistenceType] ~
   (__ \ "orderType").format[OrderType] ~
   (__ \ "side").format[Side] ~
   (__ \ "itemDescription").format[ItemDescription] ~
   (__ \ "betOutcome").format[String] ~
   (__ \ "priceRequested").format[Double] ~
   (__ \ "settledDate").format[DateTime] ~
   (__ \ "lastMatchedDate").format[DateTime] ~
   (__ \ "betCount").format[Int] ~
   (__ \ "commission").format[Double] ~
   (__ \ "priceMatched").format[Double] ~
   (__ \ "priceReduced").format[Boolean] ~
   (__ \ "sizeSettled").format[Double] ~
   (__ \ "profit").format[Double] ~
   (__ \ "sizeCancelled").format[Double]).tupled

  implicit val formatClearedOrderSummary: Format[ClearedOrderSummary] = (clearedOrderSummaryPt1 ~ clearedOrderSummaryPt2)({
    case ((eventTypeId, eventId), (marketId ,selectionId ,handicap ,betId ,placeDate ,persistenceType ,orderType ,side ,
                                   itemDescription ,betOutcome ,priceRequested ,settledDate ,lastMatchedDate ,betCount ,
                                   commission ,priceMatched ,priceReduced ,sizeSettled ,profit ,sizeCancelled)) =>
      new ClearedOrderSummary(eventTypeId, eventId, marketId, selectionId, handicap, betId, placeDate, persistenceType, orderType, side,
                              itemDescription, betOutcome, priceRequested, settledDate, lastMatchedDate, betCount,
                              commission, priceMatched, priceReduced, sizeSettled, profit, sizeCancelled)
    }, (clearedOrderSummary: ClearedOrderSummary) => ((clearedOrderSummary.eventTypeId, clearedOrderSummary.eventId),
        (clearedOrderSummary.marketId, clearedOrderSummary.selectionId, clearedOrderSummary.handicap, clearedOrderSummary.betId,
          clearedOrderSummary.placeDate, clearedOrderSummary.persistenceType, clearedOrderSummary.orderType, clearedOrderSummary.side,
          clearedOrderSummary.itemDescription, clearedOrderSummary.betOutcome, clearedOrderSummary.priceRequested, clearedOrderSummary.settledDate,
          clearedOrderSummary.lastMatchedDate, clearedOrderSummary.betCount, clearedOrderSummary.commission, clearedOrderSummary.priceMatched,
          clearedOrderSummary.priceReduced, clearedOrderSummary.sizeSettled, clearedOrderSummary.profit, clearedOrderSummary.sizeCancelled))
    )
}