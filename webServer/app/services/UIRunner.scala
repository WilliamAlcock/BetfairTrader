package services

import domain.{ExchangePrices, Order, Runner, Side}
import play.api.libs.json._

import scala.collection.immutable.Map

case class UIRunner(runner: Runner) {
  val ladder = UIRunner.getLadder(runner.orders.getOrElse(Set.empty[Order]), runner.ex.getOrElse(ExchangePrices(List(), List(), List())))
}

// TODO some duplication with server code, move functions into shared lib
object UIRunner {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeWrites = Writes.optionWithNull(Writes.jodaDateWrites(dateFormat))
  implicit val writesUIRunner: Writes[UIRunner] = new Writes[UIRunner] {
    def writes(r: UIRunner) = Json.obj(
      "selectionId" -> JsNumber(r.runner.selectionId),
      "handicap" -> JsNumber(r.runner.handicap),
      "status" -> JsString(r.runner.status),
      "adjustmentFactor" -> Json.toJson(r.runner.adjustmentFactor),
      "lastPriceTraded" -> Json.toJson(r.runner.lastPriceTraded),
      "totalMatched" -> Json.toJson(r.runner.totalMatched),
      "removalDate" -> Json.toJson(r.runner.removalDate),
      "sp" -> Json.toJson(r.runner.sp),
      "ex" -> Json.toJson(r.runner.ex),
      "orders" -> Json.toJson(r.runner.orders),
      "matches" -> Json.toJson(r.runner.matches),
      "uniqueId" -> JsString(r.runner.uniqueId),
      "backPrice" -> Json.toJson(r.runner.backPrice),
      "layPrice" -> Json.toJson(r.runner.layPrice),
      "position" -> Json.toJson(r.runner.position),
      "hedgeStake" -> Json.toJson(r.runner.hedgeStake),
      "hedge" -> Json.toJson(r.runner.hedge),
//      "weightOfMoney" -> Json.toJson(r.runner.weightOfMoney),
      "ladder" -> Json.toJson(r.ladder.map{case (k, v) => k.toString -> Json.toJson(v)})
    )
  }

  def getLadder(orders: Set[Order], exchangePrices: ExchangePrices): Map[Double, LadderRow] = {
    val sizeToLay = exchangePrices.availableToBack.map(x => (x.price, LadderRow(sizeToLay = x.size)))
    val sizeToBack = exchangePrices.availableToLay.map(x => (x.price, LadderRow(sizeToBack = x.size)))
    val tradedVolume = exchangePrices.tradedVolume.map(x => (x.price, LadderRow(tradedVolume = x.size)))

    val ordersToBack = orders.filter(x => x.side == Side.BACK && x.sizeRemaining > 0).toList.map(x => (x.price, LadderRow(ordersToBack = x.sizeRemaining)))
    val ordersToLay = orders.filter(x => x.side == Side.LAY && x.sizeRemaining > 0).toList.map(x => (x.price, LadderRow(ordersToLay = x.sizeRemaining)))

    List(sizeToLay, sizeToBack, tradedVolume, ordersToBack, ordersToLay).flatten.groupBy{case(price, ladderRow) => price}
      .mapValues[LadderRow](_.foldLeft[LadderRow](LadderRow())((acc: LadderRow, x) => x match {
      case(price, row) => acc.copy(
        ordersToLay = acc.ordersToLay + row.ordersToLay,
        sizeToLay = acc.sizeToLay + row.sizeToLay,
        sizeToBack = acc.sizeToBack + row.sizeToBack,
        ordersToBack = acc.ordersToBack + row.ordersToBack,
        tradedVolume = acc.tradedVolume + row.tradedVolume
      )}
    ))
  }
}


