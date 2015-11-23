/**
 * Created by Alcock on 08/11/2015.
 */

package core.dataModel

import domain.{MarketCatalogue, MarketBook}

case class Market(data: List[MarketBook] = List[MarketBook](), catalogue: List[MarketCatalogue] = List[MarketCatalogue]()) {
  def update(marketBook: MarketBook): Market = {
    this.copy(data = marketBook :: this.data)
  }

  def updateCatalogue(marketCatalogue: MarketCatalogue): Market = {
    this.copy(catalogue = marketCatalogue :: this.catalogue)
  }
}