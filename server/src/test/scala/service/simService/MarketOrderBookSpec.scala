package service.simService

import domain.OrderProjection.OrderProjection
import domain._
import org.joda.time.DateTimeUtils
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
import service.simService.TestHelpers._

import scala.collection.immutable.HashMap

class MarketOrderBookSpec extends FlatSpec with Matchers with MockFactory with BeforeAndAfterEach with BeforeAndAfterAll {

  override def afterAll = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  // Mock out the current time
  DateTimeUtils.setCurrentMillisFixed(123456789)

  var runnerOrderBook1, runnerOrderBook2, runnerOrderBook3: RunnerOrderBook = _
  var mockReportFactory: ReportFactory = _
  var mockUtils: Utils = _

  override def beforeEach() = {
    runnerOrderBook1 = mock[MockRunnerBook]
    runnerOrderBook2 = mock[MockRunnerBook]
    runnerOrderBook3 = mock[MockRunnerBook]
    mockReportFactory = mock[MockReportFactory]
    mockUtils = mock[MockUtils]
  }

  "MarketOrderBook" should "call placeOrder in the correct RunnerOrderBook" in {
    val instruction = generatePlaceInstruction(Side.BACK, 2, 1)

    val returnedOrderBook = mock[MockRunnerBook]
    val mockPlaceInstructionReport = mock[MockPlaceInstructionReport]
    val placeOrderResponse = PlaceOrderResponse[RunnerOrderBook](returnedOrderBook, mockPlaceInstructionReport)

    (runnerOrderBook1.placeOrder _).expects(instruction).returns(placeOrderResponse)

    val marketOrderBook = MarketOrderBook(HashMap(
      instruction.uniqueId -> runnerOrderBook1,
      "2" -> runnerOrderBook2,
      "3" -> runnerOrderBook3
    ), reportFactory = mockReportFactory, utils = mockUtils)

    val output = marketOrderBook.placeOrder(instruction)

    output.result should be (marketOrderBook.copy(runners = marketOrderBook.runners + (instruction.uniqueId -> returnedOrderBook)))
    output.report should be (mockPlaceInstructionReport)
  }

  "MarketOrderBook" should "call cancelOrder on the runnerBook with the betId" in {
    val instruction = CancelInstruction("TestBetId", None)

    val returnedOrderBook = mock[MockRunnerBook]
    val mockCancelInstructionReport = mock[MockCancelInstructionReport]
    val returnedResponse = CancelOrderResponse[RunnerOrderBook](returnedOrderBook, mockCancelInstructionReport)

    (runnerOrderBook1.hasBetId _).expects("TestBetId").returns(false)
    (runnerOrderBook2.hasBetId _).expects("TestBetId").returns(true)

    (runnerOrderBook2.cancelOrder _).expects(instruction).returns(returnedResponse)

    val marketOrderBook = MarketOrderBook(HashMap(
      "1" -> runnerOrderBook1,
      "2" -> runnerOrderBook2,
      "3" -> runnerOrderBook3
    ), reportFactory = mockReportFactory, utils = mockUtils)

    val output = marketOrderBook.cancelOrder(instruction)

    output.result should be (marketOrderBook.copy(runners = marketOrderBook.runners + ("2" -> returnedOrderBook)))
    output.report should be (mockCancelInstructionReport)
  }

  "MarketOrderBook" should "return a failed CancelInstructionReport if the betId is not found" in {
    val instruction = CancelInstruction("TestBetId", None)

    val instructionReport = mock[MockCancelInstructionReport]

    (runnerOrderBook1.hasBetId _).expects("TestBetId").returns(false)
    (runnerOrderBook2.hasBetId _).expects("TestBetId").returns(false)
    (runnerOrderBook3.hasBetId _).expects("TestBetId").returns(false)

    (mockReportFactory.getCancelInstructionReport _)
      .expects(InstructionReportStatus.FAILURE, Some(InstructionReportErrorCode.INVALID_BET_ID), instruction, None)
      .returns(instructionReport)

    val marketOrderBook = MarketOrderBook(HashMap(
      "1" -> runnerOrderBook1,
      "2" -> runnerOrderBook2,
      "3" -> runnerOrderBook3
    ), reportFactory = mockReportFactory, utils = mockUtils)

    val output = marketOrderBook.cancelOrder(instruction)

    output.result should be (marketOrderBook)
    output.report should be (instructionReport)
  }

  "MarketOrderBook" should "call updateOrder on the runnerBook with the betId" in {
    val instruction = UpdateInstruction("TestBetId", lapse)

    val returnedOrderBook = mock[MockRunnerBook]
    val mockUpdateInstructionReport = mock[MockUpdateInstructionReport]
    val cancelOrderResponse = UpdateOrderResponse[RunnerOrderBook](returnedOrderBook, mockUpdateInstructionReport)

    (runnerOrderBook1.hasBetId _).expects("TestBetId").returns(false)
    (runnerOrderBook2.hasBetId _).expects("TestBetId").returns(true)

    (runnerOrderBook2.updateOrder _).expects(instruction).returns(cancelOrderResponse)

    val marketOrderBook = MarketOrderBook(HashMap(
      "1" -> runnerOrderBook1,
      "2" -> runnerOrderBook2,
      "3" -> runnerOrderBook3
    ), reportFactory = mockReportFactory, utils = mockUtils)

    val output = marketOrderBook.updateOrder(instruction)

    output.result should be (marketOrderBook.copy(runners = marketOrderBook.runners + ("2" -> returnedOrderBook)))
    output.report should be (mockUpdateInstructionReport)
  }

  "MarketOrderBook" should "return a failed UpdateInstructionReport if the betId is not found when updateOrder is called" in {
    val instruction = UpdateInstruction("TestBetId", lapse)

    val instructionReport = mock[MockUpdateInstructionReport]

    (runnerOrderBook1.hasBetId _).expects("TestBetId").returns(false)
    (runnerOrderBook2.hasBetId _).expects("TestBetId").returns(false)
    (runnerOrderBook3.hasBetId _).expects("TestBetId").returns(false)

    (mockReportFactory.getUpdateInstructionReport _)
      .expects(InstructionReportStatus.FAILURE, Some(InstructionReportErrorCode.INVALID_BET_ID), instruction)
      .returns(instructionReport)

    val marketOrderBook = MarketOrderBook(HashMap(
      "1" -> runnerOrderBook1,
      "2" -> runnerOrderBook2,
      "3" -> runnerOrderBook3
    ), reportFactory = mockReportFactory, utils = mockUtils)

    val output = marketOrderBook.updateOrder(instruction)

    output.result should be (marketOrderBook)
    output.report should be (instructionReport)
  }

  "MarketOrderBook" should "matchOrders on each available RunnerOrderBook" in {
    val runner1 = Runner(1, 1, "TEST_STATUS", ex = Some(ExchangePrices(List(PriceSize(1, 1)), List(PriceSize(2, 2)), List(PriceSize(3, 3)))))
    val runner2 = Runner(2, 2, "TEST_STATUS", ex = Some(ExchangePrices(List(PriceSize(4, 4)), List(PriceSize(5, 5)), List(PriceSize(6, 6)))))
    val runner3 = Runner(3, 3, "TEST_STATUS", ex = Some(ExchangePrices(List(PriceSize(7, 7)), List(PriceSize(8, 8)), List(PriceSize(9, 9)))))
    val runner4 = Runner(4, 4, "TEST_STATUS", ex = Some(ExchangePrices(List(PriceSize(7, 7)), List(PriceSize(8, 8)), List(PriceSize(9, 9)))))     // This one does NOT exist
    val marketBook = MarketBook("TEST_MARKET", false, "TEST_STATUS", 0, false, false, false, 1, 1, 1, None, 0, 0, false, false, 1, Set(runner1, runner2, runner3, runner4))

    val returnedOrderBook1 = mock[MockRunnerBook]
    val returnedOrderBook2 = mock[MockRunnerBook]
    val returnedOrderBook3 = mock[MockRunnerBook]

    (runnerOrderBook1.matchOrders _).expects(1.0, 2.0).returns(returnedOrderBook1)
    (runnerOrderBook2.matchOrders _).expects(4.0, 5.0).returns(returnedOrderBook2)
    (runnerOrderBook3.matchOrders _).expects(7.0, 8.0).returns(returnedOrderBook3)

    val marketOrderBook = MarketOrderBook(HashMap(
      "1-1.0" -> runnerOrderBook1,
      "2-2.0" -> runnerOrderBook2,
      "3-3.0" -> runnerOrderBook3
    ), reportFactory = mockReportFactory, utils = mockUtils)

    val output = marketOrderBook.matchOrders(marketBook)

    output should be (marketOrderBook.copy(runners = marketOrderBook.runners + (
      "1-1.0" -> returnedOrderBook1,
      "2-2.0" -> returnedOrderBook2,
      "3-3.0" -> returnedOrderBook3)
    ))
  }

  "MarketOrderBook" should "match orders with the highest price availableToBack and lowest price availableToLay" in {
    val availableToBack = List(PriceSize(10, 1), PriceSize(20, 1), PriceSize(5, 1), PriceSize(15, 1))
    val availableToLay = List(PriceSize(60, 1), PriceSize(45, 1), PriceSize(25, 1), PriceSize(30, 1))
    val tradedVolume = List(PriceSize(3, 3))

    val runner1 = Runner(1, 1, "TEST_STATUS", ex = Some(ExchangePrices(availableToBack, availableToLay, tradedVolume)))
    val marketBook = MarketBook("TEST_MARKET", false, "TEST_STATUS", 0, false, false, false, 1, 1, 1, None, 0, 0, false, false, 1, Set(runner1))

    (runnerOrderBook1.matchOrders _).expects(20.0, 25.0)

    MarketOrderBook(HashMap(
      "1-1.0" -> runnerOrderBook1,
      "2-2.0" -> runnerOrderBook2,
      "3-3.0" -> runnerOrderBook3
    ), reportFactory = mockReportFactory, utils = mockUtils).matchOrders(marketBook)
  }

  "MarketOrderBook" should "updateMarketBook with the orders for orders from the correct RunnerOrderBook" in {
    class MockExchangePrices extends ExchangePrices(List.empty, List.empty, List.empty)

    val runner1 = Runner(1, 1, "TEST_STATUS", ex = Some(ExchangePrices(List(PriceSize(1 ,1)), List(PriceSize(2 ,2)), List(PriceSize(3 ,3)))))
    val runner2 = Runner(2, 2, "TEST_STATUS", ex = Some(ExchangePrices(List(PriceSize(4 ,4)), List(PriceSize(5 ,5)), List(PriceSize(6 ,6)))))
    val runner3 = Runner(3, 3, "TEST_STATUS", ex = Some(ExchangePrices(List(PriceSize(7 ,7)), List(PriceSize(8 ,8)), List(PriceSize(9 ,9)))))
    val runner4 = Runner(4, 4, "TEST_STATUS", ex = Some(ExchangePrices(List(PriceSize(10 ,10)), List(PriceSize(11 ,11)), List(PriceSize(12 ,12)))))     // This one does NOT exist in the MarketOrderBook
    val marketBook = MarketBook(
      "TEST_MARKET", false, "TEST_STATUS", 0, false, false, false, 1, 1, 1, None, 0, 0, false, false, 1, Set(runner1, runner2, runner3, runner4)
    )

    val mockOrder1 = generateOrder("1", 1,0, Side.BACK)
    val mockOrder2 = generateOrder("2", 1,0, Side.BACK)
    val mockOrder3 = generateOrder("3", 1,0, Side.BACK)

    val match1 = Match(None, None, Side.BACK, 1, 1, None)
    val match2 = Match(None, None, Side.BACK, 2, 2, None)
    val match3 = Match(None, None, Side.BACK, 3, 3, None)

    val exchangePrices1 = mock[MockExchangePrices]
    val exchangePrices2 = mock[MockExchangePrices]
    val exchangePrices3 = mock[MockExchangePrices]

    val expectedResult = marketBook.copy(runners = Set(
      runner1.copy(orders = Some(Set(mockOrder1)), matches = Some(Set(match1)), ex = Some(exchangePrices1)),
      runner2.copy(orders = Some(Set(mockOrder2)), matches = Some(Set(match2)), ex = Some(exchangePrices2)),
      runner3.copy(orders = Some(Set(mockOrder3)), matches = Some(Set(match3)), ex = Some(exchangePrices3)),
      runner4
    ))

    (runnerOrderBook1.getOrders _).expects().returns(Set(mockOrder1))
    (runnerOrderBook2.getOrders _).expects().returns(Set(mockOrder2))
    (runnerOrderBook3.getOrders _).expects().returns(Set(mockOrder3))

    (runnerOrderBook1.getMatches _).expects().returns(Set(match1))
    (runnerOrderBook2.getMatches _).expects().returns(Set(match2))
    (runnerOrderBook3.getMatches _).expects().returns(Set(match3))

    (mockUtils.updateExchangePrices _).expects(runner1.ex, Set[Order](mockOrder1)).returns(Some(exchangePrices1))
    (mockUtils.updateExchangePrices _).expects(runner2.ex, Set[Order](mockOrder2)).returns(Some(exchangePrices2))
    (mockUtils.updateExchangePrices _).expects(runner3.ex, Set[Order](mockOrder3)).returns(Some(exchangePrices3))

    val _filterOrdersByProjection = mockFunction[Set[Order], Option[OrderProjection], Set[Order]]

    _filterOrdersByProjection.expects(Set(mockOrder1), Some(OrderProjection.ALL)).returns(Set(mockOrder1))
    _filterOrdersByProjection.expects(Set(mockOrder2), Some(OrderProjection.ALL)).returns(Set(mockOrder2))
    _filterOrdersByProjection.expects(Set(mockOrder3), Some(OrderProjection.ALL)).returns(Set(mockOrder3))

    class TestMarketOrderBook extends MarketOrderBook(HashMap(
      "1-1.0" -> runnerOrderBook1,
      "2-2.0" -> runnerOrderBook2,
      "3-3.0" -> runnerOrderBook3
    ), reportFactory = mockReportFactory, utils = mockUtils) {
      override def filterOrdersByProjection(o: Set[Order], op: Option[OrderProjection]) = _filterOrdersByProjection.apply(o, op)
    }

    new TestMarketOrderBook().updateMarketBook(marketBook, Some(OrderProjection.ALL)) should be (expectedResult)
  }

  "MarketOrderBook" should "get orders should return the order or None if it does not exist" in {
    val mockOrder = generateOrder("TEST_ID", 1, 2, Side.BACK)

    val getOrderScenarios = Table(
      ("uniqueId", "betIdIsFound",  "expectedResult"),

      ("2",        true,            Some(mockOrder)),    // unique Id exists, betId is found
      ("2",        false,           None),               // unique Id exists, betId is not found
      ("4",        true,            None)                // unique Id does Not exist
    )

    forAll(getOrderScenarios) {(uniqueId: String, betIdIsFound: Boolean, expectedResult: Option[Order]) =>
      if (uniqueId == "2") {
        (runnerOrderBook2.getOrders _).expects().returns(if (betIdIsFound) Set(mockOrder) else Set())
      }

      val output = MarketOrderBook(HashMap(
        "1" -> runnerOrderBook1,
        "2" -> runnerOrderBook2,
        "3" -> runnerOrderBook3
      ), reportFactory = mockReportFactory, utils = mockUtils).getOrder(uniqueId, "TEST_ID")

      output should be (expectedResult)
    }
  }
}

