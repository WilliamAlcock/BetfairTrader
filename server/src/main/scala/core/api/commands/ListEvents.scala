package core.api.commands

import akka.actor.ActorRef
import domain.MarketFilter

case class ListEvents(marketFilter: MarketFilter, sender: ActorRef) extends Command
