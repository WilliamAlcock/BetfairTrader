package core.dataModel

import akka.actor.{Actor, Props}
import core.api.commands.GetNavigationData
import core.api.output._
import core.eventBus.{EventBus, MessageEvent}
import server.Configuration

class DataModelActor(config: Configuration, eventBus: EventBus, var dataModel: DataModel) extends Actor {

  private def updateMarketBook(marketBookUpdate: MarketBookUpdate) = DataModel.getMarketBook(dataModel, marketBookUpdate.data.marketId) match {
    case Some(x) if marketBookUpdate.data == x =>
      eventBus.publish(MessageEvent(
        config.getPublishChannel(Seq(marketBookUpdate.data.marketId)),
        marketBookUpdate
      ))
      DataModel.updateMarketBook(dataModel, marketBookUpdate.data)
    case _ => dataModel
  }

  def receive = {
    case GetNavigationData(subscriber)  => subscriber ! dataModel.navData
    case x: MarketBookUpdate            => this.dataModel = updateMarketBook(x)
  }
}

object DataModelActor {
  def props(config: Configuration, eventBus: EventBus, dataModel: DataModel) = Props(new DataModelActor(config, eventBus, dataModel))
}