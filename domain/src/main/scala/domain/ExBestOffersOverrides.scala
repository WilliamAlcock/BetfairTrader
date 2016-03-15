package domain

import domain.RollupModel.RollupModel
import play.api.libs.json.Json

case class ExBestOffersOverrides(bestPricesDepth: Int, rollupModel: RollupModel, rollupLimit: Int)

object ExBestOffersOverrides {
  implicit val formatExBestOffersOverrides = Json.format[ExBestOffersOverrides]
}

