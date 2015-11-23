package core.dataModel

import akka.actor.Actor
import core.api.{MarketCatalogueUpdate, MarketBookUpdate, EventUpdate, EventTypeUpdate}
import core.dataProvider.output._
import core.eventBus.{MessageEvent, EventBus}
import domain.{MarketCatalogue, MarketBook, EventResult, EventTypeResult}

/**
 * Created by Alcock on 08/11/2015.
 */

class DataModelActor(eventBus: EventBus) extends Actor {

  // TODO get these from config
  private val OUTPUT_CHANNEL = "modelUpdates"

  var dataModel: DataModel = DataModel()

  // TODO this code is duplicated move it into central utils lib
  def getPublishChannel(ids: Seq[String]): String = ids.foldLeft[String](OUTPUT_CHANNEL)((channel, id) => channel + "/" + id)

  def receive = {
    case eventTypeData: EventTypeData =>
      this.dataModel = eventTypeData.listEventTypeResultContainer.result.foldLeft[DataModel](this.dataModel)(
        (dataModel: DataModel, eventTypeResult: EventTypeResult) => {
          // get broadcast channel
          val channel = getPublishChannel(Seq(eventTypeResult.eventType.id))
          // update data model
          val newDataModel = dataModel.updateEventType(eventTypeResult)
          // publish change to broadcast channel
          val newEventType = newDataModel.getEventType(eventTypeResult.eventType.id)
          eventBus.publish(MessageEvent(channel, EventTypeUpdate(newEventType)))
          newDataModel
        }
      )
    case eventData: EventData =>
      this.dataModel = eventData.listEventResultContainer.result.foldLeft[DataModel](this.dataModel)(
        (dataModel: DataModel, eventResult: EventResult) => {
          // get broadcast channel
          val channel = getPublishChannel(Seq(eventData.eventTypeId, eventResult.event.id))
          // update data model
          val newDataModel = dataModel.updateEvent(eventData.eventTypeId, eventResult)
          // publish change to broadcast channel
          val newEvent = newDataModel.getEvent(eventData.eventTypeId, eventResult.event.id)
          eventBus.publish(MessageEvent(channel, EventUpdate(newEvent)))
          newDataModel
        }
      )
    case marketData: MarketData =>
      this.dataModel = marketData.listMarketBookContainer.result.foldLeft[DataModel](this.dataModel)(
        (dataModel: DataModel, marketBook: MarketBook) => {
          // get broadcast channel
          val channel = getPublishChannel(Seq(marketData.eventTypeId, marketData.eventId, marketBook.marketId))
          // update data model
          val newDataModel = dataModel.updateMarket(marketData.eventTypeId, marketData.eventId, marketBook)
          // publish change to broadcast channel
          val newMarket = newDataModel.getMarket(marketData.eventTypeId, marketData.eventId, marketBook.marketId)
          eventBus.publish(MessageEvent(channel, MarketBookUpdate(newMarket)))
          newDataModel
        }
      )
    // TODO think about how to differ between marketCatalogue update and marketUpdate
    case marketCatalogueUpdate: MarketCatalogueData =>
      this.dataModel = marketCatalogueUpdate.listMarketCatalogueContainer.result.foldLeft[DataModel](this.dataModel)(
        (dataModel: DataModel, marketCatalogue: MarketCatalogue) => {
          // get broadcast channel
          val channel = getPublishChannel(Seq(marketCatalogueUpdate.eventTypeId, marketCatalogue.event.id, marketCatalogue.marketId))
          // update data model
          val newDataModel = dataModel.updateMarketCatalogue(marketCatalogueUpdate.eventTypeId, marketCatalogue)
          // publish change to broadcast channel
          val newMarket = newDataModel.getMarket(marketCatalogueUpdate.eventTypeId, marketCatalogue.event.id, marketCatalogue.marketId)
          eventBus.publish(MessageEvent(channel, MarketCatalogueUpdate(newMarket)))
          newDataModel
        }
      )
  }
}