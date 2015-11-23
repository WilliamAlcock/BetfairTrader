package core.dataModel

import domain.{MarketCatalogue, MarketBook, EventResult, EventTypeResult}

import scala.collection.immutable.HashMap

case class EventType(data: List[EventTypeResult], events: HashMap[String, Event] = HashMap[String, Event]()) {
  def getEvent(eventId: String): Event = {
    this.events.get(eventId) match {
      case Some(event: Event) => event
      case None => throw new DataModelException("event " + eventId + "does not exist in model")
    }
  }

  def getMarket(eventId: String, marketId: String): Market = {
    this.events.get(eventId) match {
      case Some(event: Event) => event.getMarket(marketId)
      case None => throw new DataModelException("event " + eventId + "does not exist in model")
    }
  }

  def update(eventTypeResult: EventTypeResult): EventType = {
    this.copy(data = eventTypeResult :: data)
  }

  def updateEvent(eventResult: EventResult): EventType = {
    val eventId = eventResult.event.id
    this.events.get(eventId) match {
      case Some(event: Event) => this.copy(events = events + (eventId -> event.update(eventResult)))
      case None => this.copy(events = events + (eventId -> Event(List(eventResult))))
    }
  }

  def updateMarket(eventId: String, marketBook: MarketBook): EventType = {
    this.events.get(eventId) match {
      case Some(event: Event) => this.copy(events = events + (eventId -> event.updateMarket(marketBook)))
      case None => throw new DataModelException(
        "cannot update market: " + marketBook.marketId + " as event: " + eventId + " does not exist")
    }
  }

  def updateMarketCatalogue(marketCatalogue: MarketCatalogue): EventType = {
    val eventId = marketCatalogue.event.id
    this.events.get(eventId) match {
      case Some(event: Event) =>
        this.copy(events = events + (eventId -> event.updateMarketCatalogue(marketCatalogue)))
      case None => throw new DataModelException(
        "cannot update marketCatalogue for market: " + marketCatalogue.marketId + " as event: " + eventId + " does not exist")
    }
  }
}