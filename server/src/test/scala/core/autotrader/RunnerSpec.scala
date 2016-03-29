package core.autotrader

import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import core.api.commands._
import core.autotrader.Runner._
import core.dataProvider.polling.BEST
import core.orderManager.OrderManager.{OrderExecuted, OrderUpdated, OrderPlaced}
import core.orderManager.OrderManagerException
import domain.Side.Side
import domain._
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}
import scala.concurrent.duration._
import scala.language.postfixOps
import org.scalatest.prop.TableDrivenPropertyChecks._


class RunnerSpec extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(""))) with FlatSpecLike with Matchers with MockFactory with BeforeAndAfterEach {

  var mockController : TestProbe = _

  val marketId = "TEST_MARKET_ID"
  val selectionId = 1L
  val betId = "TEST_BET_ID"
  val instruction = PlaceInstruction(OrderType.LIMIT, selectionId, 0.0, Side.BACK)
  val mockStrategy = Strategy(
    marketId,
    selectionId,
    stopPrice = 1.0,
    stopSide = Side.BACK,
    startTime = Some(DateTime.now().plusMinutes(30)),
    instructions = List(instruction)
  )

  val mockMarketBook = MarketBookUpdate(DateTime.now(), MarketBook(marketId, false, "", 0, false, false, false, 0, 0, 0, None, 0, 0, false, false, 0, Set()))

  val mockCurrentOrderSummary =
    CurrentOrderSummary(betId, marketId, selectionId, 0, PriceSize(0,0), 1, Side.BACK, OrderStatus.EXECUTABLE, PersistenceType.LAPSE, OrderType.LIMIT, DateTime.now(), None, 0, 0, 0, 0, 0, 0, "")

  override def beforeEach(): Unit = {
    mockController = TestProbe()

  }

  "mockRunner" should "start in Idle state with NoData" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))
    mockRunner.stateName should be (Idle)
    mockRunner.stateData should be (NoData)

    mockController.expectNoMsg(200 millis)
    mockRunner.stop()
  }

  "mockRunner when (Idle)" should "transition to Initialised and update Data when Init(strategy) received" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))

    mockRunner ! Init(mockStrategy)

    mockController.expectMsg(SubscribeToMarkets(Set(marketId), BEST))
    mockController.expectMsg(SubscribeToOrderUpdates(Some(marketId), Some(selectionId)))
    mockController.expectMsg(ListMarketBook(Set(marketId)))

    mockRunner.isTimerActive("StartTimer") should be (true)

    mockRunner.stateName should be (Initialised)
    mockRunner.stateData should be (mockStrategy)

    mockRunner.stop()
  }

  "mockRunner when (Initialised)" should "update marketBook and stay when MarketBookUpdate received" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))

    mockRunner.setState(Initialised, mockStrategy)

    mockRunner ! mockMarketBook

    mockRunner.stateName should be (Initialised)
    mockRunner.stateData should be (mockStrategy.copy(marketBook = Some(mockMarketBook)))

    mockController.expectNoMsg(200 millis)
    mockRunner.stop()
  }

  List[Option[DateTime]](Some(DateTime.now()), None).foreach(stopTime =>
    "mockRunner when (Initialised)" should "transition to Confirming Order and place the first instruction when Start is received " + (if (stopTime.isDefined) "and start stop timer" else "") in {
      val mockRunner = TestFSMRef(new Runner(mockController.ref))

      val strategy = mockStrategy.copy(stopTime = stopTime)

      mockRunner.setState(Initialised, strategy)

      mockRunner ! Start

      mockController.expectMsg(PlaceOrders(marketId, Set(instruction)))

      mockRunner.stateName should be (ConfirmingOrder)
      mockRunner.stateData should be (strategy)

      mockRunner.isTimerActive("StopTimer") should be (stopTime.isDefined)

      mockRunner.stop()
    }
  )

  "mockRunner when (ConfirmingOrder)" should "update marketBook and stay when MarketBookUpdate received" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))

    mockRunner.setState(ConfirmingOrder, mockStrategy)

    mockRunner ! mockMarketBook

    mockRunner.stateName should be (ConfirmingOrder)
    mockRunner.stateData should be (mockStrategy.copy(marketBook = Some(mockMarketBook)))

    mockController.expectNoMsg(200 millis)
    mockRunner.stop()
  }

  "mockRunner when (ConfirmingOrder)" should "place the last instruction when OrderManagerException is received" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))

    mockRunner.setState(ConfirmingOrder, mockStrategy)

    mockRunner ! OrderManagerException("TEST")

    mockController.expectMsg(PlaceOrders(marketId, Set(instruction)))

    mockRunner.stateName should be (ConfirmingOrder)
    mockRunner.stateData should be (mockStrategy)

    mockRunner.stop()
  }

  List(ExecutionReportStatus.TIMEOUT, ExecutionReportStatus.PROCESSED_WITH_ERRORS, ExecutionReportStatus.FAILURE).foreach(status =>
    "mockRunner when (ConfirmingOrder)" should "place the last instruction when PlaceExecutionReportContainer is received with status == " + status in {
      val mockRunner = TestFSMRef(new Runner(mockController.ref))

      mockRunner.setState(ConfirmingOrder, mockStrategy)

      mockRunner ! PlaceExecutionReportContainer(PlaceExecutionReport(status, "TEST_MARKET_ID", None, Set(), None))

      mockController.expectMsg(PlaceOrders(marketId, Set(instruction)))

      mockRunner.stateName should be (ConfirmingOrder)
      mockRunner.stateData should be (mockStrategy)

      mockRunner.stop()
    }
  )

  "mockRunner when (ConfirmingOrder)" should
    "transition to WaitingForOrder, unstash all messages, resend the last marketBook and update the strategy when PlaceExecutionReportContainer is received with status == SUCCESS" in {
    val mockUnstashAll = mockFunction[Unit]
    val mockResendMarketBook = mockFunction[Strategy, Unit]

    val mockRunner = TestFSMRef(new Runner(mockController.ref) {
      override def unstashAllMsg() = mockUnstashAll.apply()
      override def resendMarketBook(s: Strategy) = mockResendMarketBook.apply(s)
    })

    mockUnstashAll.expects()
    mockResendMarketBook.expects(mockStrategy)

    mockRunner.setState(ConfirmingOrder, mockStrategy)

    mockRunner ! PlaceExecutionReportContainer(
      PlaceExecutionReport(
        ExecutionReportStatus.SUCCESS,
        "TEST_MARKET_ID",
        None,
        Set(PlaceInstructionReport(
          status = InstructionReportStatus.SUCCESS,
          None,
          instruction = instruction,
          betId = Some(betId),
          None, None, None
        )),
        None
      )
    )

    // Strategy Updates:
    // - Pop the last instruction from the strategy
    // - Add a new order to the strategy with the received BetId
    val newStrategy = mockStrategy.copy(instructions = mockStrategy.instructions.tail, orders = List(OrderData(betId)))

    mockRunner.stateName should be (WaitingForOrder)
    mockRunner.stateData should be (newStrategy)

    mockController.expectNoMsg(200 millis)
    mockRunner.stop()
  }

  "mockRunner when (ConfirmingOrder)" should "stash all messages not listed above and stay" in {
    val mockStash = mockFunction[Unit]

    val mockRunner = TestFSMRef(new Runner(mockController.ref) {
      override def stashMsg() = mockStash.apply()
    })

    mockRunner.setState(ConfirmingOrder, mockStrategy)
    val testMessage = "TEST_MESSAGE"

    mockStash.expects()

    mockRunner ! testMessage

    mockRunner.stateName should be (ConfirmingOrder)
    mockRunner.stateData should be (mockStrategy)

    mockController.expectNoMsg(200 millis)
    mockRunner.stop()
  }

  List(OrderPlaced, OrderUpdated).foreach(msg =>
    "mockRunner when (WaitingForOrder)" should "update the strategy when " + msg + " is received with the same order Id" in {
      val mockRunner = TestFSMRef(new Runner(mockController.ref))

      val strategy = mockStrategy.copy(orders = List(OrderData(betId)))

      mockRunner.setState(WaitingForOrder, strategy)

      mockRunner ! msg.apply(marketId, selectionId, 0.0, mockCurrentOrderSummary)

      mockRunner.stateName should be (WaitingForOrder)
      mockRunner.stateData should be (strategy.copy(orders = List(OrderData(betId, Some(mockCurrentOrderSummary)))))

      mockController.expectNoMsg(200 millis)
      mockRunner.stop()
    }
  )

  "mockRunner when (WaitingForOrder)" should
    "update the strategy and transition to Finished when OrderExecuted is received with the same order Id and the strategy has no more instructions" in {
      val mockRunner = TestFSMRef(new Runner(mockController.ref))

      val strategy = mockStrategy.copy(orders = List(OrderData(betId)), instructions = List.empty)

      mockRunner.setState(WaitingForOrder, strategy)

      mockRunner ! OrderExecuted(marketId, selectionId, 0.0, mockCurrentOrderSummary)

      mockRunner.stateName should be (Finished)
      mockRunner.stateData should be (strategy.copy(orders = List(OrderData(betId, Some(mockCurrentOrderSummary)))))

      mockController.expectNoMsg(200 millis)
      mockRunner.stop()
  }

  "mockRunner when (WaitingForOrder)" should
    "update the strategy, place the next instruction and transition to ConfirmingOrder when OrderExecuted is received with the same order Id" in {
      val mockRunner = TestFSMRef(new Runner(mockController.ref))

      val strategy = mockStrategy.copy(orders = List(OrderData(betId)))

      mockRunner.setState(WaitingForOrder, strategy)

      mockRunner ! OrderExecuted(marketId, selectionId, 0.0, mockCurrentOrderSummary)

      mockController.expectMsg(PlaceOrders(marketId, Set(strategy.instructions.head)))

      mockRunner.stateName should be (ConfirmingOrder)
      mockRunner.stateData should be (strategy.copy(orders = List(OrderData(betId, Some(mockCurrentOrderSummary)))))

      mockRunner.stop()
  }


  List(OrderPlaced, OrderUpdated, OrderExecuted).foreach(msg =>
    "mockRunner when (WaitingForOrder)" should "NOT update the strategy when " + msg + " is received with a different order Id" in {
      val mockRunner = TestFSMRef(new Runner(mockController.ref))

      val strategy = mockStrategy.copy(orders = List(OrderData("DIFFERENT_BET_ID")))

      mockRunner.setState(WaitingForOrder, strategy)

      mockRunner ! msg.apply(marketId, selectionId, 0.0, mockCurrentOrderSummary)

      mockRunner.stateName should be (WaitingForOrder)
      mockRunner.stateData should be (strategy)

      mockController.expectNoMsg(200 millis)
      mockRunner.stop()
    }
  )

  "mockRunner when (WaitingForOrder)" should "cancel Orders, cancel StopTimer and transition to CancellingOrder when STOP is received" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))

    val strategy = mockStrategy.copy(orders = List(OrderData(betId)))

    mockRunner.setState(WaitingForOrder, strategy)
    mockRunner.setTimer("StopTimer", "TEST", 10 minutes)

    mockRunner ! Stop

    mockController.expectMsg(CancelOrders(marketId, Set(CancelInstruction(strategy.orders.head.id))))
    mockRunner.isTimerActive("StopTimer") should be (false)

    mockRunner.stateName should be (CancellingOrder)
    mockRunner.stateData should be (strategy)

    mockController.expectNoMsg(200 millis)
    mockRunner.stop()
  }

  "mockRunner when (WaitingForOrder)" should
    "cancel Orders, cancel StopTimer, update the strategy and transition to CancellingOrder when MarketBookUpdate is received and Stop is Triggered" in {
      val _isStopTriggered = mockFunction[Strategy, MarketBook, Boolean]

      val mockRunner = TestFSMRef(new Runner(mockController.ref) {
        override def isStopTriggered(s: Strategy, m: MarketBook) = _isStopTriggered.apply(s ,m)
      })

      val strategy = mockStrategy.copy(orders = List(OrderData(betId)))

      mockRunner.setState(WaitingForOrder, strategy)
      mockRunner.setTimer("StopTimer", "TEST", 10 minutes)

      _isStopTriggered.expects(strategy, mockMarketBook.data).returns(true)

      mockRunner ! mockMarketBook

      mockController.expectMsg(CancelOrders(marketId, Set(CancelInstruction(strategy.orders.head.id))))
      mockRunner.isTimerActive("StopTimer") should be (false)

      mockRunner.stateName should be (CancellingOrder)
      mockRunner.stateData should be (strategy.copy(marketBook = Some(mockMarketBook)))

      mockRunner.stop()
  }

  val waitingForOrder_MarketBookUpdate_Cases = Table(
    ("Stop Triggered", "Strategy.StoppingOut", "CancelOrders"),
    (true,             false,                  true),
    (false,            true,                   false),
    (false,            false,                  false),
    (true,             true,                   false)
  )

  forAll(waitingForOrder_MarketBookUpdate_Cases){(stopTriggered: Boolean, stoppingOut: Boolean, cancelOrders: Boolean) =>
    "mockRunner when (WaitingForOrder)" should
      (if (cancelOrders) "stay" else "cancel orders") + " update strategy when MarketBookUpdate received and isStopTriggered = " + stopTriggered + " or strategy.stoppingOut = " + stoppingOut in {
      val _isStopTriggered = mockFunction[Strategy, MarketBook, Boolean]

      val mockRunner = TestFSMRef(new Runner(mockController.ref) {
        override def isStopTriggered(s: Strategy, m: MarketBook) = _isStopTriggered.apply(s ,m)
      })

      val strategy = mockStrategy.copy(orders = List(OrderData(betId)), stoppingOut = stoppingOut)

      mockRunner.setState(WaitingForOrder, strategy)
      mockRunner.setTimer("StopTimer", "TEST", 10 minutes)

      if (!stoppingOut) {
        _isStopTriggered.expects(strategy, mockMarketBook.data).returns(stopTriggered)
      }

      mockRunner ! mockMarketBook

      if (cancelOrders) {
        mockRunner.isTimerActive("StopTimer") should be (false)

        mockRunner.stateName should be (CancellingOrder)
        mockController.expectMsg(CancelOrders(marketId, Set(CancelInstruction(betId))))
      } else {
        mockRunner.isTimerActive("StopTimer") should be (true)

        mockRunner.stateName should be (WaitingForOrder)
        mockRunner.stateData should be (strategy.copy(marketBook = Some(mockMarketBook)))
        mockController.expectNoMsg(200 millis)
      }

      mockRunner.stop()
    }
  }

  "mockRunner when (CancellingOrder)" should "stay when CancelExecutionReportContainer with status == SUCCESS received" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))

    mockRunner.setState(CancellingOrder, mockStrategy)

    mockRunner ! CancelExecutionReportContainer(CancelExecutionReport(ExecutionReportStatus.SUCCESS, marketId, None, Set(), None))

    mockRunner.stateName should be (CancellingOrder)
    mockRunner.stateData should be (mockStrategy)

    mockController.expectNoMsg(200 millis)
    mockRunner.stop()
  }

  "mockRunner when (CancellingOrder)" should "cancel the last order and stay when OrderManagerException is received" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))

    val strategy = mockStrategy.copy(orders = List(OrderData(betId)))

    mockRunner.setState(CancellingOrder, strategy)

    mockRunner ! OrderManagerException("TEST")

    mockController.expectMsg(CancelOrders(marketId, Set(CancelInstruction(betId))))

    mockRunner.stateName should be (CancellingOrder)
    mockRunner.stateData should be (strategy)

    mockRunner.stop()
  }

  List(ExecutionReportStatus.TIMEOUT, ExecutionReportStatus.PROCESSED_WITH_ERRORS, ExecutionReportStatus.FAILURE).foreach(status =>
    "mockRunner when (CancellingOrder)" should "cancel the last order and stay when CancelExecutionReportContainer with status == " + status + " received" in {
      val mockRunner = TestFSMRef(new Runner(mockController.ref))

      val strategy = mockStrategy.copy(orders = List(OrderData(betId)))

      mockRunner.setState(CancellingOrder, strategy)

      mockRunner ! CancelExecutionReportContainer(CancelExecutionReport(status, marketId, None, Set(), None))

      mockController.expectMsg(CancelOrders(marketId, Set(CancelInstruction(betId))))

      mockRunner.stateName should be (CancellingOrder)
      mockRunner.stateData should be (strategy)

      mockRunner.stop()
    }
  )

  "mockRunner when (CancellingOrder)" should "update marketBook and stay when MarketBookUpdate received" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))

    mockRunner.setState(CancellingOrder, mockStrategy)

    mockRunner ! mockMarketBook

    mockRunner.stateName should be (CancellingOrder)
    mockRunner.stateData should be (mockStrategy.copy(marketBook = Some(mockMarketBook)))

    mockController.expectNoMsg(200 millis)
    mockRunner.stop()
  }

  "mockRunner when (CancellingOrder)" should "transition to Finished and update strategy when OrderExecuted is received for the same BetId and there is NO stop instruction" in {
    val _getStopInstruction = mockFunction[Strategy, Option[PlaceInstruction]]

    val strategy = mockStrategy.copy(orders = List(OrderData(betId)))

    val mockRunner = TestFSMRef(new Runner(mockController.ref) {
      override def getStopInstruction(s: Strategy): Option[PlaceInstruction] = _getStopInstruction.apply(s)
    })

    _getStopInstruction.expects(strategy.copy(orders = List(OrderData(betId, Some(mockCurrentOrderSummary))))).returns(None)

    mockRunner.setState(CancellingOrder, strategy)

    mockRunner ! OrderExecuted(marketId, selectionId, 0.0, mockCurrentOrderSummary)

    mockRunner.stateName should be (Finished)
    mockRunner.stateData should be (strategy.copy(orders = List(OrderData(betId, Some(mockCurrentOrderSummary)))))

    mockController.expectNoMsg(200 millis)
    mockRunner.stop()
  }

  "mockRunner when (CancellingOrder)" should
    "place stop Instruction, update strategy and transition to ConfirmingOrder when OrderExecuted is received for the same BetId and there is a stop instruction" in {
    val _getStopInstruction = mockFunction[Strategy, Option[PlaceInstruction]]

    val strategy = mockStrategy.copy(orders = List(OrderData(betId)), instructions = List.empty)

    val mockRunner = TestFSMRef(new Runner(mockController.ref) {
      override def getStopInstruction(s: Strategy): Option[PlaceInstruction] = _getStopInstruction.apply(s)
    })

    _getStopInstruction.expects(strategy.copy(orders = List(OrderData(betId, Some(mockCurrentOrderSummary))))).returns(Some(instruction))

    mockRunner.setState(CancellingOrder, strategy)

    mockRunner ! OrderExecuted(marketId, selectionId, 0.0, mockCurrentOrderSummary)

    mockController.expectMsg(PlaceOrders(marketId, Set(instruction)))

    mockRunner.stateName should be (ConfirmingOrder)
    mockRunner.stateData should be (strategy.copy(orders = List(OrderData(betId, Some(mockCurrentOrderSummary))), instructions = List(instruction)))

    mockRunner.stop()
  }

  "mockRunner when (CancellingOrder)" should "stay when OrderExecuted is received for a different BetId" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))

    val strategy = mockStrategy.copy(orders = List(OrderData("DIFFERENT_BET_ID")))

    mockRunner.setState(CancellingOrder, strategy)

    mockRunner ! OrderExecuted(marketId, selectionId, 0.0, mockCurrentOrderSummary)

    mockRunner.stateName should be (CancellingOrder)
    mockRunner.stateData should be (strategy)

    mockController.expectNoMsg(200 millis)
    mockRunner.stop()
  }

  val getStopInstruction_Cases = Table(
    ("position", "ExpectedOutput"),
    (10.0,       Some(PlaceInstruction(OrderType.LIMIT, selectionId, 0.0, Side.BACK, Some(LimitOrder(10, 1.01, PersistenceType.PERSIST))))),
    (-10.0,      Some(PlaceInstruction(OrderType.LIMIT, selectionId, 0.0, Side.LAY, Some(LimitOrder(10, 1000, PersistenceType.PERSIST))))),
    (0.0,        None)
  )

  forAll(getStopInstruction_Cases){(position: Double, expectedOutput: Option[PlaceInstruction]) =>
    "mockRunner.getStopInstruction" should "return " + expectedOutput + " if position = " + position in {
      val _getPosition = mockFunction[List[OrderData], Double]

      val mockRunner = TestFSMRef(new Runner(mockController.ref) {
        override def getPosition(o: List[OrderData]) = _getPosition(o)
      })

      val strategy = mockStrategy.copy(orders = List(OrderData(betId)))

      _getPosition.expects(strategy.orders).returns(position)

      mockRunner.underlyingActor.getStopInstruction(strategy) should be (expectedOutput)
      mockRunner.stop()
    }
  }

  val getPosition_Cases = Table(
    ("orders",                                                                                            "position"),
    (List(
      OrderData("BET_ID_1", Some(mockCurrentOrderSummary.copy(side = Side.BACK, sizeMatched = 5))),
      OrderData("BET_ID_2", Some(mockCurrentOrderSummary.copy(side = Side.LAY, sizeMatched = 2))),
      OrderData("BET_ID_3", None)),                                                                       -3.0),
    (List(
      OrderData("BET_ID_1", Some(mockCurrentOrderSummary.copy(side = Side.BACK, sizeMatched = 2))),
      OrderData("BET_ID_2", Some(mockCurrentOrderSummary.copy(side = Side.LAY, sizeMatched = 5))),
      OrderData("BET_ID_3", None)),                                                                       3.0),
    (List(
      OrderData("BET_ID_1", Some(mockCurrentOrderSummary.copy(side = Side.BACK, sizeMatched = 5))),
      OrderData("BET_ID_2", Some(mockCurrentOrderSummary.copy(side = Side.LAY, sizeMatched = 5))),
      OrderData("BET_ID_3", None)),                                                                       0.0)
  )

  "mockRunner.getPosition" should "return a pos number if Long (More lay sizeMatched), neg if short (More back sizeMatched) and 0 if flat" in {
    forAll(getPosition_Cases){(orders: List[OrderData], position: Double) =>
      val mockRunner = TestFSMRef(new Runner(mockController.ref))

      mockRunner.underlyingActor.getPosition(orders) should be (position)
      mockRunner.stop()
    }
  }

  val isStopTriggered_Cases = Table(
    ("stopSide", "stopPrice", "layPrice", "backPrice", "expectedOutput"),
    (Side.BACK,  10.0,        Some(5.0),  Some(15.0),  false),
    (Side.BACK,  10.0,        Some(5.0),  Some(10.0),  true),
    (Side.BACK,  10.0,        Some(5.0),  Some(9.0),   true),
    (Side.BACK,  10.0,        None,       Some(9.0),   false),
    (Side.BACK,  10.0,        Some(5.0),  None,        false),

    (Side.LAY,   10.0,        Some(5.0),  Some(20.0),  false),
    (Side.LAY,   10.0,        Some(10.0), Some(20.0),  true),
    (Side.LAY,   10.0,        Some(15.0), Some(20.0),  true),
    (Side.LAY,   10.0,        None,       Some(20.0),  false),
    (Side.LAY,   10.0,        None,       None,        false)

  )

  forAll(isStopTriggered_Cases) { (stopSide: Side, stopPrice: Double, layPrice: Option[Double], backPrice: Option[Double], expectedOutput: Boolean) =>
    "mockRunner.isStopTriggered" should
      "return " + expectedOutput + " if stopSide = " + stopSide + " stopPrice = " + stopPrice + " layPrice" + layPrice + " backPrice = " + backPrice in {
        val mockRunner = TestFSMRef(new Runner(mockController.ref))

        val strategy = mockStrategy.copy(stopSide = stopSide, stopPrice = stopPrice)

        class TestRunner(_backPrice: Option[Double], _layPrice: Option[Double]) extends domain.Runner(selectionId, 0.0, "") {
          override lazy val backPrice: Option[Double] = _backPrice
          override lazy val layPrice: Option[Double] = _layPrice
        }

        class TestMarketBook extends MarketBook(marketId, false, "", 0, false, false, false, 0, 0, 0, None, 0, 0, false, false, 0, Set()) {
          override def getRunner(s: Long): domain.Runner = new TestRunner(backPrice, layPrice)
        }

        val marketBook = new TestMarketBook

        mockRunner.underlyingActor.isStopTriggered(strategy, marketBook)
        mockRunner.stop()
    }
  }

  "mockRunner" should "cancel StartTimer & StopTimer when transitioning to Finished" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))

    mockRunner.setTimer("StartTimer", "", 10 seconds)
    mockRunner.setTimer("StopTimer", "", 10 seconds)

    mockRunner.setState(Finished, NoData)

    mockRunner.isTimerActive("StartTimer") should be (false)
    mockRunner.isTimerActive("StopTimer") should be (false)
  }
}

