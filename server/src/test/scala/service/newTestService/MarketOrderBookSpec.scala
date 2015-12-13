package service.newTestService

import domain.Side
import org.joda.time.DateTimeUtils
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
import service.newTestService.TestHelpers._

import scala.collection.immutable.HashMap

class MarketOrderBookSpec extends FlatSpec with Matchers with MockFactory with BeforeAndAfterEach with BeforeAndAfterAll {

  override def afterAll = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  // Mock out the current time
  DateTimeUtils.setCurrentMillisFixed(123456789)

  class MockRunnerBook extends RunnerOrderBook()

  var runnerOrderBook1, runnerOrderBook2, runnerOrderBook3: MockRunnerBook = _

  override def beforeEach() = {
    runnerOrderBook1 = mock[MockRunnerBook]
    runnerOrderBook2 = mock[MockRunnerBook]
    runnerOrderBook3 = mock[MockRunnerBook]
  }

  "MarketOrderBook" should "call place order in the correct RunnerOrderBook" in {
    val instruction = generatePlaceInstruction(Side.BACK, 2, 1)

    val returnedOrderBook = mock[MockRunnerBook]
    val returnedBackOrderBook = mock[MockRunnerOrderBook]
    val mockPlaceInstructionReport = mock[MockPlaceInstructionReport]
    val placeOrderResponse = PlaceOrderResponse[RunnerOrderBook](returnedBackOrderBook, mockPlaceInstructionReport)

    (runnerOrderBook2.placeOrder _).expects(instruction).returns(placeOrderResponse)

    val output = MarketOrderBook(HashMap(
      "1" -> runnerOrderBook1,
      "2-1" -> runnerOrderBook2,
      "3" -> runnerOrderBook3
    )).placeOrder(instruction)

//    output.result.should be (backOrderBook)
//    output.result.layOrderBook should be (returnedLayOrderBook)
//    output.report should be (mockPlaceInstructionReport)
  }

//  "MarketOrderBook" should "call cancel order in all RunnerOrderBooks" in {
//    val betId = "TestBetId"
//    val sizeReduction = 10.0
//
//    val returnedOrderBook1 = mock[MockRunnerBook]
//    val returnedOrderBook2 = mock[MockRunnerBook]
//    val returnedOrderBook3 = mock[MockRunnerBook]
//
//    (runnerOrderBook1.cancelOrder _).expects(betId, sizeReduction).returns(returnedOrderBook1)
//    (runnerOrderBook2.cancelOrder _).expects(betId, sizeReduction).returns(returnedOrderBook2)
//    (runnerOrderBook3.cancelOrder _).expects(betId, sizeReduction).returns(returnedOrderBook3)
//
//    val marketOrderBook = MarketOrderBook(HashMap(
//      "1" -> runnerOrderBook1,
//      "2" -> runnerOrderBook2,
//      "3" -> runnerOrderBook3
//    )).cancelOrder(betId, sizeReduction)
//
//    marketOrderBook.runners("1") should be(returnedOrderBook1)
//    marketOrderBook.runners("2") should be(returnedOrderBook2)
//    marketOrderBook.runners("3") should be(returnedOrderBook3)
//  }
//
//  "MarketOrderBook" should "call replace order in all RunnerOrderBooks" in {
//    val betId = "TestBetId"
//    val newPrice = 10.0
//
//    val returnedOrderBook1 = mock[MockRunnerBook]
//    val returnedOrderBook2 = mock[MockRunnerBook]
//    val returnedOrderBook3 = mock[MockRunnerBook]
//
//    (runnerOrderBook1.replaceOrder _).expects(betId, newPrice).returns(returnedOrderBook1)
//    (runnerOrderBook2.replaceOrder _).expects(betId, newPrice).returns(returnedOrderBook2)
//    (runnerOrderBook3.replaceOrder _).expects(betId, newPrice).returns(returnedOrderBook3)
//
//    val marketOrderBook = MarketOrderBook(HashMap(
//      "1" -> runnerOrderBook1,
//      "2" -> runnerOrderBook2,
//      "3" -> runnerOrderBook3
//    )).replaceOrder(betId, newPrice)
//
//    marketOrderBook.runners("1") should be(returnedOrderBook1)
//    marketOrderBook.runners("2") should be(returnedOrderBook2)
//    marketOrderBook.runners("3") should be(returnedOrderBook3)
//  }
//
//  "MarketOrderBook" should "call update order in all RunnerOrderBooks" in {
//    val betId = "TestBetId"
//    val newPersistenceType = PersistenceType.LAPSE
//
//    val returnedOrderBook1 = mock[MockRunnerBook]
//    val returnedOrderBook2 = mock[MockRunnerBook]
//    val returnedOrderBook3 = mock[MockRunnerBook]
//
//    (runnerOrderBook1.updateOrder _).expects(betId, newPersistenceType).returns(returnedOrderBook1)
//    (runnerOrderBook2.updateOrder _).expects(betId, newPersistenceType).returns(returnedOrderBook2)
//    (runnerOrderBook3.updateOrder _).expects(betId, newPersistenceType).returns(returnedOrderBook3)
//
//    val marketOrderBook = MarketOrderBook(HashMap(
//      "1" -> runnerOrderBook1,
//      "2" -> runnerOrderBook2,
//      "3" -> runnerOrderBook3
//    )).updateOrder(betId, newPersistenceType)
//
//    marketOrderBook.runners("1") should be(returnedOrderBook1)
//    marketOrderBook.runners("2") should be(returnedOrderBook2)
//    marketOrderBook.runners("3") should be(returnedOrderBook3)
//  }
//
//  "MarketOrderBook" should "match orders in the correct RunnerOrderBook" in {
//    val orders = List(Order(
//      "TestId",
//      OrderType.LIMIT,
//      OrderStatus.EXECUTABLE,
//      PersistenceType.LAPSE,
//      Side.BACK, 1, 2, 0, DateTime.now(), 0, 0, 0, 0, 0, 0
//    ))
//    val uniqueId = "2"
//
//    (runnerOrderBook2.getOrders _).expects().returns(orders)
//
//    val output = MarketOrderBook(HashMap(
//      "1" -> runnerOrderBook1,
//      "2" -> runnerOrderBook2,
//      "3" -> runnerOrderBook3
//    )).getOrders(uniqueId)
//
//    output should be (orders)
//  }
//
//  "MarketOrderBook" should "match orders should return an empty list if the order book does not exist" in {
//    val uniqueId = "4"
//
//    val output = MarketOrderBook(HashMap(
//      "1" -> runnerOrderBook1,
//      "2" -> runnerOrderBook2,
//      "3" -> runnerOrderBook3
//    )).getOrders(uniqueId)
//
//    output should be (List.empty[Order])
//  }


}

