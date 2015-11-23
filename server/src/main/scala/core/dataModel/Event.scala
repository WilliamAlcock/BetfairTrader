/**
 * Created by Alcock on 08/11/2015.
 */

package core.dataModel

import domain.{MarketCatalogue, MarketBook, EventResult}

import scala.collection.immutable.HashMap

case class Event(data: List[EventResult], markets: HashMap[String, Market] = HashMap[String, Market]()) {

  def getMarket(marketId: String): Market = {
    this.markets.get(marketId) match {
      case Some(market: Market) => market
      case None => throw new DataModelException("market " + marketId + "does not exist in model")
    }
  }

  def update(eventResult: EventResult): Event = {
    this.copy(data = eventResult :: data)
  }

  def updateMarket(marketBook: MarketBook): Event = {
    val marketId = marketBook.marketId
    this.markets.get(marketId) match {
      case Some(market: Market) =>
        this.copy(markets = this.markets + (marketId -> market.update(marketBook)))
      case None =>
        this.copy(markets = this.markets + (marketId -> Market(data = List(marketBook))))
    }
  }

  def updateMarketCatalogue(marketCatalogue: MarketCatalogue): Event = {
    val marketId = marketCatalogue.marketId
    this.markets.get(marketId) match {
      case Some(market: Market) =>
        this.copy(markets = markets + (marketId -> market.updateCatalogue(marketCatalogue)))
      case None =>
        this.copy(markets = this.markets + (marketId -> Market(catalogue = List(marketCatalogue))))
    }
  }
}