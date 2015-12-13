package domain

import play.api.libs.json.Json

case class ExchangePrices(availableToBack: List[PriceSize],
                          availableToLay: List[PriceSize],
                          tradedVolume: List[PriceSize])

object ExchangePrices {
  implicit val formatExchangePrices = Json.format[ExchangePrices]
}