package service.newTestService

import domain.Side.Side
import domain._
import org.joda.time.DateTimeUtils
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import service.newTestService.TestHelpers._


class OrderBookSpec extends FlatSpec with Matchers with BeforeAndAfterAll with MockFactory {

  override def afterAll = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  // Mock out the current time
  DateTimeUtils.setCurrentMillisFixed(123456789)

  // Mock class to mock the OrderFactory
  class MockOrderFactory extends OrderFactory {}

  "orderBook" should "placeOrder and return new orderBook and report" in {
    val mockOrderFactory = mock[MockOrderFactory]
    val instruction = generatePlaceInstruction(Side.LAY, 10, 20)
    val order = generateOrder("1", 4, 10, Side.LAY)
    val orderBook = OrderBook(Side.LAY, orderFactory = mockOrderFactory)
    val placeOrderResponse = PlaceOrderResponse(
      orderBook.copy(orders = List(order)),
      PlaceInstructionReport(
        InstructionReportStatus.SUCCESS, None, instruction,
        Some(order.betId), Some(order.placedDate), Some(order.avgPriceMatched), Some(order.sizeMatched)
      )
    )

    (mockOrderFactory.createOrder _).expects(instruction).returns(order)

    orderBook.placeOrder(instruction) should be (placeOrderResponse)
  }

  val placeScenarios = Table(
    ("orderBookSide", "expectedOrders"),

    (Side.LAY,        List(generateOrder("3", 5, 22, Side.LAY),
                           generateOrder("1", 4, 10, Side.LAY),
                           generateOrder("2", 2, 5, Side.LAY))),

    (Side.BACK,       List(generateOrder("2", 2, 5, Side.BACK),
                           generateOrder("1", 4, 10, Side.BACK),
                           generateOrder("3", 5, 22, Side.BACK)))
  )

  "orderBook" should "placeOrder and store them ordered by price (ascending for Back, descending for LAY)" in {

    forAll(placeScenarios) { (orderBookSide: Side, expectedOrders: List[Order]) =>
      val mockOrderFactory = mock[MockOrderFactory]

      val instructions = List(
        generatePlaceInstruction(orderBookSide, 4, 10),
        generatePlaceInstruction(orderBookSide, 2, 5),
        generatePlaceInstruction(orderBookSide, 5, 22)
      )

      val orders = List(
        generateOrder("1", 4, 10, orderBookSide),
        generateOrder("2", 2, 5, orderBookSide),
        generateOrder("3", 5, 22, orderBookSide)
      )

      (mockOrderFactory.createOrder _).expects(instructions(0)).returning(orders(0))
      (mockOrderFactory.createOrder _).expects(instructions(1)).returning(orders(1))
      (mockOrderFactory.createOrder _).expects(instructions(2)).returning(orders(2))

      val orderBook = instructions.foldLeft[OrderBook](OrderBook(orderBookSide, orderFactory = mockOrderFactory))(
        (acc: OrderBook, x: PlaceInstruction) => acc.placeOrder(x).result)

      orderBook.getOrders should be(expectedOrders)
    }
  }

  val sides = Table(
    ("instructionSide", "orderBookSide", "throws"),
    (Side.LAY,    Side.BACK,       true),
    (Side.BACK,   Side.LAY,        true),
    (Side.BACK,   Side.BACK,       false),
    (Side.LAY,    Side.LAY,        false)
  )

  "orderBook" should "throw an error if you placeOrder with different side to the orderBook" in {
    forAll(sides) { (orderSide: Side, orderBookSide: Side, throws: Boolean) =>
      val mockOrderFactory = mock[MockOrderFactory]
      val order = generateOrder("3", 5, 22, orderBookSide)

      val instruction = generatePlaceInstruction(orderSide, 4, 10)
      val orderBook = OrderBook(orderBookSide, orderFactory = mockOrderFactory)

      if (!throws)
        (mockOrderFactory.createOrder _).expects(instruction).returns(order)

      try {
        orderBook.placeOrder(instruction)
        if (throws) fail()
      }
      catch {
        case _: IllegalArgumentException => if (!throws) fail()// Expected, so continue
        case _ => if (throws) fail()
      }
    }
  }

  // TODO add scenarios for the LAY side


  val cancelScenarios = Table(
    ("cancelInstruction",                "expectedReport",                              "expectedOrders"),

    (CancelInstruction("4", Some(10.0)), genFailedCancelReport("4", Some(10.0)),        List(generateOrder("2", 2, 5, Side.BACK),
                                                                                             generateOrder("1", 4, 10, Side.BACK),
                                                                                             generateOrder("3", 5, 22, Side.BACK))),

    (CancelInstruction("4", None),       genFailedCancelReport("4", None),              List(generateOrder("2", 2, 5, Side.BACK),
                                                                                             generateOrder("1", 4, 10, Side.BACK),
                                                                                             generateOrder("3", 5, 22, Side.BACK))),

    (CancelInstruction("1", Some(0.0)),  genSuccessCancelReport("1", Some(0.0), 0.0),   List(generateOrder("2", 2, 5, Side.BACK),
                                                                                             generateOrder("1", 4, 10, Side.BACK),
                                                                                             generateOrder("3", 5, 22, Side.BACK))),            // TODO this should really generate an error

    (CancelInstruction("1", Some(5.0)),  genSuccessCancelReport("1", Some(5.0), 5.0),   List(generateOrder("2", 2, 5, Side.BACK),
                                                                                             generateOrder("1", 4, 10, Side.BACK, sizeRemaining = Some(5), sizeCancelled = 5),
                                                                                             generateOrder("3", 5, 22, Side.BACK))),

    (CancelInstruction("1", Some(10.0)), genSuccessCancelReport("1", Some(10.0), 10.0), List(generateOrder("2", 2, 5, Side.BACK),
                                                                                             generateOrder("1", 4, 10, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeRemaining = Some(0), sizeCancelled = 10),
                                                                                             generateOrder("3", 5, 22, Side.BACK))),

    (CancelInstruction("1", None),       genSuccessCancelReport("1", None, 10.0),       List(generateOrder("2", 2, 5, Side.BACK),
                                                                                             generateOrder("1", 4, 10, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeRemaining = Some(0), sizeCancelled = 10),
                                                                                             generateOrder("3", 5, 22, Side.BACK))),

    (CancelInstruction("1", Some(15.0)), genSuccessCancelReport("1", Some(15.0), 10.0), List(generateOrder("2", 2, 5, Side.BACK),
                                                                                             generateOrder("1", 4, 10, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeRemaining = Some(0), sizeCancelled = 10),
                                                                                             generateOrder("3", 5, 22, Side.BACK)))
  )

  "orderBook" should "cancelOrder and maintain ordering" in {

    forAll(cancelScenarios) { (instruction: CancelInstruction, expectedReport: CancelInstructionReport, expectedOrders: List[Order]) =>
      val mockOrderFactory = mock[MockOrderFactory]

      val orders = List(
        generateOrder("2", 2, 5, Side.BACK),
        generateOrder("1", 4, 10, Side.BACK),
        generateOrder("3", 5, 22, Side.BACK)
      )

      val response = OrderBook(Side.BACK, orders = orders, orderFactory = mockOrderFactory).cancelOrder(instruction)

      val expectedResult = OrderBook(Side.BACK, orders = expectedOrders, orderFactory = mockOrderFactory)

      response.result should be (expectedResult)
      response.report should be (expectedReport)
    }
  }

  val updateScenarios = Table(
    ("UpdateInstruction",                   "orderBookSide", "expectedReport",                     "expectedOutput"),

    (UpdateInstruction("2", lapse),         Side.LAY,        genSuccessUpdateReport("2", lapse),          List(generateOrder("3", 5, 22, Side.LAY),
                                                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                                                               generateOrder("2", 2, 5, Side.LAY))),       // TODO this should really generate an error as does nothing

    (UpdateInstruction("2", persist),       Side.LAY,        genSuccessUpdateReport("2", persist),        List(generateOrder("3", 5, 22, Side.LAY),
                                                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                                                               generateOrder("2", 2, 5, Side.LAY, persistenceType = persist))),

    (UpdateInstruction("2", marketOnClose), Side.LAY,        genSuccessUpdateReport("2", marketOnClose),  List(generateOrder("3", 5, 22, Side.LAY),
                                                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                                                               generateOrder("2", 2, 5, Side.LAY, persistenceType = PersistenceType.MARKET_ON_CLOSE))),

    (UpdateInstruction("2", lapse),         Side.BACK,       genSuccessUpdateReport("2", lapse),          List(generateOrder("2", 2, 5, Side.BACK),
                                                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (UpdateInstruction("2", persist),       Side.BACK,       genSuccessUpdateReport("2", persist),        List(generateOrder("2", 2, 5, Side.BACK, persistenceType = PersistenceType.PERSIST),
                                                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (UpdateInstruction("2", marketOnClose), Side.BACK,       genSuccessUpdateReport("2", marketOnClose),  List(generateOrder("2", 2, 5, Side.BACK, persistenceType = PersistenceType.MARKET_ON_CLOSE),
                                                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (UpdateInstruction("4", lapse),         Side.LAY,        genFailedUpdateReport("4", lapse),           List(generateOrder("3", 5, 22, Side.LAY),
                                                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                                                               generateOrder("2", 2, 5, Side.LAY))),

    (UpdateInstruction("4", persist),       Side.LAY,        genFailedUpdateReport("4", persist),         List(generateOrder("3", 5, 22, Side.LAY),
                                                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                                                               generateOrder("2", 2, 5, Side.LAY))),

    (UpdateInstruction("4", marketOnClose), Side.LAY,        genFailedUpdateReport("4", marketOnClose),   List(generateOrder("3", 5, 22, Side.LAY),
                                                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                                                               generateOrder("2", 2, 5, Side.LAY))),

    (UpdateInstruction("4", lapse),         Side.BACK,       genFailedUpdateReport("4", lapse),           List(generateOrder("2", 2, 5, Side.BACK),
                                                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (UpdateInstruction("4", persist),       Side.BACK,       genFailedUpdateReport("4", persist),         List(generateOrder("2", 2, 5, Side.BACK),
                                                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (UpdateInstruction("4", marketOnClose), Side.BACK,       genFailedUpdateReport("4", marketOnClose),   List(generateOrder("2", 2, 5, Side.BACK),
                                                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                                                               generateOrder("3", 5, 22, Side.BACK)))
  )

  "orderBook" should "updateOrder and maintain ordering" in {

    forAll(updateScenarios) { (instruction: UpdateInstruction, orderBookSide: Side, expectedReport: UpdateInstructionReport, expectedOrders: List[Order]) =>
      val mockOrderFactory = mock[MockOrderFactory]

      val orders = if (orderBookSide == Side.BACK) {
        List(
          generateOrder("2", 2, 5, Side.BACK),
          generateOrder("1", 4, 10, Side.BACK),
          generateOrder("3", 5, 22, Side.BACK)
        )
      } else {
        List(
          generateOrder("3", 5, 22, Side.LAY),
          generateOrder("1", 4, 10, Side.LAY),
          generateOrder("2", 2, 5, Side.LAY)
        )
      }

      val response = OrderBook(orderBookSide, orders = orders, orderFactory = mockOrderFactory).updateOrder(instruction)

      val expectedResult = OrderBook(orderBookSide, orders = expectedOrders, orderFactory = mockOrderFactory)

      response.result should be (expectedResult)
      response.report should be (expectedReport)
    }
  }

  val matchScenarios = Table(
    ("price", "orderBookSide", "expectedOutput"),

    (6.0,     Side.LAY,        List(generateOrder("3", 5, 22, Side.LAY),
                                    generateOrder("1", 4, 10, Side.LAY),
                                    generateOrder("2", 2, 5, Side.LAY))),

    (6.0,     Side.BACK,       List(generateOrder("2", 2, 5, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 5, sizeRemaining = Some(0)),
                                    generateOrder("1", 4, 10, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 10, sizeRemaining = Some(0)),
                                    generateOrder("3", 5, 22, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 22, sizeRemaining = Some(0)))),

    (4.0,     Side.LAY,        List(generateOrder("3", 5, 22, Side.LAY, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 22, sizeRemaining = Some(0)),
                                    generateOrder("1", 4, 10, Side.LAY, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 10, sizeRemaining = Some(0)),
                                    generateOrder("2", 2, 5, Side.LAY))),

    (4.0,     Side.BACK,       List(generateOrder("2", 2, 5, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 5, sizeRemaining = Some(0)),
                                    generateOrder("1", 4, 10, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 10, sizeRemaining = Some(0)),
                                    generateOrder("3", 5, 22, Side.BACK))),

    (1.0,     Side.LAY,        List(generateOrder("3", 5, 22, Side.LAY, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 22, sizeRemaining = Some(0)),
                                    generateOrder("1", 4, 10, Side.LAY, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 10, sizeRemaining = Some(0)),
                                    generateOrder("2", 2, 5, Side.LAY, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 5, sizeRemaining = Some(0)))),

    (1.0,     Side.BACK,       List(generateOrder("2", 2, 5, Side.BACK),
                                    generateOrder("1", 4, 10, Side.BACK),
                                    generateOrder("3", 5, 22, Side.BACK)))
  )

  "orderBook" should "matchOrders and maintain ordering" in {

    forAll(matchScenarios) { (price: Double, orderBookSide: Side, expectedResult: List[Order]) =>
      val mockOrderFactory = mock[MockOrderFactory]

      val orders = if (orderBookSide == Side.BACK) {
        List(
          generateOrder("2", 2, 5, Side.BACK),
          generateOrder("1", 4, 10, Side.BACK),
          generateOrder("3", 5, 22, Side.BACK)
        )
      } else {
        List(
          generateOrder("3", 5, 22, Side.LAY),
          generateOrder("1", 4, 10, Side.LAY),
          generateOrder("2", 2, 5, Side.LAY)
        )
      }

      val orderBook = OrderBook(orderBookSide, orders = orders, orderFactory = mockOrderFactory).matchOrders(price)

      orderBook.getOrders should be (expectedResult)
    }
  }

}