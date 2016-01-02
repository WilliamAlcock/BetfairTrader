package services

import domain.MarketBook
import play.api.libs.json._

case class UIMarketBook(marketBook: MarketBook) {
  val runners: Map[String, UIRunner] = (marketBook.runners.map(_.uniqueId) zip marketBook.runners.map(UIRunner(_))).toMap
}

object UIMarketBook {
  implicit val writeUIMarketBook: Writes[UIMarketBook] = new Writes[UIMarketBook] {
    def writes(r: UIMarketBook) = Json.obj(
      "marketId" -> JsString(r.marketBook.marketId),
      "isMarketDataDelayed" -> JsBoolean(r.marketBook.isMarketDataDelayed),
      "status" -> JsString(r.marketBook.status),
      "betDelay" -> JsNumber(r.marketBook.betDelay),
      "bspReconciled" -> JsBoolean(r.marketBook.bspReconciled),
      "complete" -> JsBoolean(r.marketBook.complete),
      "inplay" -> JsBoolean(r.marketBook.inplay),
      "numberOfWinners" -> JsNumber(r.marketBook.numberOfWinners),
      "numberOfRunners" -> JsNumber(r.marketBook.numberOfRunners),
      "numberOfActiveRunners" -> JsNumber(r.marketBook.numberOfActiveRunners),
      "lastMatchTime" -> Json.toJson(r.marketBook.lastMatchTime),
      "totalMatched" -> JsNumber(r.marketBook.totalMatched),
      "totalAvailable" -> JsNumber(r.marketBook.totalAvailable),
      "crossMatching" -> JsBoolean(r.marketBook.crossMatching),
      "runnersVoidable" -> JsBoolean(r.marketBook.runnersVoidable),
      "version" -> JsNumber(r.marketBook.version),
      "overround" -> Json.toJson(r.marketBook.overround),
      "hedge" -> JsNumber(r.marketBook.hedge),
      "runners" -> Json.toJson(r.runners)
    )
  }
}