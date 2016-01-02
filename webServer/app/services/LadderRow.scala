package services

import play.api.libs.json._

case class LadderRow(ordersToLay: Double = 0, sizeToLay: Double = 0, sizeToBack: Double = 0, ordersToBack: Double = 0, tradedVolume: Double = 0)

object LadderRow {
  implicit val formatLadderRow = Json.format[LadderRow]
}

