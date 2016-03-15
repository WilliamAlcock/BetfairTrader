package domain

import domain.PriceData.PriceData
import play.api.libs.json.Json

case class PriceProjection(priceData: Set[PriceData],
                           exBestOffersOverrides: Option[ExBestOffersOverrides] = None,
                           virtualise: Option[Boolean] = None)

object PriceProjection {
  implicit val formatPriceProjection = Json.format[PriceProjection]
}