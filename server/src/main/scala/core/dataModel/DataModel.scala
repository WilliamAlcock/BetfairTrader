package core.dataModel

import core.dataModel.navData.NavData
import domain._

import scala.collection.immutable.HashMap

case class DataModel(eventTypeResults: HashMap[String, EventTypeResult] = HashMap.empty,
                     eventResults: HashMap[String, EventResult] = HashMap.empty,
                     marketBooks: HashMap[String, MarketBook] = HashMap.empty,
                     marketCatalogues: HashMap[String, MarketCatalogue] = HashMap.empty,
                     navData: NavData) {

  def getEventType(eventTypeId: String): Option[EventTypeResult] = eventTypeResults.get(eventTypeId)

  def getEvent(eventId: String): Option[EventResult] = eventResults.get(eventId)

  def getMarketBook(marketId: String): Option[MarketBook] = marketBooks.get(marketId)

  def getMarketCatalogue(marketId: String): Option[MarketCatalogue] = marketCatalogues.get(marketId)

  def updateEventTypeResult(eventTypeResult: EventTypeResult): DataModel =
    this.copy(eventTypeResults = eventTypeResults + (eventTypeResult.eventType.id -> eventTypeResult))

  def updateEventResult(eventResult: EventResult): DataModel =
    this.copy(eventResults = eventResults + (eventResult.event.id -> eventResult))

  def updateMarketBook(marketBook: MarketBook): DataModel =
    this.copy(marketBooks = marketBooks + (marketBook.marketId -> marketBook))

  def updateMarketCatalogue(marketCatalogue: MarketCatalogue): DataModel =
    this.copy(marketCatalogues = marketCatalogues + (marketCatalogue.marketId -> marketCatalogue))
}