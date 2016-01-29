package core.api.commands

import akka.actor.ActorRef
import domain.PlaceInstruction

case class PlaceOrders(marketId: String, instructions: Set[PlaceInstruction], customerRef: Option[String] = None, sender: ActorRef) extends Command
