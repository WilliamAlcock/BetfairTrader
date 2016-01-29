package core.api.commands

import akka.actor.ActorRef
import domain.MarketFilter
import domain.MarketSort.MarketSort

case class ListMarketCatalogue(marketFilter: MarketFilter, sort: MarketSort, sender: ActorRef) extends Command
