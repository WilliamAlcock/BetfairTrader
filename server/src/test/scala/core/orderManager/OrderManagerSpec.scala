package core.orderManager

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.miguno.akka.testing.VirtualTime
import com.typesafe.config.ConfigFactory
import core.api.commands._
import core.dataProvider.polling.BEST
import core.eventBus.{EventBus, MessageEvent}
import core.orderManager.OrderManager._
import domain.ExecutionReportStatus.ExecutionReportStatus
import domain.OrderStatus.OrderStatus
import domain.{OrderType, PersistenceType, Side, _}
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}
import server.Configuration
import service.BetfairService

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class OrderManagerSpec extends TestKit(ActorSystem("TestSystem", ConfigFactory.parseString(""))) with FlatSpecLike with Matchers with BeforeAndAfterEach with MockFactory with ImplicitSender{

  var controller: TestProbe = _
  var betfairService: BetfairService = _
  var eventBus: EventBus = _
  var orderManager: TestActorRef[OrderManager] = _

  val sessionToken = "TEST_TOKEN"
  val marketId = "TEST_MARKET_ID"
  val customerRef = Some("TEST_CUSTOMER_REF")
  val betId = "TEST_BET_ID"
  val selectionId = 1L

  case class testBroadcast(marketId: String) extends OrderManagerOutput {
    override val selectionId = 1L
    override val handicap = 1.0
  }

  val testConfig = Configuration(
    appKey                    = "TEST_APP_KEY",
    username                  = "TEST_USERNAME",
    password                  = "TEST_PASSWORD",
    apiUrl                    = "TEST_API_URL",
    isoUrl                    = "TEST_ISO_URL",
    navUrl                    = "TEST_NAV_URL",
    orderManagerUpdateInterval = 1 second,
    systemAlertsChannel       = "TEST_SYSTEM_ALERTS",
    marketUpdateChannel       = "TEST_MARKET_UPDATES",
    orderUpdateChannel        = "TEST_ORDER_UPDATES",
    navDataInstructions       = "TEST_NAV_DATA_INSTRUCTIONS",
    dataModelInstructions     = "TEST_DATA_MODEL_INSTRUCTIONS",
    orderManagerInstructions  = "TEST_ORDER_MANAGER_INSTRUCTIONS",
    dataProviderInstructions  = "TEST_DATA_PROVIDER_INSTRUCTIONS"
  )

  class TestBetfairService extends BetfairService {
    val config = testConfig
    val command = null
  }

  override def beforeEach(): Unit = {
    controller = TestProbe()
    betfairService = mock[BetfairService]
    eventBus = mock[EventBus]
  }

  val placeOrders_Cases = Table(
    ("Betfair Service Output status",             "subscribeToMarkets"),

    (ExecutionReportStatus.SUCCESS,               true),
    (ExecutionReportStatus.PROCESSED_WITH_ERRORS, false),
    (ExecutionReportStatus.TIMEOUT,               false),
    (ExecutionReportStatus.FAILURE,               false)
  )

  forAll(placeOrders_Cases) { (reportStatus: ExecutionReportStatus, subscribeToMarkets: Boolean) =>
    "OrderManager.receive -> PlaceOrders" should "notify user when " + reportStatus in {
      orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
        override def preStart() = {}
      }))

      val instructions = Set(PlaceInstruction(OrderType.LIMIT, 1L, 0.0, Side.BACK))
      val serviceOutput = PlaceExecutionReportContainer(PlaceExecutionReport(reportStatus, marketId, None, Set(), None))

      (betfairService.placeOrders _)
        .expects(sessionToken, marketId, instructions, customerRef)
        .returns(Future.successful(Some(serviceOutput)))

      orderManager ! PlaceOrders(marketId, instructions, customerRef)

      if (subscribeToMarkets) {
        controller.expectMsg(SubscribeToMarkets(Set(marketId), BEST))
      }

      expectMsg(serviceOutput)
    }
  }

  "OrderManager.receive -> PlaceOrders" should "reply with an exception when there is no reply from betfairService" in {
    orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
      override def preStart() = {}
    }))

    val instructions = Set(PlaceInstruction(OrderType.LIMIT, 1L, 0.0, Side.BACK))

    (betfairService.placeOrders _)
      .expects(sessionToken, marketId, instructions, customerRef)
      .returns(Future.successful(None))

    orderManager ! PlaceOrders(marketId, instructions, customerRef)

    expectMsg(OrderManagerException("Market TEST_MARKET_ID placeOrders failed!"))
  }

  val cancelOrders_Cases = Table(
    "Betfair Service Output status",

    ExecutionReportStatus.SUCCESS,
    ExecutionReportStatus.PROCESSED_WITH_ERRORS,
    ExecutionReportStatus.TIMEOUT,
    ExecutionReportStatus.FAILURE
  )

  forAll(cancelOrders_Cases) { (reportStatus: ExecutionReportStatus) =>
    "OrderManager.receive -> CancelOrders" should "notify user when " + reportStatus in {
      orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
        override def preStart() = {}
      }))

      val sizeReduction = Some(10.0)

      val instructions = Set(CancelInstruction(betId, sizeReduction))
      val serviceOutput = CancelExecutionReportContainer(CancelExecutionReport(reportStatus, marketId, None, Set(), None))

      (betfairService.cancelOrders _)
        .expects(sessionToken, marketId, instructions, customerRef)
        .returns(Future.successful(Some(serviceOutput)))

      orderManager ! CancelOrders(marketId, instructions, customerRef)

      expectMsg(serviceOutput)
    }
  }

  "OrderManager.receive -> CancelOrders" should "reply with an exception when there is no reply from betfairService" in {
    orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
      override def preStart() = {}
    }))

    val sizeReduction = Some(10.0)

    val instructions = Set(CancelInstruction(betId, sizeReduction))

    (betfairService.cancelOrders _)
      .expects(sessionToken, marketId, instructions, customerRef)
      .returns(Future.successful(None))

    orderManager ! CancelOrders(marketId, instructions, customerRef)

    expectMsg(OrderManagerException("Market TEST_MARKET_ID cancelOrders failed!"))
  }

  val replaceOrders_Cases = Table(
    "Betfair Service Output status",

    ExecutionReportStatus.SUCCESS,
    ExecutionReportStatus.PROCESSED_WITH_ERRORS,
    ExecutionReportStatus.TIMEOUT,
    ExecutionReportStatus.FAILURE
  )

  forAll(replaceOrders_Cases) { (reportStatus: ExecutionReportStatus) =>
    "OrderManager.receive -> ReplaceOrders" should "notify user when " + reportStatus in {
      orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
        override def preStart() = {}
      }))

      val newPrice = 10.0

      val instructions = Set(ReplaceInstruction(betId, newPrice))
      val serviceOutput = ReplaceExecutionReportContainer(ReplaceExecutionReport(customerRef, reportStatus, None, marketId, Set()))

      (betfairService.replaceOrders _)
        .expects(sessionToken, marketId, instructions, customerRef)
        .returns(Future.successful(Some(serviceOutput)))

      orderManager ! ReplaceOrders(marketId, instructions, customerRef)

      expectMsg(serviceOutput)
    }
  }

  "OrderManager.receive -> ReplaceOrders" should "reply with an exception when there is no reply from betfairService" in {
    orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
      override def preStart() = {}
    }))

    val newPrice = 10.0

    val instructions = Set(ReplaceInstruction(betId, newPrice))

    (betfairService.replaceOrders _)
      .expects(sessionToken, marketId, instructions, customerRef)
      .returns(Future.successful(None))

    orderManager ! ReplaceOrders(marketId, instructions, customerRef)

    expectMsg(OrderManagerException("Market TEST_MARKET_ID replaceOrders failed!"))
  }

  val updateOrders_Cases = Table(
    "Betfair Service Output status",

    ExecutionReportStatus.SUCCESS,
    ExecutionReportStatus.PROCESSED_WITH_ERRORS,
    ExecutionReportStatus.TIMEOUT,
    ExecutionReportStatus.FAILURE
  )

  forAll(cancelOrders_Cases) { (reportStatus: ExecutionReportStatus) =>
    "OrderManager.receive -> UpdateOrders" should "notify user when " + reportStatus in {
      orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
        override def preStart() = {}
      }))

      val newPersistenceType = PersistenceType.LAPSE

      val instructions = Set(UpdateInstruction(betId, newPersistenceType))
      val serviceOutput = UpdateExecutionReportContainer(UpdateExecutionReport(reportStatus, marketId, None, Set(), customerRef))

      (betfairService.updateOrders _)
        .expects(sessionToken, marketId, instructions, customerRef)
        .returns(Future.successful(Some(serviceOutput)))

      orderManager ! UpdateOrders(marketId, instructions, customerRef)

      expectMsg(serviceOutput)
    }
  }

  "OrderManager.receive -> UpdateOrders" should "reply with an exception when there is no reply from betfairService" in {
    orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
      override def preStart() = {}
    }))

    val newPersistenceType = PersistenceType.LAPSE

    val instructions = Set(UpdateInstruction(betId, newPersistenceType))

    (betfairService.updateOrders _)
      .expects(sessionToken, marketId, instructions, customerRef)
      .returns(Future.successful(None))

    orderManager ! UpdateOrders(marketId, instructions, customerRef)

    expectMsg(OrderManagerException("Market TEST_MARKET_ID updateOrders failed!"))
  }


  "OrderManager.receive -> MarketBookUpdate" should "process the marketBook and broadcast the results and unSubscribe from the market if all the orders are completed" in {

    List(true, false).foreach(allOrdersAreCompleted => {
      val _processMarketBook = mockFunction[MarketBook, Set[OrderManagerOutput]]
      val _broadcast = mockFunction[OrderManagerOutput, Unit]
      val _allOrdersCompleted = mockFunction[MarketBook, Boolean]

      orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
        override def preStart() = {}

        override def processMarketBook(m: MarketBook): Set[OrderManagerOutput] = _processMarketBook.apply(m)
        override def broadcast(o: OrderManagerOutput): Unit = _broadcast.apply(o)
        override def allOrdersCompleted(m: MarketBook): Boolean = _allOrdersCompleted.apply(m)
      }))

      val marketBookUpdate = MarketBookUpdate(
        DateTime.now(),
        MarketBook(
          marketId = marketId,
          isMarketDataDelayed = false,
          status = "TEST_STATUS",
          betDelay = 1,
          bspReconciled = false,
          complete = false,
          inplay = false,
          numberOfWinners = 2,
          numberOfRunners = 3,
          numberOfActiveRunners = 4,
          lastMatchTime = Some(DateTime.now()),
          totalMatched = 1.0,
          totalAvailable = 2.0,
          crossMatching = false,
          runnersVoidable = false,
          version = 1L,
          runners = Set(Runner(1L, 1.0, "TEST_RUNNER_STATUS"))
        )
      )

      _processMarketBook
        .expects(marketBookUpdate.data)
        .returns(Set(testBroadcast("1")))

      _broadcast
        .expects(testBroadcast("1"))

      _allOrdersCompleted
        .expects(marketBookUpdate.data)
        .returns(allOrdersAreCompleted)

      orderManager ! marketBookUpdate

      if (allOrdersAreCompleted) {
        controller.expectMsg(UnSubscribeFromMarkets(Set(marketBookUpdate.data.marketId), BEST))
      } else {
        controller.expectNoMsg()
      }
    })
  }

  val allOrdersCompleted_Cases = Table(
    ("order statuses",                                                          "expectedOutput"),

    (List(OrderStatus.EXECUTABLE, OrderStatus.EXECUTABLE),                      false),
    (List(OrderStatus.EXECUTABLE, OrderStatus.EXECUTION_COMPLETE),              false),
    (List(OrderStatus.EXECUTION_COMPLETE, OrderStatus.EXECUTION_COMPLETE),      true)
  )

  forAll(allOrdersCompleted_Cases) { (orderStatuses: List[OrderStatus], expectedOutput: Boolean) =>
    "OrderManager.allOrdersCompleted" should "return " + expectedOutput + " for " + orderStatuses in {
      val input = MarketBook(
        marketId = marketId,
        isMarketDataDelayed = false,
        status = "TEST_STATUS",
        betDelay = 1,
        bspReconciled = false,
        complete = false,
        inplay = false,
        numberOfWinners = 2,
        numberOfRunners = 3,
        numberOfActiveRunners = 4,
        lastMatchTime = Some(DateTime.now()),
        totalMatched = 1.0,
        totalAvailable = 2.0,
        crossMatching = false,
        runnersVoidable = false,
        version = 1L,
        runners = Set(
          Runner(1L, 1.0, "TEST_RUNNER_STATUS", orders = Some(orderStatuses.map(x =>
              Order(betId, OrderType.LIMIT, x, PersistenceType.LAPSE, Side.BACK, 1.0, 2.0, 3.0, DateTime.now(), 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
            ).toSet)
          )
        )
      )

      orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
        override def preStart() = {}
      }))

      orderManager.underlyingActor.allOrdersCompleted(input) should be (expectedOutput)
    }
  }

  "OrderManager.processMarketBook" should "return the output of processRunner for each runner in the given marketBook" in {
    val runners = List(
      Runner(1L, 1.0, "TEST_RUNNER_STATUS_1"),
      Runner(2L, 2.0, "TEST_RUNNER_STATUS_2"),
      Runner(3L, 3.0, "TEST_RUNNER_STATUS_3")
    )

    val input = MarketBook(
      marketId = marketId,
      isMarketDataDelayed = false,
      status = "TEST_STATUS",
      betDelay = 1,
      bspReconciled = false,
      complete = false,
      inplay = false,
      numberOfWinners = 2,
      numberOfRunners = 3,
      numberOfActiveRunners = 4,
      lastMatchTime = Some(DateTime.now()),
      totalMatched = 1.0,
      totalAvailable = 2.0,
      crossMatching = false,
      runnersVoidable = false,
      version = 1L,
      runners = runners.toSet
    )

    val _processRunner = mockFunction[String, Runner, Set[OrderManagerOutput]]

    val expectedOutput = List(
      testBroadcast("1"),
      testBroadcast("2"),
      testBroadcast("3")
    )

    _processRunner.expects(marketId, runners(0)).returns(Set[OrderManagerOutput](expectedOutput(0)))
    _processRunner.expects(marketId, runners(1)).returns(Set[OrderManagerOutput](expectedOutput(1)))
    _processRunner.expects(marketId, runners(2)).returns(Set[OrderManagerOutput](expectedOutput(2)))

    orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
      override def preStart() = {}

      override def processRunner(marketId: String, runner: Runner): Set[OrderManagerOutput] = _processRunner.apply(marketId, runner)
    }))

    orderManager.underlyingActor.processMarketBook(input) should be (expectedOutput.toSet)
  }

  val processRunner_Cases = Table(
    ("orders defined", "matches defined"),
    (true, true),
    (true, false),
    (false, true),
    (false, false)
  )

  forAll(processRunner_Cases){(ordersDefined: Boolean, matchesDefined: Boolean) =>
    "OrderManager.processRunner" should "return the orderManagerOutput for the orders and matches in the given runner, orders: " + ordersDefined + ", matches: " + matchesDefined in {
      val _updateOrder = mockFunction[String, Long, Double, Order, Set[OrderManagerOutput]]
      val _removeCompletedOrders = mockFunction[String, Long, Double, Set[Order], Set[OrderManagerOutput]]
      val _updateMatch = mockFunction[String, Long, Double, Match, Set[OrderManagerOutput]]

      orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
        override def preStart() = {}

        override def updateOrder(m: String, s: Long, h: Double, o: Order): Set[OrderManagerOutput] = _updateOrder.apply(m, s, h, o)
        override def removeCompletedOrders(m: String, s: Long, h: Double, o: Set[Order]): Set[OrderManagerOutput] = _removeCompletedOrders.apply(m, s, h, o)
        override def updateMatch(m: String, s: Long, h: Double, ma: Match): Set[OrderManagerOutput] = _updateMatch.apply(m, s, h, ma)
      }))

      val orders = List(
        Order(betId, OrderType.LIMIT, OrderStatus.EXECUTABLE, PersistenceType.LAPSE, Side.BACK, 1.0, 1.0, 1.0, DateTime.now(), 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
        Order(betId, OrderType.LIMIT, OrderStatus.EXECUTABLE, PersistenceType.LAPSE, Side.BACK, 2.0, 2.0, 2.0, DateTime.now(), 2.0, 2.0, 2.0, 2.0, 2.0, 2.0)
      )

      val matches = List(
        Match(side = Side.BACK, price = 1.0, size = 2.0),
        Match(side = Side.LAY, price = 1.0, size = 2.0)
      )

      val runner = Runner(1L, 1.0, "TEST_RUNNER_STATUS_1",
        orders = if (ordersDefined) Some(orders.toSet) else None,
        matches = if (matchesDefined) Some(matches.toSet) else None
      )

      if (ordersDefined) {
        _updateOrder.expects(marketId, 1L, 1.0, orders(0)).returns(Set(testBroadcast("1")))
        _updateOrder.expects(marketId, 1L, 1.0, orders(1)).returns(Set(testBroadcast("2")))

        _removeCompletedOrders.expects(marketId, 1L, 1.0, orders.toSet[Order]).returns(Set(testBroadcast("3")))
      }

      if (matchesDefined) {
        _updateMatch.expects(marketId, 1L, 1.0, matches(0)).returns(Set(testBroadcast("4")))
        _updateMatch.expects(marketId, 1L, 1.0, matches(1)).returns(Set(testBroadcast("5")))
      }

      val expectedOutput = (ordersDefined, matchesDefined) match {
        case (true, true) => Set(testBroadcast("1"), testBroadcast("2"), testBroadcast("3"), testBroadcast("4"), testBroadcast("5"))
        case (true, false) => Set(testBroadcast("1"), testBroadcast("2"), testBroadcast("3"))
        case (false, true) => Set(testBroadcast("4"), testBroadcast("5"))
        case (false, false) => Set()
      }

      orderManager.underlyingActor.processRunner(marketId, runner) should be (expectedOutput)
    }
  }

//  private def generateOrderManagerOutput(s: String, marketId: String, selectionId: Long, handicap: Double, order: Order, size: Double = 0): OrderManagerOutput = s match {
//    case "OrderPlaced" => OrderPlaced(CurrentOrderSummary.fromOrder(marketId, selectionId + "-" + handicap, order))
//    case "OrderFilled" => OrderFilled(CurrentOrderSummary.fromOrder(marketId, selectionId + "-" + handicap, order), size)
//    case "OrderUpdated" => OrderUpdated(CurrentOrderSummary.fromOrder(marketId, selectionId + "-" + handicap, order))
//    case "OrderExecuted" => OrderExecuted(CurrentOrderSummary.fromOrder(marketId, selectionId + "-" + handicap, order))
//  }
//
//  val processOrder_Cases = Table(
//    ("order status",                  "is bet tracked", "has sizeMatched increased",  "expected output"),
//    (OrderStatus.EXECUTABLE,          true,             true,                         Set("OrderFilled", "OrderUpdated")),
//    (OrderStatus.EXECUTABLE,          true,             false,                        Set("OrderUpdated")),
//    (OrderStatus.EXECUTABLE,          false,            false,                        Set("OrderPlaced")),
//    (OrderStatus.EXECUTABLE,          false,            true,                         Set("OrderPlaced")),
//    (OrderStatus.EXECUTION_COMPLETE,  true,             true,                         Set("OrderExecuted")),
//    (OrderStatus.EXECUTION_COMPLETE,  true,             false,                        Set("OrderExecuted")),
//    (OrderStatus.EXECUTION_COMPLETE,  false,            false,                        Set.empty[String]),
//    (OrderStatus.EXECUTION_COMPLETE,  false,            true,                         Set.empty[String])
//  )
//
//  forAll(processOrder_Cases) { (orderStatus: OrderStatus, isBetTracked: Boolean, hasSizeMatchedIncreased: Boolean, expectedOutput: Set[String]) =>
//    "OrderManager.processOrder" should "return " + expectedOutput + " when " + isBetTracked + " " + hasSizeMatchedIncreased + " " + orderStatus in {
//      val handicap = 1.2
//      val order = Order(betId, OrderType.LIMIT, orderStatus, PersistenceType.LAPSE, Side.BACK, 1.0, 1.0, 1.0, DateTime.now(), 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
//
//      orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
//        override def preStart() = {}
//      }))
//
//      if (isBetTracked) {
//        orderManager.underlyingActor.trackedOrders = Map[String, OrderData](betId -> OrderData(marketId, selectionId, 1.0, order))
//      }
//
//      val placedOrder = if (hasSizeMatchedIncreased) order.copy(sizeMatched = order.sizeMatched + 10) else order
//
//      orderManager.underlyingActor.processOrder(marketId, selectionId, handicap, placedOrder) should be (expectedOutput.map(x => generateOrderManagerOutput(x, marketId, selectionId, handicap, placedOrder, 10)))
//    }
//  }

  "OrderManager.broadcast" should "publish the given output on the orderUpdateChannel" in {
    val _publish = mockFunction[MessageEvent, Unit]

    class TestEventBus extends EventBus {
      override def publish(m: MessageEvent) = _publish.apply(m)
    }

    val output = testBroadcast("1")
    val channel = "TEST_ORDER_UPDATES/" + output.marketId + "/" + output.selectionId + "/" + output.handicap

    orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, new TestEventBus) {
      override def preStart() = {}
    }))

    _publish.expects(MessageEvent(channel, output, orderManager))

    orderManager.underlyingActor.broadcast(output)
  }

  "OrderManager" should "update tracked orders from the exchange on startup and at intervals defined by config" in {
    val _update = mockFunction[Map[String, Set[OrderData]], Map[String, Set[OrderData]]]

    val time = new VirtualTime

    _update.expects(Map.empty[String, Set[OrderData]]).returns(Map.empty[String, Set[OrderData]]).repeated(2)

    orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
      override def update(to: Map[String, Set[OrderData]]) = _update.apply(to)
      override def scheduler = time.scheduler
    }))

    time.advance(testConfig.orderManagerUpdateInterval)
  }
}