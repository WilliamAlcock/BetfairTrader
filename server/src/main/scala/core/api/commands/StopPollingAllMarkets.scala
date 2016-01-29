package core.api.commands

import akka.actor.ActorRef

case class StopPollingAllMarkets(sender: ActorRef) extends Command

