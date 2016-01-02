package domain

import play.api.libs.json.{JsNumber, Writes, Json}

case class WeightOfMoney(backVolume: Double, layVolume: Double) {
  lazy val backPer = (backVolume / (backVolume + layVolume)) * 100
  lazy val layPer = (layVolume / (backVolume + layVolume)) * 100
}

object WeightOfMoney {
  implicit val readsWeightOfMoney = Json.reads[WeightOfMoney]
  implicit val writesWeightOfMoney = new Writes[WeightOfMoney] {
    def writes(w: WeightOfMoney) = Json.obj(
      "backVolume" -> JsNumber(w.backVolume),
      "backPer" -> JsNumber(w.backPer),
      "layVolume" -> JsNumber(w.layVolume),
      "layPer" -> JsNumber(w.layPer)
    )
  }
}
