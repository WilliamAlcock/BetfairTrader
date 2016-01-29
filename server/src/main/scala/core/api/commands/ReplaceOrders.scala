package core.api.commands

import akka.actor.ActorRef
import domain.ReplaceInstruction

case class ReplaceOrders(marketId: String, instructions: Set[ReplaceInstruction], customerRef: Option[String] = None, sender: ActorRef) extends Command

