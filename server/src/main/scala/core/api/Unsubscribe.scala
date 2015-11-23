package core.api

import akka.actor.ActorRef

case class Unsubscribe(subscriber: ActorRef, subChannels: Seq[String] = Seq()) extends Command


