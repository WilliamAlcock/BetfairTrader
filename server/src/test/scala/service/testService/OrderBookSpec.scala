package service.testService

import com.betfair.domain.{OrderStatus, OrderType, PersistenceType, Side, _}
import org.joda.time.{DateTime, DateTimeUtils}
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, ShouldMatchers}

class OrderBookSpec extends FlatSpec with ShouldMatchers with MockFactory with BeforeAndAfterAll with BeforeAndAfterEach{

  override def afterAll = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  DateTimeUtils.setCurrentMillisFixed(123456789)
  val selectionId = 3142
  val marketId = "testMarketId"
  val betId: String = "testBetId"

  var orderBook:OrderBook = _
  var betIdCounter: BetIdCounter = _

  override def beforeEach = {
    // Mocks
    betIdCounter = mock[BetIdCounter]
    orderBook = new OrderBook(betIdCounter)
  }

  // ***** CONVERT INSTRUCTION -> ORDER *****

  val getOrderFromInstructionCases =
    Table(
      ("description", "placeInstruction", "expectedOutput"),
      (
        "Limit Order, LAY",
        PlaceInstruction(
          OrderType.LIMIT, selectionId, 0.0, Side.LAY,
          Some(LimitOrder(123, 321, PersistenceType.LAPSE)), None, None
        ),
        Order(
          betId, OrderType.LIMIT, OrderStatus.EXECUTABLE, PersistenceType.LAPSE, Side.LAY,
          321, 123, 0.0, DateTime.now(), 0.0, 0.0, 123, 0.0, 0.0, 0.0
        )
        ),
      (
        "Limit Order, BACK",
        PlaceInstruction(
          OrderType.LIMIT, selectionId, 0.0, Side.BACK,
          Some(LimitOrder(123, 321, PersistenceType.LAPSE)), None, None
        ),
        Order(
          betId, OrderType.LIMIT, OrderStatus.EXECUTABLE, PersistenceType.LAPSE, Side.BACK,
          321, 123, 0.0, DateTime.now(), 0.0, 0.0, 123, 0.0, 0.0, 0.0
        )
      ),
      (
        "Limit On Close Order, LAY",
        PlaceInstruction(
          OrderType.LIMIT_ON_CLOSE, selectionId, 0.0, Side.LAY,
          None, Some(LimitOnCloseOrder(liability = 456, price = 654)), None
        ),
        Order(
          betId, OrderType.LIMIT_ON_CLOSE, OrderStatus.EXECUTABLE, PersistenceType.MARKET_ON_CLOSE, Side.LAY,
          654, 0.0, 456, DateTime.now(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )
      ),
      (
        "Limit On Close Order, BACK",
        PlaceInstruction(
          OrderType.LIMIT_ON_CLOSE, selectionId, 0.0, Side.BACK,
          None, Some(LimitOnCloseOrder(456, 654)), None
        ),
        Order(
          betId, OrderType.LIMIT_ON_CLOSE, OrderStatus.EXECUTABLE, PersistenceType.MARKET_ON_CLOSE, Side.BACK,
          654, 0.0, 456, DateTime.now(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )
      ),
      (
        "Market On Close Order, LAY",
        PlaceInstruction(
          OrderType.MARKET_ON_CLOSE, selectionId, 0.0, Side.LAY,
          None, None, Some(MarketOnCloseOrder(789))
        ),
        Order(
          betId, OrderType.MARKET_ON_CLOSE, OrderStatus.EXECUTABLE, PersistenceType.MARKET_ON_CLOSE, Side.LAY,
          0.0, 0.0, 789, DateTime.now(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )
      ),
      (
        "Market On Close Order, BACK",
        PlaceInstruction(
          OrderType.MARKET_ON_CLOSE, selectionId, 0.0, Side.BACK,
          None, None, Some(MarketOnCloseOrder(789))
        ),
        Order(
          betId, OrderType.MARKET_ON_CLOSE, OrderStatus.EXECUTABLE, PersistenceType.MARKET_ON_CLOSE, Side.BACK,
          0.0, 0.0, 789, DateTime.now(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )
      )
    )

  forAll(getOrderFromInstructionCases) { (description: String,
                                          placeInstruction: PlaceInstruction,
                                          expectedOutput: Order) =>
    "OrderBook.getOrderFromPlaceInstruction" should "convert " + description + "to an order" in {
      orderBook.getOrderFromPlaceInstruction(placeInstruction, betId) should be(expectedOutput)
    }
  }

  // Helper function to add orders
  def addOrder(instruction: PlaceInstruction) = {
    (betIdCounter.getNextBetId _).expects().returning(betId)

    orderBook.placeOrder(marketId, instruction)
  }

  "OrderBook.placeOrder" should "convert placeInstruction to order and add the order to the chosen market" in {
    // Input
    addOrder(
      PlaceInstruction(
        OrderType.LIMIT, selectionId, 3.142, Side.LAY,
        Some(LimitOrder(123, 321, PersistenceType.LAPSE)), None, None
      )
    )

    orderBook.getOrders(marketId, selectionId) should be(Set(
      Order(
        betId, OrderType.LIMIT, OrderStatus.EXECUTABLE, PersistenceType.LAPSE, Side.LAY,
        321, 123, 0.0, DateTime.now(), 0.0, 0.0, 123, 0.0, 0.0, 0.0
      )
    ))
  }

  // ***** CANCEL ORDERS *****

  val cancelOrderCases = Table(
    ("description", "cancelInstruction", "expectedSizeRemaining", "expectedSizeCancelled", "expectedOutput"),
    (
      "fail - invalid size reduction",
      CancelInstruction(betId, Some(0.0)),
      123.0,
      0.0,
      CancelInstructionReport(
        InstructionReportStatus.FAILURE,
        Some(InstructionReportErrorCode.INVALID_BET_SIZE),
        CancelInstruction(betId, Some(0.0)),
        None,
        None
      )
    ),
    (
      "fail - invalid BET ID",
      CancelInstruction("invalidBetId", Some(0.0)),
      123.0,
      0.0,
      CancelInstructionReport(
        InstructionReportStatus.FAILURE,
        Some(InstructionReportErrorCode.INVALID_BET_ID),
        CancelInstruction("invalidBetId", Some(0.0)),
        None,
        None
      )
    ),
    (
      "cancell all",
      CancelInstruction(betId, Some(123.0)),
      0.0,
      123.0,
      CancelInstructionReport(
        InstructionReportStatus.SUCCESS,
        None,
        CancelInstruction(betId, Some(123.0)),
        Some(123.0),
        Some(DateTime.now())
      )
    ),
    (
      "cancelled all, even though size reduction was larger than bet size",
      CancelInstruction(betId, Some(300.0)),
      0.0,
      123.0,
      CancelInstructionReport(
        InstructionReportStatus.SUCCESS,
        None,
        CancelInstruction(betId, Some(300.0)),
        Some(123.0),
        Some(DateTime.now())
      )
    ),
    (
      "cancell part",
      CancelInstruction(betId, Some(100.0)),
      23.0,
      100.0,
      CancelInstructionReport(
        InstructionReportStatus.SUCCESS,
        None,
        CancelInstruction(betId, Some(100.0)),
        Some(100.0),
        Some(DateTime.now())
      )
    )
  )

  forAll(cancelOrderCases) { (description: String,
                              cancelInstruction: CancelInstruction,
                              expectedSizeRemaining: Double,
                              expectedSizeCancelled: Double,
                              expectedOutput: CancelInstructionReport) =>
      "OrderBook.cancelOrder" should description in {
        // Add an order to cancel
        addOrder(
          PlaceInstruction(
            OrderType.LIMIT, selectionId, 3.142, Side.LAY,
            Some(LimitOrder(123, 321, PersistenceType.LAPSE)), None, None
          )
        )
        // Cancel the order
        val report = orderBook.cancelOrder(marketId, cancelInstruction)
        // Assert the order has been cancelled
        val order = orderBook.getOrders(marketId, selectionId).find(x => x.betId == betId)
        order.get.sizeRemaining should be(expectedSizeRemaining)
        order.get.sizeCancelled should be(expectedSizeCancelled)
        // Assert the report is correct
        report should be(expectedOutput)
      }
  }

  // TODO add tests for replaceOrder

//  val replaceOrderCases = Table(
//    ("description", "replaceInstruction", "expectedPrice", "expectedOutput"),
//    (
//      "successfully replace order with new order with ",
//
//    )
//    (
//      "fail if marketId does not exist",
//
//    ),
//    (
//      "fail if selectedId does not exist",
//
//    ),
//    (
//      "fail if size reduction is 0 or lower",
//
//    ),
//  )
//
//  forAll(replaceOrderCases) { (description: String, expectedOutput: ReplaceInstructionReport) =>
//    "OrderBook.replaceOrder" should description in {
//      // Add an order to replace
//      addOrder()
//      val report = orderBook.replaceOrder(marketId, replaceInstruction)
//      // Assert the order has been replaced
//      val order = orderBook.getOrders(marketId, selectionId).find(x => x.betId == betId)
//
//      // Assert the report is correct
//      report should be(expectedOutput)
//    }
//
//  }

  // ***** FILL ORDERS *****

  val fillOrderCases = Table(
    ("description", "instruction", "backPrice", "layPrice", "expectedOutput"),
    (
      "fill a lay order",
      PlaceInstruction(
        OrderType.LIMIT, selectionId, 0.0, Side.LAY,
        Some(LimitOrder(1, 5, PersistenceType.LAPSE)), None, None
      ),
      PriceSize(4.0, 0.0),  // back price
      PriceSize(5.0, 0.0),  // lay price
      Some(Set(Order(
        betId, OrderType.LIMIT, OrderStatus.EXECUTION_COMPLETE, PersistenceType.LAPSE, Side.LAY,
        5,      // Price
        1,      // Size
        0,      // Liability
        DateTime.now(),
        5,      // Avg Price Matched
        1,      // Matched
        0.0,    // Remaining
        0.0,    // Lapsed
        0.0,    // Cancelled
        0.0     // Voided
      )))
    ),
    (
      "fill a back order",
      PlaceInstruction(
        OrderType.LIMIT, selectionId, 0.0, Side.BACK,
        Some(LimitOrder(1, 4, PersistenceType.LAPSE)), None, None
      ),
      PriceSize(4.0, 0.0),   // back price
      PriceSize(5.0, 0.0),   // lay price
      Some(Set(Order(
        betId, OrderType.LIMIT, OrderStatus.EXECUTION_COMPLETE, PersistenceType.LAPSE, Side.BACK,
        4,      // Price
        1,      // Size
        0,      // Liability
        DateTime.now(),
        4,      // Avg Price Matched
        1,      // Matched
        0.0,    // Remaining
        0.0,    // Lapsed
        0.0,    // Cancelled
        0.0     // Voided
      )))
    ),
    (
      "dont fill a lay order",
      PlaceInstruction(
        OrderType.LIMIT, selectionId, 0.0, Side.LAY,
        Some(LimitOrder(1, 4, PersistenceType.LAPSE)), None, None
      ),
      PriceSize(4.0, 0.0),   // back price
      PriceSize(5.0, 0.0),   // lay price
      Some(Set(Order(
        betId, OrderType.LIMIT, OrderStatus.EXECUTABLE, PersistenceType.LAPSE, Side.LAY,
        4,      // Price
        1,      // Size
        0,      // Liability
        DateTime.now(),
        0.0,    // Avg Price Matched
        0.0,    // Matched
        1,      // Remaining
        0.0,    // Lapsed
        0.0,    // Cancelled
        0.0     // Voided
      )))
    ),
    (
      "dont fill a back order",
      PlaceInstruction(
        OrderType.LIMIT, selectionId, 0.0, Side.BACK,
        Some(LimitOrder(1, 5, PersistenceType.LAPSE)), None, None
      ),
      PriceSize(4.0, 0.0),   // back price
      PriceSize(5.0, 0.0),   // lay price
      Some(Set(Order(
        betId, OrderType.LIMIT, OrderStatus.EXECUTABLE, PersistenceType.LAPSE, Side.BACK,
        5,      // Price
        1,      // Size
        0,      // Liability
        DateTime.now(),
        0.0,    // Avg Price Matched
        0.0,    // Matched
        1,      // Remaining
        0.0,    // Lapsed
        0.0,    // Cancelled
        0.0     // Voided
      )))
    )
  )

  forAll(fillOrderCases) { (description: String,
                            instruction: PlaceInstruction,
                            backPrice: PriceSize,
                            layPrice: PriceSize,
                            expectedOutput: Option[Set[Order]]) =>
    "GetOrderFromPlaceInstruction" should description in {
      addOrder(instruction)
      orderBook.fillOrders(marketId, selectionId, backPrice, layPrice) should be(expectedOutput)
    }
  }

}
