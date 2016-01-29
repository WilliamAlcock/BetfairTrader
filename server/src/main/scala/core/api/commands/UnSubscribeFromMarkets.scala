package core.api.commands

import akka.actor.ActorRef
import core.dataProvider.polling.PollingGroup

case class UnSubscribeFromMarkets(markets: Set[String], pollingGroup: PollingGroup, sender: ActorRef) extends Command