package core

import akka.actor.{Actor, ActorRef, Props, Terminated}
import core.api.commands._
import core.eventBus.{EventBus, MessageEvent}
import server.Configuration

class Controller(config: Configuration, eventBus: EventBus) extends Actor {

  import context._

  private def getCommandChannel(x: Command): String = x match {
    case x: GetNavigationData     => config.navDataInstructions
    case x: ListEventTypes        => config.dataProviderInstructions
    case x: ListEvents            => config.dataProviderInstructions
    case x: ListMarketCatalogue   => config.dataProviderInstructions
    case x: ListMarketBook        => config.dataModelInstructions
    case StopPollingAllMarkets    => config.dataProviderInstructions
    case x: PlaceOrders           => config.orderManagerInstructions
    case x: CancelOrders          => config.orderManagerInstructions
    case x: ReplaceOrders         => config.orderManagerInstructions
    case x: UpdateOrders          => config.orderManagerInstructions
    case x: ListCurrentOrders     => config.orderManagerInstructions
    case ListMatches              => config.orderManagerInstructions
  }

  var subscribers = Set.empty[ActorRef]

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
      eventBus.publish(MessageEvent(config.dataProviderInstructions, UnSubscribe, subscriber))      // UnSubscribe from dataProvider
      system.log.info("UnSubscribing " + subscriber)

    case SubscribeToSystemAlerts =>
      watch(sender())
      eventBus.subscribe(sender(), config.systemAlertsChannel)

    case SubscribeToMarkets(marketIds, pollingGroup) =>
      watch(sender())
      marketIds.foreach(id => {
        val channel = config.getMarketUpdateChannel(Some(id))
        eventBus.subscribe(sender(), channel)
        system.log.info("Subscribing " + sender() + " to " + channel)
      })
      eventBus.publish(MessageEvent(config.dataProviderInstructions, SubscribeToMarkets(marketIds, pollingGroup), sender()))

    case UnSubscribeFromMarkets(marketIds, pollingGroup) =>
      marketIds.foreach(id => {
        val channel = config.getMarketUpdateChannel(Some(id))
        eventBus.unsubscribe(sender(), channel)
        system.log.info("UnSubscribing " + sender + " from " + channel)
      })
      eventBus.publish(MessageEvent(config.dataProviderInstructions, UnSubscribeFromMarkets(marketIds, pollingGroup), sender()))

    case SubscribeToOrderUpdates(marketId, selectionId, handicap) =>
      watch(sender())
      val channel = config.getOrderUpdateChannel(marketId, selectionId, handicap)
      eventBus.subscribe(sender(), channel)
      system.log.info("Subscribing " + sender() + " to " + channel)

    case x: Command =>
      watch(sender())
      eventBus.publish(MessageEvent(getCommandChannel(x), x, sender()))
      system.log.info("Actioning command: " + x)
    case x: String =>
      system.log.info("I have your message: " + x)
  }
}

object Controller {
  def props(config: Configuration, eventBus: EventBus) = Props(new Controller(config, eventBus))
}
