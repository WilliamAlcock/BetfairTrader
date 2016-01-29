package core.api.commands

import akka.actor.ActorRef
import domain.MarketFilter

case class ListEventTypes(marketFilter: MarketFilter, sender: ActorRef) extends Command