package domain

import org.joda.time.DateTime
import play.api.libs.json._

case class Runner(selectionId: Long,
                  handicap: Double,
                  status: String,
                  adjustmentFactor: Option[Double] = None,
                  lastPriceTraded: Option[Double] = None,
                  totalMatched: Option[Double] = None,
                  removalDate: Option[DateTime] = None,
                  sp: Option[StartingPrices] = None,
                  ex: Option[ExchangePrices] = None,
                  orders: Option[Set[Order]] = None,
                  matches: Option[Set[Match]] = None) {
  val uniqueId:String = selectionId.toString + "-" + handicap.toString
}

object Runner {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.optionWithNull(Writes.jodaDateWrites(dateFormat))
  implicit val readsRunner = Json.reads[Runner]
  implicit val writesRunner: Writes[Runner] = {
    new Writes[Runner] {
      def writes(r: Runner) = Json.obj(
        "selectionId" -> JsNumber(r.selectionId),
        "handicap" -> JsNumber(r.handicap),
        "status" -> JsString(r.status),
        "adjustmentFactor" -> Json.toJson(r.adjustmentFactor),
        "lastPriceTraded" -> Json.toJson(r.lastPriceTraded),
        "totalMatched" -> Json.toJson(r.totalMatched),
        "removalDate" -> Json.toJson(r.removalDate),
        "sp" -> Json.toJson(r.sp),
        "ex" -> Json.toJson(r.ex),
        "orders" -> Json.toJson(r.orders),
        "matches" -> Json.toJson(r.matches),
        "uniqueId" -> JsString(r.uniqueId)
      )
    }
  }
}