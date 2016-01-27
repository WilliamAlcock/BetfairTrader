package core.dataModel

import akka.actor.{Actor, Props}
import core.api.commands.GetNavigationData
import core.api.output._
import core.dataProvider.output._
import core.eventBus.{EventBus, MessageEvent}
import domain._
import server.Configuration

class DataModelActor(config: Configuration, eventBus: EventBus, var dataModel: DataModel) extends Actor {

  // TODO get these from config
  private val OUTPUT_CHANNEL = "modelUpdates"

  // TODO this code is duplicated move it into central utils lib
  private def getPublishChannel(ids: Seq[String]): String = ids.foldLeft[String](OUTPUT_CHANNEL)((channel, id) => channel + "/" + id)

  private def updateEventTypeResult(dataModel: DataModel, eventTypeResult: EventTypeResult): DataModel = {
    // update data model
    val newDataModel = DataModel.updateEventTypeResult(dataModel, eventTypeResult)
    // publish change to broadcast channel
    eventBus.publish(MessageEvent(
      getPublishChannel(Seq("eventTypeResult", eventTypeResult.eventType.id)),
      EventTypeUpdate(eventTypeResult)
    ))
    newDataModel
  }

  private def updateEventResult(dataModel: DataModel, eventResult: EventResult): DataModel = {
    // update data model
    val newDataModel = DataModel.updateEventResult(dataModel, eventResult)
    // publish change to broadcast channel
    eventBus.publish(MessageEvent(
      getPublishChannel(Seq("eventResult", eventResult.event.id)),
      EventUpdate(eventResult)
    ))
    newDataModel
  }

  private def updateMarketBook(dataModel: DataModel, marketBook: MarketBook): DataModel = {
    // update data model
    val newDataModel = DataModel.updateMarketBook(dataModel, marketBook)
    // publish change to broadcast channel
    eventBus.publish(MessageEvent(
      getPublishChannel(Seq("marketBook", marketBook.marketId)),
      MarketBookUpdate(marketBook, DataModel.getMarketTickData(newDataModel, marketBook.marketId))
    ))
    newDataModel
  }

  private def updateMarketCatalogue(dataModel: DataModel, marketCatalogue: MarketCatalogue): DataModel = {
    // update data model
    val newDataModel = DataModel.updateMarketCatalogue(dataModel, marketCatalogue)
    // publish change to broadcast channel
    eventBus.publish(MessageEvent(
      getPublishChannel(Seq("marketBook", marketCatalogue.marketId)),
      MarketCatalogueUpdate(marketCatalogue)
    ))
    newDataModel
  }

  def receive = {
    case GetNavigationData =>
      eventBus.publish(MessageEvent(getPublishChannel(Seq("navData")), NavigationDataUpdate(dataModel.navData)))
    case eventTypeData: EventTypeDataUpdate =>
      this.dataModel = eventTypeData.listEventTypeResultContainer.result.foldLeft(this.dataModel)(updateEventTypeResult)
    case eventData: EventDataUpdate =>
      this.dataModel = eventData.listEventResultContainer.result.foldLeft(this.dataModel)(updateEventResult)
    case marketDataUpdate: MarketDataUpdate =>
      this.dataModel = marketDataUpdate.listMarketBookContainer.result.foldLeft(this.dataModel)(updateMarketBook)
    case marketCatalogueData: MarketCatalogueDataUpdate =>
      this.dataModel = marketCatalogueData.listMarketCatalogueContainer.result.foldLeft(this.dataModel)(updateMarketCatalogue)
  }
}

object DataModelActor {
  def props(config: Configuration, eventBus: EventBus, dataModel: DataModel) = Props(new DataModelActor(config, eventBus, dataModel))
}