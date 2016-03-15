package core.eventBus

import akka.actor.ActorRef

case class MessageEvent(channel: String, message: Any, sender: ActorRef)