package service.simService

import akka.actor.{Props, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import domain.{OrderStatus, OrderType, PersistenceType, Side, _}
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import service.simService.SimOrderBook.{CancelOrders, MatchOrders, PlaceOrders, UpdateOrders}
import service.simService.TestHelpers._

import scala.collection.immutable.HashMap
import scala.concurrent.duration._
import scala.language.postfixOps

class SimOrderBookSpec extends TestKit(ActorSystem("testSystem")) with DefaultTimeout with ImplicitSender
  with FlatSpecLike with BeforeAndAfterAll with Matchers with MockFactory with BeforeAndAfterEach {

  override def afterAll {
    shutdown()
  }

  var marketOrderBook1, marketOrderBook2, marketOrderBook3: MarketOrderBook = _
  var markets:HashMap[String, MarketOrderBook] = _
  var mockReportFactory:ReportFactory = _
  var simOrderBook: TestActorRef[SimOrderBook] = _

  override def beforeEach: Unit = {
    marketOrderBook1 = mock[MarketOrderBook]
    marketOrderBook2 = mock[MarketOrderBook]
    marketOrderBook3 = mock[MarketOrderBook]

    markets = HashMap[String, MarketOrderBook](
      "1" -> marketOrderBook1,
      "2" -> marketOrderBook2,
      "3" -> marketOrderBook3
    )

    mockReportFactory = mock[MockReportFactory]
    simOrderBook = TestActorRef[SimOrderBook](SimOrderBook.props(markets, reportFactory = mockReportFactory))
  }

  "SimOrderBook" should "call place orders on the correct market" in {
    val marketId = "1"
    val instruction = generatePlaceInstruction(Side.BACK, 10.0, 20.0)
    val customerRef = Some("TEST")

    val returnedOrderBook = mock[MarketOrderBook]
    val mockPlaceInstructionReport = mock[MockPlaceInstructionReport]
    val placeOrderResponse = PlaceOrderResponse[MarketOrderBook](returnedOrderBook, mockPlaceInstructionReport)
    val mockPlaceExecutionReportContainer = mock[MockPlaceExecutionReportContainer]

    (mockReportFactory.getPlaceExecutionReportContainer _)
      .expects(marketId, Set[PlaceInstructionReport](mockPlaceInstructionReport), customerRef)
      .returns(mockPlaceExecutionReportContainer)

    (marketOrderBook1.placeOrder _).expects(instruction).returns(placeOrderResponse)

    within(1 second) {
      simOrderBook ! PlaceOrders(marketId, Set(instruction), customerRef)
      expectMsg(mockPlaceExecutionReportContainer)
      simOrderBook.underlyingActor.markets(marketId) should be (returnedOrderBook)
    }

  }

  "SimOrderBook" should "call cancel orders on the correct market " in {
    val marketId = "1"
    val instruction = CancelInstruction("2", None)
    val customerRef = Some("Test")

    val returnedOrderBook = mock[MarketOrderBook]
    val mockCancelInstructionReport = mock[MockCancelInstructionReport]
    val cancelOrderResponse = CancelOrderResponse[MarketOrderBook](returnedOrderBook, mockCancelInstructionReport)
    val mockCancelExecutionReportContainer = mock[MockCancelExecutionReportContainer]

    (mockReportFactory.getCancelExecutionReportContainer _)
      .expects(marketId, Set[CancelInstructionReport](mockCancelInstructionReport), customerRef)
      .returns(mockCancelExecutionReportContainer)

    (marketOrderBook1.cancelOrder _).expects(instruction).returns(cancelOrderResponse)

    within(1 second) {
      simOrderBook ! CancelOrders(marketId, Set(instruction), customerRef)
      expectMsg(mockCancelExecutionReportContainer)
      simOrderBook.underlyingActor.markets(marketId) should be (returnedOrderBook)
    }
  }

  "SimOrderBook" should "call update orders on correct market" in {
    val marketId = "1"
    val instruction = UpdateInstruction("2", lapse)
    val customerRef = Some("Test")

    val returnedOrderBook = mock[MarketOrderBook]
    val mockUpdateInstructionReport = mock[MockUpdateInstructionReport]
    val updateOrderResponse = UpdateOrderResponse[MarketOrderBook](returnedOrderBook, mockUpdateInstructionReport)
    val mockUpdateExecutionReportContainer = mock[MockUpdateExecutionReportContainer]

    (mockReportFactory.getUpdateExecutionReportContainer _)
      .expects(marketId, Set[UpdateInstructionReport](mockUpdateInstructionReport), customerRef)
      .returns(mockUpdateExecutionReportContainer)

    (marketOrderBook1.updateOrder _).expects(instruction).returns(updateOrderResponse)

    within(1 second) {
      simOrderBook ! UpdateOrders(marketId, Set(instruction), customerRef)
      expectMsg(mockUpdateExecutionReportContainer)
      simOrderBook.underlyingActor.markets(marketId) should be (returnedOrderBook)
    }
  }

// TODO add test for replace orders
//  "SimOrderBook" should "replace orders by cancelling an order and placing a new order" in {
//  }

  "SimOrderBook" should "match orders in the correct MarketOrderBook and return updated MarketBooks with orders added" in {
    val marketBook1 = MarketBook("1", false, "TEST_STATUS", 0, false, false, false, 1, 1, 1, None, 0, 0, false, false, 1, Set.empty)
    val marketBook2 = MarketBook("2", false, "TEST_STATUS", 0, false, false, false, 1, 1, 1, None, 0, 0, false, false, 1, Set.empty)
    val marketBook4 = MarketBook("4", false, "TEST_STATUS", 0, false, false, false, 1, 1, 1, None, 0, 0, false, false, 1, Set.empty)          // Does Not exist
    val listMarketBookContainer = ListMarketBookContainer(List(marketBook1, marketBook2, marketBook4))

    val returnedMarketBook1 = MarketBook("5", false, "TEST_STATUS", 0, false, false, false, 1, 1, 1, None, 0, 0, false, false, 1, Set.empty)
    val returnedMarketBook2 = MarketBook("6", false, "TEST_STATUS", 0, false, false, false, 1, 1, 1, None, 0, 0, false, false, 1, Set.empty)

    val returnedMarketOrderBook1 = mock[MarketOrderBook]
    val returnedMarketOrderBook2 = mock[MarketOrderBook]

    (marketOrderBook1.matchOrders _).expects(marketBook1).returns(returnedMarketOrderBook1)
    (returnedMarketOrderBook1.updateMarketBook _).expects(marketBook1, OrderProjection.ALL).returns(returnedMarketBook1)

    (marketOrderBook2.matchOrders _).expects(marketBook2).returns(returnedMarketOrderBook2)
    (returnedMarketOrderBook2.updateMarketBook _).expects(marketBook2, OrderProjection.ALL).returns(returnedMarketBook2)

    val expectedResult = Some(ListMarketBookContainer(List(returnedMarketBook1, returnedMarketBook2, marketBook4)))

    val expectedMarkets = HashMap[String, MarketOrderBook](
      "1" -> returnedMarketOrderBook1,
      "2" -> returnedMarketOrderBook2,
      "3" -> marketOrderBook3
    )

    within(1 second) {
      simOrderBook ! MatchOrders(listMarketBookContainer, OrderProjection.ALL)
      expectMsg(expectedResult)
      simOrderBook.underlyingActor.markets should be (expectedMarkets)
    }
  }

  "SimOrderBook.getCurrentOrderSummary" should "convert an order to currentOrderSummary" in {
    val marketId = "TEST_MARKET_ID"
    val uniqueId = "12345-123.45"
    val placedDate = DateTime.now()
    val order = Order(
      "TEST_BET_ID",
      OrderType.LIMIT,
      OrderStatus.EXECUTABLE,
      PersistenceType.LAPSE,
      Side.BACK,
      price = 10.0,
      size = 100.0,
      bspLiability = 1.0,
      placedDate = placedDate,
      avgPriceMatched = 3.0,
      sizeMatched = 4.0,
      sizeRemaining = 5.0,
      sizeLapsed = 6.0,
      sizeCancelled = 7.0,
      sizeVoided = 8.0
    )

    val expectedOutput = CurrentOrderSummary(
      "TEST_BET_ID",
      "TEST_MARKET_ID",
      12345L,
      123.45,
      PriceSize(10.0, 100.0),
      bspLiability = 1.0,
      Side.BACK,
      OrderStatus.EXECUTABLE,
      PersistenceType.LAPSE,
      OrderType.LIMIT,
      placedDate = placedDate,
      matchedDate = None,
      averagePriceMatched = 3.0,
      sizeMatched = 4.0,
      sizeRemaining = 5.0,
      sizeLapsed = 6.0,
      sizeCancelled = 7.0,
      sizeVoided = 8.0,
      regulatorCode = ""
    )

    simOrderBook.underlyingActor.getCurrentOrderSummary(marketId, uniqueId, order) should be (expectedOutput)
  }

  "SimOrderBook.getMarketOrders" should "return the market's orders as a set of currentOrderSummary" in {
    val marketId = "TEST_MARKET_ID"
    val uniqueId = "12345-123.56"

    val marketOrderBook = mock[MarketOrderBook]

    class TestOrder extends Order(
      "TEST_BET_ID",
      OrderType.LIMIT,
      OrderStatus.EXECUTABLE,
      PersistenceType.LAPSE,
      Side.BACK,
      price = 10.0,
      size = 100.0,
      bspLiability = 1.0,
      placedDate = DateTime.now(),
      avgPriceMatched = 3.0,
      sizeMatched = 4.0,
      sizeRemaining = 5.0,
      sizeLapsed = 6.0,
      sizeCancelled = 7.0,
      sizeVoided = 8.0
    )

    class TestCurrentOrderSummary extends CurrentOrderSummary(
      "TEST_BET_ID",
      "TEST_MARKET_ID",
      12345L,
      123.45,
      PriceSize(10.0, 100.0),
      bspLiability = 1.0,
      Side.BACK,
      OrderStatus.EXECUTABLE,
      PersistenceType.LAPSE,
      OrderType.LIMIT,
      placedDate = DateTime.now(),
      matchedDate = None,
      averagePriceMatched = 3.0,
      sizeMatched = 4.0,
      sizeRemaining = 5.0,
      sizeLapsed = 6.0,
      sizeCancelled = 7.0,
      sizeVoided = 8.0,
      regulatorCode = ""
    )

    val mockOrders = List.range(1,4).map(x => mock[TestOrder])
    val mockCurrentOrderSummary = List.range(1,4).map(x => mock[TestCurrentOrderSummary])

    val orders = Map[String, Set[Order]](
      "1-2" -> Set(mockOrders(0), mockOrders(1)),
      "2-3" -> Set(mockOrders(2), mockOrders(3))
    )

    (marketOrderBook.getOrders _).expects().returns(orders)

    val _getCurrentOrderSummary = mockFunction[String, String, Order, CurrentOrderSummary]

    simOrderBook = TestActorRef[SimOrderBook](Props(new SimOrderBook(markets, reportFactory = mockReportFactory) {
      override def getCurrentOrderSummary(m: String, u: String, o: Order) = _getCurrentOrderSummary.apply(m, u ,o)
    }))

    _getCurrentOrderSummary.expects(marketId, "1-2", mockOrders(0)).returns(mockCurrentOrderSummary(0))
    _getCurrentOrderSummary.expects(marketId, "1-2", mockOrders(1)).returns(mockCurrentOrderSummary(1))
    _getCurrentOrderSummary.expects(marketId, "1-2", mockOrders(2)).returns(mockCurrentOrderSummary(2))
    _getCurrentOrderSummary.expects(marketId, "1-2", mockOrders(3)).returns(mockCurrentOrderSummary(3))

    simOrderBook.underlyingActor.getMarketOrders(marketId, marketOrderBook)
  }

//  "SimOrderBook.getOrdersByMarket" should "call getMarketOrders for all the given marketIds" in {

    // no marketIds

    // no

//  }

//  "SimOrderBook." should "" in {

//  }
}

