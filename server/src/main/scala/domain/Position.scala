package domain

import play.api.libs.json.{JsNumber, Writes, Json}

case class Position(sumBacked: Double, sumLaid: Double, backReturn: Double, layLiability: Double) {
  val avgPriceBacked: Double = if (sumBacked == 0.0) 0 else BigDecimal(backReturn / sumBacked).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  val avgPriceLaid: Double = if (sumLaid == 0.0) 0 else BigDecimal(layLiability / sumLaid).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  val winPnL: Double = BigDecimal(backReturn - layLiability).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  val losePnL: Double = BigDecimal(sumLaid - sumBacked).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
}

object Position {
  implicit val readsPosition = Json.reads[Position]
  implicit val writesPosition: Writes[Position] = new Writes[Position] {
    def writes(p: Position) = Json.obj(
      "sumBacked" -> JsNumber(p.sumBacked),
      "sumLaid" -> JsNumber(p.sumLaid),
      "backReturn" -> JsNumber(p.backReturn),
      "layLiability" -> JsNumber(p.layLiability),
      "avgPriceBacked" -> JsNumber(p.avgPriceBacked),
      "avgPriceLaid" -> JsNumber(p.avgPriceLaid),
      "winPnL" -> JsNumber(p.winPnL),
      "losePnL" -> JsNumber(p.losePnL)
    )
  }
}
