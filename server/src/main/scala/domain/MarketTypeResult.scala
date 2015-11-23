/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class MarketTypeResult(marketType: String, marketCount: Int)

object MarketTypeResult {
  implicit val formatMarketTypeResult = Json.format[MarketTypeResult]
}