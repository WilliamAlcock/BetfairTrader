package domain

import play.api.libs.json.Json

case class ListMarketBookContainer(result: List[MarketBook])

object ListMarketBookContainer {

  implicit val formatListMarketBookContainer = Json.format[ListMarketBookContainer]
}