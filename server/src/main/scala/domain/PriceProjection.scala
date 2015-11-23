package domain

import domain.PriceData.PriceData
import play.api.libs.json.Json

case class PriceProjection(priceData: Set[PriceData])

object PriceProjection {
  implicit val formatPriceProjection = Json.format[PriceProjection]
}