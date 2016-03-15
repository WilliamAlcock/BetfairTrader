package core.dataModel

import domain._

import scala.collection.immutable.HashMap

sealed case class MarketData(book: Option[MarketBook] = None, catalogue: Option[MarketCatalogue] = None)

case class DataModel(markets: HashMap[String, MarketData] = HashMap.empty) {

  def getMarketBook(marketId: String): Option[MarketBook] = this.markets.get(marketId) match {
    case Some(x) => x.book
    case _ => None
  }

  def getMarketCatalogue(marketId: String): Option[MarketCatalogue] = this.markets.get(marketId) match {
    case Some(x) => x.catalogue
    case _ => None
  }

  def setMarketBook(marketBook: MarketBook): DataModel = this.copy(
    markets = this.markets + (marketBook.marketId -> this.markets.getOrElse(marketBook.marketId, MarketData()).copy(book = Some(marketBook)))
  )

  def setMarketCatalogue(marketCatalogue: MarketCatalogue): DataModel = this.copy(
    markets = this.markets + (marketCatalogue.marketId -> this.markets.getOrElse(marketCatalogue.marketId, MarketData()).copy(catalogue = Some(marketCatalogue)))
  )
}