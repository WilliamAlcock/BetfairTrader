package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import core.api.{ListEventTypes, Output, Subscribe}

class WebSocketActor(out: ActorRef) extends Actor with ActorLogging {
  // TODO get this from config
  println ("SETTING UP WEB SOCKET ACTOR")
  val remote = context.actorSelection("akka.tcp://BFTrader@127.0.0.1:2552/user/controller")

  override def receive = {
    case "Subscribe" =>
      println ("Subscribing")
      remote ! Subscribe(self, Seq("*"))
    case "ListEventTypes" =>
      println ("listing events")
      remote ! ListEventTypes()
    case x: Output =>
      println ("I have a message: " + x)
      out ! (x.toString)
//    case x: String =>
//      remote ! x
//      println ("I have a message: " + x)
//      out ! ("I have received your message " + x)
  }

  override def postStop() = {
    // close down functions
    println ("I am closing down")
  }
}

object WebSocketActor {
  def props(out: ActorRef) = Props(new WebSocketActor(out))
}