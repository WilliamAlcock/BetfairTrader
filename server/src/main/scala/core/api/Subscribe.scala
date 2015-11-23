package core.api

import akka.actor.ActorRef

case class Subscribe(subscriber: ActorRef, subChannels: Seq[String] = Seq()) extends Command


