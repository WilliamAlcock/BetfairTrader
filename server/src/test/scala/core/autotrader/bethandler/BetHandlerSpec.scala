package core.autotrader.bethandler

import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import core.api.commands._
import core.autotrader.betHandler.BetHandler
import core.autotrader.betHandler.BetHandler._
import core.dataProvider.polling.BEST
import core.orderManager.OrderManagerException
import domain._
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._


class BetHandlerSpec extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(""))) with FlatSpecLike with Matchers with MockFactory with BeforeAndAfterEach with BeforeAndAfterAll {

  var mockController, subscriber, exchange: TestProbe = _

  override def beforeEach: Unit = {
    mockController = TestProbe()
    subscriber = TestProbe()
    exchange = TestProbe()
  }

  val marketId = "TEST_MARKET_ID"
  val selectionId = 1L
  val betId = "TEST_BET_ID"
  val instruction = PlaceInstruction(OrderType.LIMIT, selectionId, 0.0, Side.BACK)


  "BetHandler" should "start in Idle state with noData" in {
    val betHandler = TestFSMRef(new BetHandler(mockController.ref))
    betHandler.stateName should be (Idle)
    betHandler.stateData should be (NoData)
  }

  "BetHandler, When Idle" should "subscribe to the market and place the bet with the controller and goto Confirming when PlaceBet received" in {
    val betHandler = TestFSMRef(new BetHandler(mockController.ref))

    subscriber.send(betHandler, PlaceBet(marketId, instruction))
    mockController.expectMsg(SubscribeToMarkets(Set(marketId), BEST))
    mockController.expectMsg(PlaceOrders(marketId, Set(instruction)))
    betHandler.stateName should be (Confirming)
    betHandler.stateData should be (BetInstruction(marketId, instruction, subscriber.ref))
  }

  val testCases_1 = Table(
    ("cancel flag", "newPrice", "final State"),
    (false,         None,       Executing),
    (true,          Some(10.0), Cancelling),
    (true,          None,       Cancelling),
    (false,         Some(10.0), Replacing)
  )

  forAll(testCases_1) {(cancel: Boolean, newPrice: Option[Double], finalState: State) =>
    "BetHandler, When Confirming" should "goto " + finalState + " when the order is confirmed as SUCCESS, cancel = " + cancel + ", newPrice = " + newPrice in {
      val container = PlaceExecutionReportContainer(
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

      val betHandler = TestFSMRef(new BetHandler(mockController.ref))

      betHandler.setState(Confirming, BetInstruction(marketId, instruction, subscriber.ref, cancel = cancel, newPrice = newPrice))
      exchange.send(betHandler, container)
      subscriber.expectMsg(BetPlaced(marketId, selectionId, betId))
      if (cancel) {
        mockController.expectMsg(CancelOrders(marketId, Set(CancelInstruction(betId))))
      } else if (newPrice.isDefined) {
        mockController.expectMsg(ReplaceOrders(marketId, Set(ReplaceInstruction(betId, newPrice.get))))
      }
      betHandler.stateName should be (finalState)
      betHandler.stateData should be (Bet(marketId, selectionId, betId, subscriber = subscriber.ref, newPrice = if(!cancel && newPrice.isDefined) newPrice else None))
    }
  }

  val notSuccess = List(ExecutionReportStatus.TIMEOUT, ExecutionReportStatus.PROCESSED_WITH_ERRORS, ExecutionReportStatus.FAILURE)

  "BetHandler, When Confirming" should "goto Idle and UnSubscribe from the market when the order is not SUCCESS" in {
    notSuccess.foreach({reportStatus =>
      val betHandler = TestFSMRef(new BetHandler(mockController.ref))

      val container = PlaceExecutionReportContainer(
        PlaceExecutionReport(
          reportStatus,
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

      betHandler.setState(Confirming, BetInstruction(marketId, instruction, subscriber.ref))
      exchange.send(betHandler, container)
      subscriber.expectMsg(BetFailed(marketId, instruction))
      mockController.expectMsg(UnSubscribeFromMarkets(Set(marketId), BEST))
      betHandler.stateName should be (Idle)
      betHandler.stateData should be (BetInstruction(marketId, instruction, subscriber = subscriber.ref))
    })
  }

  "BetHandler, When Confirming" should "goto Idle and UnSubscribe from the market when an OrderManagerException is received" in {
    val betHandler = TestFSMRef(new BetHandler(mockController.ref))

    betHandler.setState(Confirming, BetInstruction(marketId, instruction, subscriber.ref))
    exchange.send(betHandler, OrderManagerException("TEST_EXCEPTION"))
    subscriber.expectMsg(BetFailed(marketId, instruction))
    mockController.expectMsg(UnSubscribeFromMarkets(Set(marketId), BEST))
    betHandler.stateName should be (Idle)
    betHandler.stateData should be (BetInstruction(marketId, instruction, subscriber = subscriber.ref))
  }

  "BetHandler, When Confirming" should "mark cancel flag == true when CancelBet is received" in {
    val betHandler = TestFSMRef(new BetHandler(mockController.ref))

    betHandler.setState(Confirming, BetInstruction(marketId, instruction, subscriber.ref))
    subscriber.send(betHandler, CancelBet)
    betHandler.stateName should be (Confirming)
    betHandler.stateData should be (BetInstruction(marketId, instruction, subscriber.ref, cancel = true))

  }

  "BetHandler, When Confirming" should "mark set newPrice field when ReplaceBet is received" in {
    val betHandler = TestFSMRef(new BetHandler(mockController.ref))

    val newPrice = 10.0

    betHandler.setState(Confirming, BetInstruction(marketId, instruction, subscriber.ref))
    subscriber.send(betHandler, ReplaceBet(newPrice))
    betHandler.stateName should be (Confirming)
    betHandler.stateData should be (BetInstruction(marketId, instruction, subscriber.ref, newPrice = Some(newPrice)))
  }

  "BetHandler, When Executing" should "goto Cancelling when CancelBet is received" in {
    val betHandler = TestFSMRef(new BetHandler(mockController.ref))

    betHandler.setState(Executing, Bet(marketId, selectionId, betId, subscriber = subscriber.ref))
    subscriber.send(betHandler, CancelBet)
    mockController.expectMsg(CancelOrders(marketId, Set(CancelInstruction(betId))))
    betHandler.stateName should be (Cancelling)
    betHandler.stateData should be (Bet(marketId, selectionId, betId, subscriber = subscriber.ref))
  }

  "BetHandler, When Executing" should "goto Replacing when ReplaceBet is received" in {
    val betHandler = TestFSMRef(new BetHandler(mockController.ref))

    val newPrice = 10.0

    betHandler.setState(Executing, Bet(marketId, selectionId, betId, subscriber = subscriber.ref))
    subscriber.send(betHandler, ReplaceBet(newPrice))
    mockController.expectMsg(ReplaceOrders(marketId, Set(ReplaceInstruction(betId, newPrice))))
    betHandler.stateName should be (Replacing)
    betHandler.stateData should be (Bet(marketId, selectionId, betId, subscriber = subscriber.ref, newPrice = Some(10.0)))
  }

  "BetHandler, When Executing" should "goto Idle when MarketBookUpdate is received and order.status == EXECUTION.COMPLETE" in {
    val betHandler = TestFSMRef(new BetHandler(mockController.ref))

    val testOrder = Order(betId, OrderType.LIMIT, OrderStatus.EXECUTION_COMPLETE, PersistenceType.PERSIST, Side.BACK, 0, 0, 0, DateTime.now(), 0, 0, 0, 0, 0, 0)

    // Mock MarketBook and override getOrder so it returns testOrder
    class testMarketBook extends MarketBook(
      marketId,
      isMarketDataDelayed = false,
      status = "",
      betDelay = 0,
      bspReconciled = false,
      complete = false,
      inplay = false,
      numberOfWinners = 0,
      numberOfRunners = 0,
      numberOfActiveRunners = 0,
      lastMatchTime = None,
      totalMatched = 0,
      totalAvailable = 0,
      crossMatching = false,
      runnersVoidable = false,
      version = 0L,
      runners = Set()) {
      override def getOrder(selectionId: Long, betId: String): Option[Order] = Some(testOrder)
    }

    betHandler.setState(Executing, Bet(marketId, selectionId, betId, subscriber = subscriber.ref))
    exchange.send(betHandler, MarketBookUpdate(DateTime.now(), new testMarketBook()))
    subscriber.expectMsg(BetExecuted(marketId, selectionId, testOrder))
    mockController.expectMsg(UnSubscribeFromMarkets(Set(marketId), BEST))
    betHandler.stateName should be (Idle)
    betHandler.stateData should be (Bet(marketId, selectionId, betId, order = Some(testOrder), subscriber = subscriber.ref))
  }

  "Bet Handler, when Executing" should "stay and alert the user when the order has changed" in {
    val betHandler = TestFSMRef(new BetHandler(mockController.ref))

    val initialOrder = Order(betId, OrderType.LIMIT, OrderStatus.EXECUTABLE, PersistenceType.PERSIST, Side.BACK, 0, 0, 0, DateTime.now(), 0, 0, 0, 0, 0, 0)
    val testOrder = initialOrder.copy(sizeMatched = 10)

    // Mock MarketBook and override getOrder so it returns testOrder
    class testMarketBook extends MarketBook(
      marketId,
      isMarketDataDelayed = false,
      status = "",
      betDelay = 0,
      bspReconciled = false,
      complete = false,
      inplay = false,
      numberOfWinners = 0,
      numberOfRunners = 0,
      numberOfActiveRunners = 0,
      lastMatchTime = None,
      totalMatched = 0,
      totalAvailable = 0,
      crossMatching = false,
      runnersVoidable = false,
      version = 0L,
      runners = Set()) {
      override def getOrder(selectionId: Long, betId: String): Option[Order] = Some(testOrder)
    }

    betHandler.setState(Executing, Bet(marketId, selectionId, betId, order = Some(initialOrder), subscriber = subscriber.ref))
    exchange.send(betHandler, MarketBookUpdate(DateTime.now(), new testMarketBook()))
    subscriber.expectMsg(BetUpdated(marketId, selectionId, testOrder))
    betHandler.stateName should be (Executing)
    betHandler.stateData should be (Bet(marketId, selectionId, betId, order = Some(testOrder), subscriber = subscriber.ref))
  }

  List(
    ExecutionReportStatus.SUCCESS,
    ExecutionReportStatus.FAILURE,
    ExecutionReportStatus.PROCESSED_WITH_ERRORS,
    ExecutionReportStatus.TIMEOUT
  ).foreach({status =>
    "Bet Handler, when Cancelling" should "notify subscriber and goto Executing when Cancel Order is confirmed as " + status in {
      val container = CancelExecutionReportContainer(CancelExecutionReport(status, marketId, None, Set(), None))

      val betHandler = TestFSMRef(new BetHandler(mockController.ref))

      betHandler.setState(Cancelling, Bet(marketId, selectionId, betId, subscriber = subscriber.ref))
      exchange.send(betHandler, container)
      if (status == ExecutionReportStatus.SUCCESS) {
        subscriber.expectMsg(BetCancelled(marketId, selectionId, betId))
      } else {
        subscriber.expectMsg(CancelFailed(marketId, selectionId, betId))
      }
      betHandler.stateName should be (Executing)
      betHandler.stateData should be (Bet(marketId, selectionId, betId, subscriber = subscriber.ref))
    }
  })

  "Bet Handler, when Cancelling" should "notify subscriber and goto Executing when an OrderManagerException is received" in {
    val betHandler = TestFSMRef(new BetHandler(mockController.ref))

    betHandler.setState(Cancelling, Bet(marketId, selectionId, betId, subscriber = subscriber.ref))
    exchange.send(betHandler, OrderManagerException("TEST_EXCEPTION"))
    subscriber.expectMsg(CancelFailed(marketId, selectionId, betId))
    betHandler.stateName should be (Executing)
    betHandler.stateData should be (Bet(marketId, selectionId, betId, subscriber = subscriber.ref))
  }

  List(
    ExecutionReportStatus.SUCCESS,
    ExecutionReportStatus.FAILURE,
    ExecutionReportStatus.PROCESSED_WITH_ERRORS,
    ExecutionReportStatus.TIMEOUT
  ).foreach({status =>
    "Bet Handler, when Replacing" should "notify subscriber and goto Executing when Replace Order is confirmed as " + status in {
      val newPrice = 10
      val newId = "TEST_NEW_BET_ID"

      val container = ReplaceExecutionReportContainer(
        ReplaceExecutionReport(None, status, None, marketId,
          Set(ReplaceInstructionReport(
            InstructionReportStatus.SUCCESS,
            None,
            None,
            Some(PlaceInstructionReport(InstructionReportStatus.SUCCESS, None, instruction, Some(newId), None, None, None))
          )
        ))
      )

      val betHandler = TestFSMRef(new BetHandler(mockController.ref))

      betHandler.setState(Replacing, Bet(marketId, selectionId, betId, subscriber = subscriber.ref, newPrice = Some(newPrice)))
      exchange.send(betHandler, container)
      if (status == ExecutionReportStatus.SUCCESS) {
        subscriber.expectMsg(BetReplaced(marketId, selectionId, betId, newPrice, newId))
        betHandler.stateData should be (Bet(marketId, selectionId, newId, subscriber = subscriber.ref))
      } else {
        subscriber.expectMsg(ReplaceFailed(marketId, selectionId, betId, newPrice))
        betHandler.stateData should be (Bet(marketId, selectionId, betId, subscriber = subscriber.ref))
      }
      betHandler.stateName should be (Executing)
    }
  })

  "Bet Handler, when Replacing" should "notify subscriber and goto Executing when an OrderManagerException is received" in {
    val newPrice = 10
    val betHandler = TestFSMRef(new BetHandler(mockController.ref))

    betHandler.setState(Replacing, Bet(marketId, selectionId, betId, subscriber = subscriber.ref, newPrice = Some(newPrice)))
    exchange.send(betHandler, OrderManagerException("TEST_EXCEPTION"))
    subscriber.expectMsg(ReplaceFailed(marketId, selectionId, betId, newPrice))
    betHandler.stateName should be (Executing)
    betHandler.stateData should be (Bet(marketId, selectionId, betId, subscriber = subscriber.ref))
  }
}