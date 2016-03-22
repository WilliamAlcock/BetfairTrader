package core.dataModel

import akka.actor.{Actor, Props}
import core.api.commands.ListMarketBook
import core.eventBus.{EventBus, MessageEvent}
import domain.MarketBookUpdate
import org.joda.time.DateTime
import server.Configuration

class DataModelActor(config: Configuration, eventBus: EventBus, var dataModel: DataModel) extends Actor {

  private def setMarketBook(marketBookUpdate: MarketBookUpdate): DataModel = dataModel.getMarketBook(marketBookUpdate.data.marketId) match {
    case Some(x) if marketBookUpdate.data == x => dataModel               // If we already have this copy ignore it
    case _ =>                                                             // Otherwise broadcast it and update our copy
      eventBus.publish(MessageEvent(
        config.getMarketUpdateChannel(Seq(marketBookUpdate.data.marketId)),
        marketBookUpdate,
        self
      ))
      dataModel.setMarketBook(marketBookUpdate.data)
  }

  def receive = {
    case ListMarketBook(marketIds) =>                                     // If we have them send the requester the marketBook for the given marketIds
      marketIds.map(x => dataModel.getMarketBook(x)).foreach{
        case Some(marketBook) =>
          println("SENDING MARKET BOOK UPDATE TO ", sender(), marketBook)
          sender ! MarketBookUpdate(DateTime.now(), marketBook)
        case None =>
      }
    case x: MarketBookUpdate    => this.dataModel = setMarketBook(x)
  }
}

object DataModelActor {
  def props(config: Configuration, eventBus: EventBus, dataModel: DataModel = DataModel()) = Props(new DataModelActor(config, eventBus, dataModel))
}