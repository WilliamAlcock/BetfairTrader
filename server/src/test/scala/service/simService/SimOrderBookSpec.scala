package service.simService

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import domain._
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
    (returnedMarketOrderBook1.updateMarketBook _).expects(marketBook1).returns(returnedMarketBook1)

    (marketOrderBook2.matchOrders _).expects(marketBook2).returns(returnedMarketOrderBook2)
    (returnedMarketOrderBook2.updateMarketBook _).expects(marketBook2).returns(returnedMarketBook2)

    val expectedResult = Some(ListMarketBookContainer(List(returnedMarketBook1, returnedMarketBook2, marketBook4)))

    val expectedMarkets = HashMap[String, MarketOrderBook](
      "1" -> returnedMarketOrderBook1,
      "2" -> returnedMarketOrderBook2,
      "3" -> marketOrderBook3
    )

    within(1 second) {
      simOrderBook ! MatchOrders(listMarketBookContainer)
      expectMsg(expectedResult)
      simOrderBook.underlyingActor.markets should be (expectedMarkets)
    }
  }
}

