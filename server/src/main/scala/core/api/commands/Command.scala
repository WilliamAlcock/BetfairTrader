package core.api.commands

import core.dataProvider.polling.PollingGroup
import core.eventBus.Message
import domain.MarketSort.MarketSort
import domain._
import play.api.libs.json.Json

trait Command extends Message

case class GetNavigationData(eventTypeId: String) extends Command

case class PlaceOrders(marketId: String, instructions: Set[PlaceInstruction], customerRef: Option[String] = None) extends Command
case class CancelOrders(marketId: String, instructions: Set[CancelInstruction], customerRef: Option[String] = None) extends Command
case class ReplaceOrders(marketId: String, instructions: Set[ReplaceInstruction], customerRef: Option[String] = None) extends Command
case class UpdateOrders(marketId: String, instructions: Set[UpdateInstruction], customerRef: Option[String] = None) extends Command

case class ListEvents(marketFilter: MarketFilter) extends Command
case class ListEventTypes(marketFilter: MarketFilter) extends Command
case class ListMarketCatalogue(marketFilter: MarketFilter, sort: MarketSort) extends Command
case class ListMarketBook(marketIds: Set[String]) extends Command
case class ListCurrentOrders(betIds: Set[String] = Set.empty, marketIds: Set[String] = Set.empty) extends Command
case object ListMatches extends Command

case object SubscribeToSystemAlerts extends Command
case class SubscribeToMarkets(markets: Set[String], pollingGroup: PollingGroup) extends Command
case class UnSubscribeFromMarkets(markets: Set[String], pollingGroup: PollingGroup) extends Command
case object UnSubscribe extends Command
case object StopPollingAllMarkets extends Command

case class SubscribeToOrderUpdates(marketId: Option[String] = None, selectionId: Option[Long] = None, handicap: Option[Double] = None) extends Command

object Command {
  implicit val formatGetNavigationData = Json.format[GetNavigationData]

  implicit val formatPlaceOrders = Json.format[PlaceOrders]
  implicit val formatCancelOrders = Json.format[CancelOrders]
  implicit val formatReplaceOrders = Json.format[ReplaceOrders]
  implicit val formatUpdateOrders = Json.format[UpdateOrders]

  implicit val formatListEvents = Json.format[ListEvents]
  implicit val formatListEventTypes = Json.format[ListEventTypes]
  implicit val formatListMarketCatalogue = Json.format[ListMarketCatalogue]
  implicit val formatListMarketBook = Json.format[ListMarketBook]
  implicit val formatListCurrentOrders = Json.format[ListCurrentOrders]

  implicit val formatSubscribeToMarkets = Json.format[SubscribeToMarkets]
  implicit val formatUnSubscribeFromMarkets = Json.format[UnSubscribeFromMarkets]

  implicit val formatSubscribeToOrderUpdates = Json.format[SubscribeToOrderUpdates]
}