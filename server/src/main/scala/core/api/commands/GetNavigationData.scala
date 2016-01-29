package core.api.commands

import akka.actor.ActorRef

case class GetNavigationData(sender: ActorRef) extends Command
