package core.orderManager

import akka.actor.{Actor, ActorRef, Props, Scheduler}
import core.api.commands._
import core.api.output.Output
import core.dataProvider.polling.BEST
import core.eventBus.{EventBus, MessageEvent}
import core.orderManager.OrderManager._
import domain.Side.Side
import domain._
import org.joda.time.DateTime
import play.api.libs.json.Json
import server.Configuration
import service.BetfairService
import service.simService.OrderFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success


sealed case class OrderKey(marketId: String, selectionId: Long, handicap: Double, betId: String)
sealed case class MatchKey(marketId: String, selectionId: Long, handicap: Double, side: Side)

// TODO should orderManager make sure that any markets being operated on are being polled ?
class OrderManager(config: Configuration, sessionToken: String, controller: ActorRef, betfairService: BetfairService, eventBus: EventBus) extends Actor with OrderFactory {

  import context._

  def scheduler: Scheduler = context.system.scheduler

  // TODO orderManager should log number of orders placed/cancelled/replaced/updated to ensure systems acts within limits
  val startTime = DateTime.now()

  var timer = scheduler.schedule(config.orderManagerUpdateInterval, config.orderManagerUpdateInterval, self, Validate)

  // Keyed by MarketId
  var trackedOrders = Map.empty[OrderKey, Order]
  var matches = Map.empty[MatchKey, Match]

  override def preStart() = {
    super.preStart()
    trackedOrders = update(trackedOrders)
  }

  def broadcast(output: OrderManagerOutput): Unit = {
    eventBus.publish(MessageEvent(
      config.getOrderUpdateChannel(Some(output.marketId), Some(output.selectionId), Some(output.handicap)),
      output,
      self))
  }

  def update(trackedOrders: Map[OrderKey, Order]): Map[OrderKey, Order] = {
    try {
      val currentOrders = Await.result(betfairService.listCurrentOrders(sessionToken), 10 seconds)

      currentOrders match {
        case Some(ListCurrentOrdersContainer(x)) => x.currentOrders.map(orderSummary =>
            OrderKey(orderSummary.marketId, orderSummary.selectionId, orderSummary.handicap, orderSummary.betId) -> Order(
              orderSummary.betId,
              orderSummary.orderType,
              orderSummary.status,
              orderSummary.persistenceType,
              orderSummary.side,
              orderSummary.priceSize.price,
              orderSummary.priceSize.size,
              orderSummary.bspLiability,
              orderSummary.placedDate,
              orderSummary.averagePriceMatched,
              orderSummary.sizeMatched,
              orderSummary.sizeRemaining,
              orderSummary.sizeLapsed,
              orderSummary.sizeCancelled,
              orderSummary.sizeVoided
            )
          ).toMap
        case _ => trackedOrders
        // Unable to get current order from exchange
      }
    } catch {
      case _: Exception => trackedOrders
    }
  }

  /*
  Updated:
  Order IS tracked, IS in marketBookUpdate

  Placed:
  Order IS NOT tracked, IS in marketBookUpdate

  Executed:
  Order IS tracked, IS NOT in marketBookUpdate
 */

  // TODO test
  def updateOrder(marketId: String, selectionId: Long, handicap: Double, order: Order): Set[OrderManagerOutput] = {
      val key = OrderKey(marketId, selectionId, handicap, order.betId)
      trackedOrders.get(key) match {
        case Some(trackedOrder) if trackedOrder != order =>
          order.status match {
            case OrderStatus.EXECUTABLE =>
              trackedOrders = trackedOrders + (key -> order)
              Set(OrderUpdated(marketId, selectionId, handicap, CurrentOrderSummary.fromOrder(marketId, selectionId + "-" + handicap, order)))      // Order Updated
            case OrderStatus.EXECUTION_COMPLETE =>
              trackedOrders = trackedOrders - key
              Set(OrderExecuted(marketId, selectionId, handicap, CurrentOrderSummary.fromOrder(marketId, selectionId + "-" + handicap, order)))     // Order Executed
        }
        case _ => Set.empty[OrderManagerOutput]
      }
  }

  /*
  Filled:
  Match does not exist in matches or has changed
   */

  // TODO test
  def updateMatch(marketId: String, selectionId: Long, handicap: Double, _match: Match): Set[OrderManagerOutput] = {
    val key = MatchKey(marketId, selectionId, handicap, _match.side)
    matches.get(key) match {
      case Some(trackedMatch) if trackedMatch != _match =>
        matches = matches + (key -> _match)
        Set(OrderMatched(marketId, selectionId, handicap, _match))
      case None =>
        matches = matches + (key -> _match)
        Set(OrderMatched(marketId, selectionId, handicap, _match))
      case Some(_) => Set.empty[OrderManagerOutput]
    }
  }

  /*
    Process the orders for a given runner
    Returns a set of updates
   */
  def processRunner(marketId: String, runner: Runner): Set[OrderManagerOutput] = {
    val orderUpdates = runner.orders match {
      case Some(orders) => orders.map(updateOrder(marketId, runner.selectionId, runner.handicap, _)).flatten
      case _ => Set.empty[OrderManagerOutput]
    }
    val matchUpdates = runner.matches match {
      case Some(_matches) => _matches.map(updateMatch(marketId, runner.selectionId, runner.handicap, _)).flatten
      case _ => Set.empty[OrderManagerOutput]
    }
    orderUpdates ++ matchUpdates
  }

  /*
    Process the orders for each runner in the marketBook,
    Returns a set of updates
   */
  def processMarketBook(marketBook: MarketBook): Set[OrderManagerOutput] = {
    marketBook.runners.map(runner => processRunner(marketBook.marketId, runner)).flatten
  }

  /*
    Returns true if all orders have status == EXECUTION_COMPLETE
    false otherwise
   */
  def allOrdersCompleted(marketBook: MarketBook): Boolean = marketBook.runners.map(runner =>
    runner.orders.isDefined && runner.orders.get.forall(x => x.status != OrderStatus.EXECUTABLE)
  ).forall(x => x)

  // TODO test
  def listCurrentOrders(betIds: Set[String], marketIds: Set[String]): Set[CurrentOrderSummary] = {
    val output: Set[CurrentOrderSummary] = (marketIds.nonEmpty match {
      case true => trackedOrders.filterKeys(key => marketIds.contains(key.marketId))
      case false => trackedOrders
    }).map{case (key, order) => CurrentOrderSummary.fromOrder(key.marketId, key.selectionId + "-" + key.handicap, order)}.toSet

    if (betIds.nonEmpty) {
      output.filter(x => betIds.contains(x.betId))
    } else {
      output
    }
  }

  def listMatches: ListMatchesContainer = ListMatchesContainer(
    matches.map{case (key, _match) => OrderMatched(key.marketId, key.selectionId, key.handicap, _match)}.toSet
  )

  override def receive = {
    case Validate => trackedOrders = update(trackedOrders)

    case ListCurrentOrders(betIds: Set[String], marketIds: Set[String]) =>
      sender() ! ListCurrentOrdersContainer(CurrentOrderSummaryReport(listCurrentOrders(betIds, marketIds), moreAvailable = false))

    case ListMatches =>
      sender() ! listMatches

    case MarketBookUpdate(timestamp, marketBook) =>
      processMarketBook(marketBook).foreach(broadcast)
      if (allOrdersCompleted(marketBook)) controller ! UnSubscribeFromMarkets(Set(marketBook.marketId), BEST)           // If all the orders are matched unSubscribe from this market

    // TODO test, orders are placed once when a successful report has been received
    case x: PlaceExecutionReportContainer =>
      // if the status == success subscribe to updates for the market, add the order to the tracked orders, broadcast the order has been placed
      if (x.result.status == ExecutionReportStatus.SUCCESS) {
        controller ! SubscribeToMarkets(Set(x.result.marketId), BEST)
        x.result.instructionReports.map(report => {
          val key = OrderKey(x.result.marketId, report.instruction.selectionId, report.instruction.handicap, report.betId.get)
          val order = createOrder(report.instruction).copy(
            betId = report.betId.get,
            placedDate = report.placedDate.getOrElse(DateTime.now()),
            avgPriceMatched = report.averagePriceMatched.getOrElse(0.0),
            sizeMatched = report.sizeMatched.getOrElse(0.0)
          )
          trackedOrders = trackedOrders + (key -> order)
          Set(OrderPlaced(key.marketId, key.selectionId, key.handicap, CurrentOrderSummary.fromOrder(key.marketId, key.selectionId + "-" + key.handicap, order)))        // Order Placed
        }).flatten.foreach(broadcast)
      }

    // TODO test that the correct sender is called in the future, because sender() inside the future is NOT the sender. This has been fixed. It requires a test
    case PlaceOrders(marketId, instructions, customerRef) =>
      val _sender = sender()
      betfairService.placeOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) =>
          if (x.result.status == ExecutionReportStatus.SUCCESS) self ! x          // If the report is successful forward it to self to be tracked
          _sender ! x
        case _ => _sender ! OrderManagerException("Market " + marketId + " placeOrders failed!")
      }
    case CancelOrders(marketId, instructions, customerRef) =>
      val _sender = sender()
      betfairService.cancelOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) => _sender ! x
        case _ => _sender ! OrderManagerException("Market " + marketId + " cancelOrders failed!")
      }
    case ReplaceOrders(marketId, instructions, customerRef) =>
      val _sender = sender()
      betfairService.replaceOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) => _sender ! x
        case _ => _sender ! OrderManagerException("Market " + marketId + " replaceOrders failed!")
      }
    case UpdateOrders(marketId, instructions, customerRef) =>
      val _sender = sender()
      betfairService.updateOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) => _sender ! x
        case _ => _sender ! OrderManagerException("Market " + marketId + " updateOrders failed!")
      }
  }
}

object OrderManager {
  def props(config: Configuration, sessionToken: String, controller: ActorRef, betfairService: BetfairService, eventBus: EventBus) =
    Props(new OrderManager(config, sessionToken, controller, betfairService, eventBus))

  trait OrderManagerOutput extends Output {
    val marketId: String
    val selectionId: Long
    val handicap: Double
  }

  final case class OrderMatched(marketId: String, selectionId: Long, handicap: Double, _match: Match) extends OrderManagerOutput
  final case class OrderPlaced(marketId: String, selectionId: Long, handicap: Double, order: CurrentOrderSummary) extends OrderManagerOutput
  final case class OrderUpdated(marketId: String, selectionId: Long, handicap: Double, order: CurrentOrderSummary) extends OrderManagerOutput
  final case class OrderExecuted(marketId: String, selectionId: Long, handicap: Double, order: CurrentOrderSummary) extends OrderManagerOutput

  final case class ListMatchesContainer(matches: Set[OrderMatched]) extends Output

  implicit val formatOrderFilled = Json.format[OrderMatched]
  implicit val formatOrderPlaced = Json.format[OrderPlaced]
  implicit val formatOrderUpdated = Json.format[OrderUpdated]
  implicit val formatOrderExecuted = Json.format[OrderExecuted]
  implicit val formatListMatchesContainer = Json.format[ListMatchesContainer]

  case object Validate
}