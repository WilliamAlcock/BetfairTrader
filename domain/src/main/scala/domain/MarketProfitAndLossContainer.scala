/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class MarketProfitAndLossContainer(result: List[MarketProfitAndLoss])

object MarketProfitAndLossContainer {
  implicit val formatMarketProfitAndLossContainer = Json.format[MarketProfitAndLossContainer]
}