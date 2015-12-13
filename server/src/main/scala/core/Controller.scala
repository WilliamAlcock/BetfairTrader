package core

import akka.actor.{ActorRef, Terminated, Actor}
import core.api.commands._
import core.dataProvider.commands.{UnSubscribe, StopPollingMarkets, StartPollingMarkets}
import core.eventBus.{EventBus, MessageEvent}
import server.Configuration

class Controller(config: Configuration, eventBus: EventBus) extends Actor {

  import context._

  // TODO get these from config
  private val ORDER_MANAGER_CHANNEL = "orderManagerInstructions"
  private val DATA_PROVIDER_CHANNEL = "dataProviderInstructions"
  private val DATA_PROVIDER_OUTPUT_CHANNEL = "dataProviderOutput"       // TODO using this to send commands is bad
  private val MODEL_UPDATE_CHANNEL = "modelUpdates"

  // TODO this line of code is duplicated move it into central lib
  private def getPublishChannel(ids: Seq[String]): String = ids.foldLeft[String](MODEL_UPDATE_CHANNEL)((channel, id) => channel + "/" + id)

  private def getCommandChannel(x: Command): String = x match {
    case ListEventTypes           => DATA_PROVIDER_CHANNEL
    case x: ListEvents            => DATA_PROVIDER_CHANNEL
    case x: ListMarketCatalogue   => DATA_PROVIDER_CHANNEL
    case StopPollingAllMarkets    => DATA_PROVIDER_CHANNEL
    case x: PlaceOrders           => ORDER_MANAGER_CHANNEL
    case x: CancelOrders          => ORDER_MANAGER_CHANNEL
    case x: ReplaceOrders         => ORDER_MANAGER_CHANNEL
    case x: UpdateOrders          => ORDER_MANAGER_CHANNEL
  }

  var subscribers = Set.empty[ActorRef]

  system.log.info(self.toString)

  def watch(subscriber: ActorRef) = {
    if (!subscribers.contains(subscriber)) {
      subscribers += subscriber
      context watch subscriber
    }
  }

  def receive = {
    case Terminated(subscriber) =>
      subscribers -= subscriber
      eventBus.unsubscribe(subscriber)
      eventBus.publish(MessageEvent(DATA_PROVIDER_CHANNEL, UnSubscribe(subscriber.toString)))
      system.log.info("UnSubscribing " + subscriber)
    case x: String =>
      system.log.info("I have your message: " + x)
    case SubscribeToMarkets(marketIds, pollingGroup) =>
      watch(sender)
      marketIds.foreach(x => {
        val channel:String = getPublishChannel(Seq("marketBook", x))
        eventBus.subscribe(sender, channel)
        system.log.info("Subscribing " + sender + " to " + channel)
      })
      eventBus.publish(MessageEvent(DATA_PROVIDER_CHANNEL, StartPollingMarkets(marketIds, sender.toString, pollingGroup)))
    case UnSubscribeFromMarkets(marketIds, pollingGroup) =>
      marketIds.foreach(x => {
        val channel = getPublishChannel(Seq("marketBook", x))
        eventBus.unsubscribe(sender, channel)
        system.log.info("UnSubscribing " + sender + " from " + channel)
      })
      eventBus.publish(MessageEvent(DATA_PROVIDER_CHANNEL, StopPollingMarkets(marketIds, sender.toString, pollingGroup)))
    case SubscribeToNavData =>
      watch(sender)
      val channel = getPublishChannel(Seq("navData"))
      eventBus.subscribe(sender, channel)
      eventBus.publish(MessageEvent(DATA_PROVIDER_OUTPUT_CHANNEL , GetNavigationData))
      system.log.info("Subscribing " + sender + " to navData")
    case x: Command =>
      watch(sender)
      eventBus.publish(MessageEvent(getCommandChannel(x), x))
      system.log.info("actioning command: " + x + " from: " + sender)
  }
}
