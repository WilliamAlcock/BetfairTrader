package core

import core.api._
import akka.actor.Actor
import core.eventBus.{MessageEvent, EventBus}
import server.Configuration

class Controller(config: Configuration, eventBus: EventBus) extends Actor {

  import context._

  // TODO get these from config
  private val ORDER_MANAGER_CHANNEL = "orderManagerInstructions"
  private val DATA_PROVIDER_CHANNEL = "dataProviderInstructions"
  private val MODEL_UPDATE_CHANNEL = "modelUpdates"

  // TODO this line of code is duplicated move it into central lib
  private def getPublishChannel(ids: Seq[String]): String = ids.foldLeft[String](MODEL_UPDATE_CHANNEL)((channel, id) => channel + "/" + id)

  private def getCommandChannel(x: Command): String = x match {
    case x: ListEventTypes        => DATA_PROVIDER_CHANNEL
    case x: ListEvents            => DATA_PROVIDER_CHANNEL
    case x: ListMarketCatalogue   => DATA_PROVIDER_CHANNEL
    case x: StartPollingMarket    => DATA_PROVIDER_CHANNEL
    case x: StopPollingMarket     => DATA_PROVIDER_CHANNEL
    case x: StopPollingAllMarkets => DATA_PROVIDER_CHANNEL
    case x: PlaceOrders           => ORDER_MANAGER_CHANNEL
    case x: CancelOrders          => ORDER_MANAGER_CHANNEL
    case x: ReplaceOrders         => ORDER_MANAGER_CHANNEL
    case x: UpdateOrders          => ORDER_MANAGER_CHANNEL
  }

  system.log.info(self.toString)

  def receive = {
    case x: String =>
      system.log.info("I have your message: " + x)
    case Subscribe(subscriber, subChannels) =>
      eventBus.subscribe(subscriber, getPublishChannel(subChannels))
      system.log.info("Subscribing " + subscriber + " to " + subChannels)
      sender ! "Subscribing"
    case Unsubscribe(subscriber, subChannels) =>
      eventBus.unsubscribe(subscriber, getPublishChannel(subChannels))
      system.log.info("Unsubscribing " + subscriber + " to " + subChannels)
      sender ! "Unsubscribing"
    case x: Command =>
      eventBus.publish(MessageEvent(getCommandChannel(x), x))
      system.log.info("actioning command: " + x + " from: " + sender)
      sender ! "Actioning command"
  }
}
