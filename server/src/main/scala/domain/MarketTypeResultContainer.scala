/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class MarketTypeResultContainer(result: List[MarketTypeResult])

object MarketTypeResultContainer {
  implicit val formatMarketTypeResultContainer = Json.format[MarketTypeResultContainer]
}