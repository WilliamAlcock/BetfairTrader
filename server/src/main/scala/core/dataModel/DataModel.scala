package core.dataModel

import core.dataModel.indicators.TickData
import core.dataModel.navData.NavData
import domain._

import scala.collection.immutable.HashMap


case class DataModel(eventTypeResults: HashMap[String, EventTypeResult] = HashMap.empty,
                     eventResults: HashMap[String, EventResult] = HashMap.empty,
                     markets: HashMap[String, MarketData] = HashMap.empty,
                     navData: NavData)

object DataModel {
  // Getters

  def getEventType(dataModel: DataModel, eventTypeId: String): Option[EventTypeResult] = dataModel.eventTypeResults.get(eventTypeId)

  def getEvent(dataModel: DataModel, eventId: String): Option[EventResult] = dataModel.eventResults.get(eventId)

  def getMarketBook(dataModel: DataModel, marketId: String): Option[MarketBook] = dataModel.markets.get(marketId) match {
    case Some(x) => x.book
    case _ => None
  }

  def getMarketCatalogue(dataModel: DataModel, marketId: String): Option[MarketCatalogue] = dataModel.markets.get(marketId) match {
    case Some(x) => x.catalogue
    case _ => None
  }

  def getAllMarketTickData(dataModel: DataModel, marketId: String): HashMap[String, List[TickData]] = dataModel.markets.get(marketId) match {
    case Some(x) => x.tickData
    case _ => HashMap.empty
  }

  def getAllRunnerTickData(dataModel: DataModel, marketId: String, uniqueId: String): List[TickData] = dataModel.markets.get(marketId) match {
    case Some(x) => x.tickData.getOrElse(uniqueId, List.empty[TickData])
    case _ => List.empty
  }

  def getMarketTickData(dataModel: DataModel, marketId: String): HashMap[String, List[TickData]] = dataModel.markets.get(marketId) match {
    case Some(x) => x.tickData.map{case (k,v) => k -> v.take(1)}
    case _ => HashMap.empty
  }

  def getRunnerTickData(dataModel: DataModel, marketId: String, uniqueId: String): List[TickData] = dataModel.markets.get(marketId) match {
    case Some(x) => x.tickData.getOrElse(uniqueId, List.empty[TickData]).take(1)
    case _ => List.empty
  }

  // Setters

  def updateEventTypeResult(dataModel: DataModel, eventTypeResult: EventTypeResult): DataModel = dataModel.copy(
    eventTypeResults = dataModel.eventTypeResults + (eventTypeResult.eventType.id -> eventTypeResult)
  )

  def updateEventResult(dataModel: DataModel, eventResult: EventResult): DataModel = dataModel.copy(
    eventResults = dataModel.eventResults + (eventResult.event.id -> eventResult)
  )

  def updateMarketBook(dataModel: DataModel, marketBook: MarketBook): DataModel = dataModel.copy(
    markets = dataModel.markets + (marketBook.marketId ->
      MarketData.updateMarketBook(dataModel.markets.getOrElse(marketBook.marketId, MarketData()), marketBook))
  )

  def updateMarketCatalogue(dataModel: DataModel, marketCatalogue: MarketCatalogue): DataModel = dataModel.copy(
    markets = dataModel.markets + (marketCatalogue.marketId ->
      MarketData.updateMarketCatalogue(dataModel.markets.getOrElse(marketCatalogue.marketId, MarketData()), marketCatalogue))
  )
}