package core.api.commands

import akka.actor.ActorRef
import domain.CancelInstruction

case class CancelOrders(marketId: String, instructions: Set[CancelInstruction], customerRef: Option[String] = None, sender: ActorRef) extends Command