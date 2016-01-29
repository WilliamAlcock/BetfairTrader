package core.api.commands

import akka.actor.ActorRef
import core.eventBus.Message

trait Command extends Message {
  val sender: ActorRef
}

