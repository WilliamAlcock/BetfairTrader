/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class MarketProfitAndLoss(marketId: String,
                               commissionApplied: Double,
                               profitAndLoss: Set[RunnerProfitAndLoss])

object MarketProfitAndLoss {
  implicit val formatMarketProfitAndLoss = Json.format[MarketProfitAndLoss]
}