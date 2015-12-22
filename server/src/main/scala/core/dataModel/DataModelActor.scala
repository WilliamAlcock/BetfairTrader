package core.dataModel

import akka.actor.{Props, Actor}
import core.api.commands.GetNavigationData
import core.api.output._
import core.dataProvider.output._
import core.eventBus.{EventBus, MessageEvent}
import domain._
import server.Configuration

import scala.collection.immutable.HashMap

class DataModelActor(config: Configuration, eventBus: EventBus, var dataModel: DataModel) extends Actor {

  // TODO get these from config
  private val OUTPUT_CHANNEL = "modelUpdates"

  // TODO this code is duplicated move it into central utils lib
  def getPublishChannel(ids: Seq[String]): String = ids.foldLeft[String](OUTPUT_CHANNEL)((channel, id) => channel + "/" + id)

  def receive = {
    case GetNavigationData =>
      eventBus.publish(MessageEvent(getPublishChannel(Seq("navData")), NavigationDataUpdate(dataModel.navData, dataModel.competitions)))
    case eventTypeData: EventTypeDataUpdate =>
      this.dataModel = eventTypeData.listEventTypeResultContainer.result.foldLeft[DataModel](this.dataModel)(
        (dataModel: DataModel, eventTypeResult: EventTypeResult) => {
          // publish change to broadcast channel
          eventBus.publish(MessageEvent(
            getPublishChannel(Seq("eventTypeResult", eventTypeResult.eventType.id)),
            EventTypeUpdate(eventTypeResult)
          ))
          // update data model
          dataModel.updateEventTypeResult(eventTypeResult)
        }
      )
    case eventData: EventDataUpdate =>
      this.dataModel = eventData.listEventResultContainer.result.foldLeft[DataModel](this.dataModel)(
        (dataModel: DataModel, eventResult: EventResult) => {
          // publish change to broadcast channel
          eventBus.publish(MessageEvent(
            getPublishChannel(Seq("eventResult", eventResult.event.id)),
            EventUpdate(eventResult)
          ))
          // update data model
          dataModel.updateEventResult(eventResult)
        }
      )
    case marketDataUpdate: MarketDataUpdate =>
      this.dataModel = marketDataUpdate.listMarketBookContainer.result.foldLeft[DataModel](this.dataModel)(
        (dataModel: DataModel, marketBook: MarketBook) => {
          // publish change to broadcast channel
          eventBus.publish(MessageEvent(
            getPublishChannel(Seq("marketBook", marketBook.marketId)),
            // sort the runners here
            MarketBookUpdate(
              marketBook,
              marketBook.runners.map(x =>
                (x.selectionId.toString + "-" + x.handicap.toString) -> x)(collection.breakOut): HashMap[String, Runner]
            )
          ))
          // update data model
          dataModel.updateMarketBook(marketBook)
        }
      )
    case marketCatalogueData: MarketCatalogueDataUpdate =>
      this.dataModel = marketCatalogueData.listMarketCatalogueContainer.result.foldLeft[DataModel](this.dataModel)(
        (dataModel: DataModel, marketCatalogue: MarketCatalogue) => {
          // publish change to broadcast channel
          eventBus.publish(MessageEvent(
            getPublishChannel(Seq("marketBook", marketCatalogue.marketId)),
            MarketCatalogueUpdate(marketCatalogue)
          ))
          // update data model
          dataModel.updateMarketCatalogue(marketCatalogue)
        }
      )
  }
}

object DataModelActor {
  def props(config: Configuration, eventBus: EventBus, dataModel: DataModel) = Props(new DataModelActor(config, eventBus, dataModel))
}