package core.api.commands

import akka.actor.ActorRef
import core.dataProvider.polling.PollingGroup

case class SubscribeToMarkets(markets: Set[String], pollingGroup: PollingGroup, sender: ActorRef) extends Command