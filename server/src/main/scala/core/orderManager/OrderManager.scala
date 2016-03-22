package core.orderManager

import akka.actor.{Scheduler, Actor, ActorRef, Props}
import core.api.commands._
import core.api.output.Output
import core.dataProvider.polling.BEST
import core.eventBus.{MessageEvent, EventBus}
import core.orderManager.OrderManager._
import domain._
import org.joda.time.DateTime
import server.Configuration
import service.BetfairService
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

import scala.util.Success

sealed case class OrderData(marketId: String, selectionId: Long, handicap: Double, order: Order)

// TODO should orderManager make sure that any markets being operated on are being polled ?
class OrderManager(config: Configuration, sessionToken: String, controller: ActorRef, betfairService: BetfairService, eventBus: EventBus) extends Actor {

  import context._

  def scheduler: Scheduler = context.system.scheduler

  // TODO orderManager should log number of orders placed/cancelled/replaced/updated to ensure systems acts within limits
  val startTime = DateTime.now()

  var timer = scheduler.schedule(config.orderManagerUpdateInterval, config.orderManagerUpdateInterval, self, Validate)

  var trackedOrders = Map.empty[String, OrderData]
  var executedOrders = Map.empty[String, OrderData]

  override def preStart() = {
    super.preStart()
    trackedOrders = update(trackedOrders)
  }

  def broadcast(output: OrderManagerOutput): Unit = {
    eventBus.publish(MessageEvent(
      config.getOrderUpdateChannel(Seq(output.marketId, output.selectionId.toString, output.handicap.toString)),
      output,
      self
    ))
  }

  def update(trackedOrders: Map[String, OrderData]): Map[String, OrderData] = {
    try {
      val currentOrders = Await.result(betfairService.listCurrentOrders(sessionToken), 10 seconds)

      currentOrders match {
        case Some(ListCurrentOrdersContainer(x)) => x.currentOrders.map(orderSummary =>
          orderSummary.betId -> OrderData(
            orderSummary.marketId,
            orderSummary.selectionId,
            orderSummary.handicap,
            Order(
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
    Process order for a given market and selection
    Returns a set of updates
   */
  def processOrder(marketId: String, selectionId: Long, handicap: Double, order: Order): Set[OrderManagerOutput] = order.status match {
    case OrderStatus.EXECUTABLE =>
      val output: Set[OrderManagerOutput] = trackedOrders.get(order.betId) match {                                                                            // Is the order being tracked ?
        case Some(trackedOrder) if order.sizeMatched > trackedOrder.order.sizeMatched =>
          Set(
            OrderFilled(marketId, selectionId, handicap, order, order.sizeMatched - trackedOrder.order.sizeMatched),
            OrderUpdated(marketId, selectionId, handicap, order)                                                          // Order Filled & Updated
          )
        case Some(trackedOrder) =>
          Set(OrderUpdated(marketId, selectionId, handicap, order))                                                                                        // Order Updated
        case None =>
          Set(OrderPlaced(marketId, selectionId, handicap, order))                                                                                         // Order Placed
      }
      trackedOrders = trackedOrders + (order.betId -> OrderData(marketId, selectionId, handicap, order))
      output
    case OrderStatus.EXECUTION_COMPLETE =>                                                                              // Is the order being tracked ?
      val output: Set[OrderManagerOutput] = trackedOrders.get(order.betId) match {
        case Some(trackedOrder) =>
          trackedOrders = trackedOrders - order.betId
          Set(OrderExecuted(marketId, selectionId, handicap, order))                                                                                       // Order Executed
        case None =>
          Set.empty[OrderManagerOutput]                                                                                   // Do Nothing
      }
      executedOrders = executedOrders + (order.betId -> OrderData(marketId, selectionId, handicap, order))
      output
  }

  /*
    Process the orders for a given runner
    Returns a set of updates
   */
  def processRunner(marketId: String, runner: Runner): Set[OrderManagerOutput] = runner.orders match {
    case Some(x) => x.map(order => processOrder(marketId, runner.selectionId, runner.handicap, order)).flatten
    case _ => Set.empty[OrderManagerOutput]
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
    runner.orders.isDefined && runner.orders.get.forall(x => x.status == OrderStatus.EXECUTION_COMPLETE)
  ).forall(x => x)

  override def receive = {
    case Validate => trackedOrders = update(trackedOrders)

    case MarketBookUpdate(timestamp, marketBook) =>
      processMarketBook(marketBook).foreach(broadcast)
      if (allOrdersCompleted(marketBook)) controller ! UnSubscribeFromMarkets(Set(marketBook.marketId), BEST)           // If all the orders are matched unSubscribe from this market

    case PlaceOrders(marketId, instructions, customerRef) =>
      betfairService.placeOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) =>
          // if the status == success subscribe to updates for the market
          if (x.result.status == ExecutionReportStatus.SUCCESS) {
            controller ! SubscribeToMarkets(Set(marketId), BEST)
          }
          sender() ! x
        case _ => sender() ! OrderManagerException("Market " + marketId + " placeOrders failed!")
      }
    case CancelOrders(marketId, instructions, customerRef) =>
      betfairService.cancelOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) => sender() ! x
        case _ => sender() ! OrderManagerException("Market " + marketId + " cancelOrders failed!")
      }
    case ReplaceOrders(marketId, instructions, customerRef) =>
      betfairService.replaceOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) => sender() ! x
        case _ => sender() ! OrderManagerException("Market " + marketId + " replaceOrders failed!")
      }
    case UpdateOrders(marketId, instructions, customerRef) =>
      betfairService.updateOrders(sessionToken, marketId, instructions, customerRef) onComplete {
        case Success(Some(x)) => sender() ! x
        case _ => sender() ! OrderManagerException("Market " + marketId + " updateOrders failed!")
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
    val order: Order
  }

  final case class OrderFilled(marketId: String, selectionId: Long, handicap: Double, order: Order, size: Double) extends OrderManagerOutput
  final case class OrderPlaced(marketId: String, selectionId: Long, handicap: Double, order: Order) extends OrderManagerOutput
  final case class OrderUpdated(marketId: String, selectionId: Long, handicap: Double, order: Order) extends OrderManagerOutput
  final case class OrderExecuted(marketId: String, selectionId: Long, handicap: Double, order: Order) extends OrderManagerOutput

  case object Validate
}