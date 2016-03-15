package domain

import org.joda.time.DateTime
import play.api.libs.json._

case class MarketBook(marketId: String,
                      isMarketDataDelayed: Boolean,
                      status: String,
                      betDelay: Int,
                      bspReconciled: Boolean,
                      complete: Boolean,
                      inplay: Boolean,
                      numberOfWinners: Int,
                      numberOfRunners: Int,
                      numberOfActiveRunners: Int,
                      lastMatchTime: Option[DateTime],
                      totalMatched: Double,
                      totalAvailable: Double,
                      crossMatching: Boolean,
                      runnersVoidable: Boolean,
                      version: Long,
                      runners: Set[Runner]) {
  lazy val overround: Option[Double] = MarketBook.getOverround(runners)
  lazy val hedge: Double = MarketBook.getHedge(runners)

  def getOrder(selectionId: Long, betId: String): Option[Order] = getRunner(selectionId).orders match {
    case Some(x) => x.find(_.betId == betId)
    case _ => None
  }

  def getRunner(selectionId: Long): Runner = runners.find(_.selectionId == selectionId).get
}

object MarketBook {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)
  implicit val readsMarketBook = Json.reads[MarketBook]
  implicit val writesMarketBook = new Writes[MarketBook] {
    def writes(m: MarketBook) = Json.obj(
      "marketId" -> JsString(m.marketId),
      "isMarketDataDelayed" -> JsBoolean(m.isMarketDataDelayed),
      "status" -> JsString(m.status),
      "betDelay" -> JsNumber(m.betDelay),
      "bspReconciled" -> JsBoolean(m.bspReconciled),
      "complete" -> JsBoolean(m.complete),
      "inplay" -> JsBoolean(m.inplay),
      "numberOfWinners" -> JsNumber(m.numberOfWinners),
      "numberOfRunners" -> JsNumber(m.numberOfRunners),
      "numberOfActiveRunners" -> JsNumber(m.numberOfActiveRunners),
      "lastMatchTime" -> Json.toJson(m.lastMatchTime),
      "totalMatched" -> JsNumber(m.totalMatched),
      "totalAvailable" -> JsNumber(m.totalAvailable),
      "crossMatching" -> JsBoolean(m.crossMatching),
      "runnersVoidable" -> JsBoolean(m.runnersVoidable),
      "version" -> JsNumber(m.version),
      "runners" -> Json.toJson(m.runners),
      "overround" -> Json.toJson(m.overround),
      "hedge" -> JsNumber(m.hedge)
    )
  }

  implicit val formatMarketBook = Json.format[MarketBook]

  def getHedge(runners: Set[Runner]): Double =
    BigDecimal(runners.foldLeft(0.0)((acc, x) => acc + x.hedge)).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

  def getOverround(runners: Set[Runner]): Option[Double] = runners.foldLeft[Option[Double]](Some(0.0))((acc, x) =>
    if (acc.isDefined && x.backPrice.isDefined && x.backPrice.get != 0) Some(acc.get + (100 / x.backPrice.get)) else None
  ) match {
    case Some(x) => Some(BigDecimal(x - 100).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
    case None => None
  }
}