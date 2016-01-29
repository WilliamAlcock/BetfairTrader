package core

import akka.actor.{ActorRef, Terminated, Actor}
import core.api.commands._
import core.eventBus.{EventBus, MessageEvent}
import server.Configuration

class Controller(config: Configuration, eventBus: EventBus) extends Actor {

  import context._

  private def getCommandChannel(x: Command): String = x match {
    case x: ListEventTypes        => config.dataProviderInstructions
    case x: ListEvents            => config.dataProviderInstructions
    case x: ListMarketCatalogue   => config.dataProviderInstructions
    case x: StopPollingAllMarkets => config.dataProviderInstructions
    case x: PlaceOrders           => config.orderManagerInstructions
    case x: CancelOrders          => config.orderManagerInstructions
    case x: ReplaceOrders         => config.orderManagerInstructions
    case x: UpdateOrders          => config.orderManagerInstructions
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
      eventBus.unsubscribe(subscriber)                                                              // UnSubscribe from eventBus
      eventBus.publish(MessageEvent(config.dataProviderInstructions, UnSubscribe(subscriber)))      // UnSubscribe from dataProvider
      system.log.info("UnSubscribing " + subscriber)
    case SubscribeToMarkets(marketIds, pollingGroup, subscriber) =>
      if (!sender().equals(subscriber)) println("WARNING !!! Subscriber != Sender")
      watch(subscriber)
      marketIds.foreach(x => {
        val channel = config.getPublishChannel(Seq(x))
        eventBus.subscribe(subscriber, channel)
        system.log.info("Subscribing " + subscriber + " to " + channel)
      })
      eventBus.publish(MessageEvent(config.dataProviderInstructions, SubscribeToMarkets(marketIds, pollingGroup, subscriber)))
    case UnSubscribeFromMarkets(marketIds, pollingGroup, subscriber) =>
      if (!sender().equals(subscriber)) println("WARNING !!! Subscriber != Sender")
      marketIds.foreach(x => {
        val channel = config.getPublishChannel(Seq(x))
        eventBus.unsubscribe(sender(), channel)
        system.log.info("UnSubscribing " + sender + " from " + channel)
      })
      eventBus.publish(MessageEvent(config.dataProviderInstructions, UnSubscribeFromMarkets(marketIds, pollingGroup, subscriber)))
    case x: GetNavigationData => eventBus.publish(MessageEvent(config.dataModelInstructions, x))
    case x: Command =>
      if (!sender().equals(x.sender)) println("WARNING !!! Subscriber != Sender")
      watch(x.sender)
      eventBus.publish(MessageEvent(getCommandChannel(x), x))
      system.log.info("Actioning command: " + x)
    case x: String =>
      system.log.info("I have your message: " + x)
  }
}
