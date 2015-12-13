package service.newTestService

import domain._
import org.joda.time.DateTimeUtils
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
import service.newTestService.TestHelpers._


class RunnerOrderBookSpec extends FlatSpec with Matchers with MockFactory with BeforeAndAfterEach with BeforeAndAfterAll {

  override def afterAll = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  // Mock out the current time
  DateTimeUtils.setCurrentMillisFixed(123456789)

  var backOrderBook: MockBackOrderBook = _
  var layOrderBook : MockLayOrderBook = _

  override def beforeEach() = {
    backOrderBook = mock[MockBackOrderBook]
    layOrderBook = mock[MockLayOrderBook]
  }

  "RunnerOrderBook" should "place BACK order in backOrderBook" in {
    val instruction = generatePlaceInstruction(Side.BACK, 1, 2)

    val returnedBackOrderBook = mock[MockBackOrderBook]
    val mockPlaceInstructionReport = mock[MockPlaceInstructionReport]
    val placeOrderResponse = PlaceOrderResponse[OrderBook](returnedBackOrderBook, mockPlaceInstructionReport)

    (backOrderBook.placeOrder _).expects(instruction).returns(placeOrderResponse)

    val output = RunnerOrderBook(backOrderBook, layOrderBook).placeOrder(instruction)

    output.result.backOrderBook should be (returnedBackOrderBook)
    output.result.layOrderBook should be (layOrderBook)
    output.report should be (mockPlaceInstructionReport)
  }

  "RunnerOrderBook" should "place LAY order in layOrderBook" in {
    val instruction = generatePlaceInstruction(Side.LAY, 1, 2)

    val returnedLayOrderBook = mock[MockLayOrderBook]
    val mockPlaceInstructionReport = mock[MockPlaceInstructionReport]
    val placeOrderResponse = PlaceOrderResponse[OrderBook](returnedLayOrderBook, mockPlaceInstructionReport)

    (layOrderBook.placeOrder _).expects(instruction).returns(placeOrderResponse)

    val output = RunnerOrderBook(backOrderBook, layOrderBook).placeOrder(instruction)

    output.result.backOrderBook should be (backOrderBook)
    output.result.layOrderBook should be (returnedLayOrderBook)
    output.report should be (mockPlaceInstructionReport)
  }

  val cancelScenarios = Table( ("backBookHasBetId"), (true), (false))

  "RunnerOrderBook" should "cancelOrder" in {
    forAll(cancelScenarios) { (backBookHasBetId: Boolean) =>

      val instruction = CancelInstruction("TestBetId", Some(10.0))

      val returnedBackOrderBook = mock[MockBackOrderBook]
      val backInstructionReport = mock[MockCancelInstructionReport]
      val backOrderResponse = CancelOrderResponse[OrderBook](returnedBackOrderBook, backInstructionReport)

      val returnedLayOrderBook = mock[MockLayOrderBook]
      val layInstructionReport = mock[MockCancelInstructionReport]
      val layOrderResponse = CancelOrderResponse[OrderBook](returnedLayOrderBook, layInstructionReport)

      (backOrderBook.hasBetId _).expects("TestBetId").returns(backBookHasBetId)

      if (backBookHasBetId)
        (backOrderBook.cancelOrder _).expects(instruction).returns(backOrderResponse)
      else
        (layOrderBook.cancelOrder _).expects(instruction).returns(layOrderResponse)

      val response = RunnerOrderBook(backOrderBook, layOrderBook).cancelOrder(instruction)

      if (backBookHasBetId) {
        response.result.backOrderBook should be (returnedBackOrderBook)
        response.result.layOrderBook should be (layOrderBook)
        response.report should be (backInstructionReport)
      } else {
        response.result.backOrderBook should be(backOrderBook)
        response.result.layOrderBook should be(returnedLayOrderBook)
        response.report should be(layInstructionReport)
      }
    }
  }

  val updateScenarios = Table( ("backBookHasBetId"), (true), (false))

  "RunnerOrderBook" should "updateOrder" in {
    forAll(updateScenarios) { (backBookHasBetId: Boolean) =>

      val instruction = UpdateInstruction("TestBetId", PersistenceType.LAPSE)

      val returnedBackOrderBook = mock[MockBackOrderBook]
      val backInstructionReport = mock[MockUpdateInstructionReport]
      val backOrderResponse = UpdateOrderResponse[OrderBook](returnedBackOrderBook, backInstructionReport)

      val returnedLayOrderBook = mock[MockLayOrderBook]
      val layInstructionReport = mock[MockUpdateInstructionReport]
      val layOrderResponse = UpdateOrderResponse[OrderBook](returnedLayOrderBook, layInstructionReport)

      (backOrderBook.hasBetId _).expects("TestBetId").returns(backBookHasBetId)

      if (backBookHasBetId)
        (backOrderBook.updateOrder _).expects(instruction).returns(backOrderResponse)
      else
        (layOrderBook.updateOrder _).expects(instruction).returns(layOrderResponse)

      val response = RunnerOrderBook(backOrderBook, layOrderBook).updateOrder(instruction)

      if (backBookHasBetId) {
        response.result.backOrderBook should be (returnedBackOrderBook)
        response.result.layOrderBook should be (layOrderBook)
        response.report should be (backInstructionReport)
      } else {
        response.result.backOrderBook should be(backOrderBook)
        response.result.layOrderBook should be(returnedLayOrderBook)
        response.report should be(layInstructionReport)
      }
    }
  }

  "RunnerOrderBook" should "matchOrders" in {
    val availableToBack = 10.0
    val availableToLay = 11.0

    val returnedBackOrderBook = mock[MockBackOrderBook]
    val returnedLayOrderBook = mock[MockLayOrderBook]

    (backOrderBook.matchOrders _).expects(availableToBack).returns(returnedBackOrderBook)
    (layOrderBook.matchOrders _).expects(availableToLay).returns(returnedLayOrderBook)

    val output = RunnerOrderBook(backOrderBook, layOrderBook).matchOrders(availableToBack,availableToLay)

    output.backOrderBook should be (returnedBackOrderBook)
    output.layOrderBook should be (returnedLayOrderBook)
  }

  //  "RunnerOrderBook" should "hasBetId" in {
  //
  //  }

  //  "RunnerOrderBook" should "getOrders" in {
//
//  }
}

