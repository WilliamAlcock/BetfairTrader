/**
 * Created by Alcock on 08/11/2015.
 */

package core.dataModel

import domain.{MarketCatalogue, MarketBook, EventResult, EventTypeResult}

import scala.collection.immutable.HashMap

case class DataModel(eventTypes: HashMap[String, EventType] = HashMap[String, EventType]()) {

  def getEventType(eventTypeId: String): EventType = {
    this.eventTypes.get(eventTypeId) match {
      case Some(eventType: EventType) => eventType
      case None => throw new DataModelException("eventType " + eventTypeId + "does not exist in model")
    }
  }

  def getEvent(eventTypeId: String, eventId: String): Event = {
    this.eventTypes.get(eventTypeId) match {
      case Some(eventType: EventType) => eventType.getEvent(eventId)
      case None => throw new DataModelException("eventType " + eventTypeId + "does not exist in model")
    }
  }

  def getMarket(eventTypeId: String, eventId: String, marketId: String): Market = {
    this.eventTypes.get(eventTypeId) match {
      case Some(eventType: EventType) => eventType.getMarket(eventId, marketId)
      case None => throw new DataModelException("eventType " + eventTypeId + "does not exist in model")
    }
  }

  def updateEventType(eventTypeResult: EventTypeResult): DataModel = {
    val eventTypeId = eventTypeResult.eventType.id
    this.eventTypes.get(eventTypeId) match {
      case Some(eventType: EventType) =>
        this.copy(eventTypes = eventTypes + (eventTypeId -> eventType.update(eventTypeResult)))
      case None =>
        this.copy(eventTypes = eventTypes + (eventTypeId -> EventType(List(eventTypeResult))))
    }
  }

  def updateEvent(eventTypeId: String, eventResult: EventResult): DataModel = {
    this.eventTypes.get(eventTypeId) match {
      case Some(eventType: EventType) =>
        this.copy(eventTypes = eventTypes + (eventTypeId -> eventType.updateEvent(eventResult)))
      case None => throw new DataModelException(
        "cannot update event: " + eventResult.event.id + " as eventType: " + eventTypeId + " does not exist")
    }
  }

  def updateMarket(eventTypeId: String, eventId: String, marketBook: MarketBook): DataModel = {
    this.eventTypes.get(eventTypeId) match {
      case Some(eventType: EventType) =>
        this.copy(eventTypes = eventTypes + (eventTypeId -> eventType.updateMarket(eventId, marketBook)))
      case None => throw new DataModelException(
        "cannot update market: " + marketBook.marketId + " as eventType: " + eventTypeId + " does not exist")
    }
  }

  def updateMarketCatalogue(eventTypeId: String, marketCatalogue: MarketCatalogue): DataModel = {
    this.eventTypes.get(eventTypeId) match {
      case Some(eventType: EventType) =>
        this.copy(eventTypes = eventTypes + (eventTypeId -> eventType.updateMarketCatalogue(marketCatalogue)))
      case None => throw new DataModelException(
        "cannot update marketCatalogue for market: " + marketCatalogue.marketId + " as eventType: " + eventTypeId + " does not exist")
    }
  }
}