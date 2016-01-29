package core.api.commands

import akka.actor.ActorRef
import domain.UpdateInstruction

case class UpdateOrders(marketId: String, instructions: Set[UpdateInstruction], customerRef: Option[String] = None, sender: ActorRef) extends Command

