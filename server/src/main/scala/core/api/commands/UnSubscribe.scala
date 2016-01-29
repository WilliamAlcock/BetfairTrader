package core.api.commands

import akka.actor.ActorRef

case class UnSubscribe(sender: ActorRef) extends Command
