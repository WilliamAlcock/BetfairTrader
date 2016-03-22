package service.simService

import domain.Side.Side
import domain._
import org.joda.time.DateTimeUtils
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, FlatSpec, Matchers}
import service.simService.TestHelpers._

class OrderBookSpec extends FlatSpec with Matchers with BeforeAndAfterAll with MockFactory with BeforeAndAfterEach {

  override def afterAll = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  var mockOrderFactory: OrderFactory = _
  var mockReportFactory: ReportFactory = _
  var mockUtils: Utils = _

  override def beforeEach = {
    mockOrderFactory = mock[MockOrderFactory]
    mockReportFactory = mock[MockReportFactory]
    mockUtils = mock[MockUtils]
  }

  // Mock out the current time
  DateTimeUtils.setCurrentMillisFixed(123456789)

  "orderBook" should "placeOrder and return new orderBook and report" in {
    val instruction = generatePlaceInstruction(Side.LAY, 10, 20)
    val order = generateOrder("1", 4, 10, Side.LAY)
    val orderBook = OrderBook(
      Side.LAY,
      orderFactory = mockOrderFactory,
      reportFactory = mockReportFactory,
      utils = mockUtils)

    val mockPlaceInstructionReport = mock[MockPlaceInstructionReport]
    val placeOrderResponse = PlaceOrderResponse(orderBook.copy(orders = List(order)), mockPlaceInstructionReport)

    (mockOrderFactory.createOrder _).expects(instruction).returns(order)
    (mockReportFactory.getPlaceInstructionReport _).expects(instruction, order).returns(mockPlaceInstructionReport)

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

      (mockReportFactory.getPlaceInstructionReport _).expects(instructions(0), orders(0))
      (mockReportFactory.getPlaceInstructionReport _).expects(instructions(1), orders(1))
      (mockReportFactory.getPlaceInstructionReport _).expects(instructions(2), orders(2))

      val orderBook = instructions.foldLeft[OrderBook](OrderBook(
        orderBookSide,
        orderFactory = mockOrderFactory,
        reportFactory = mockReportFactory,
        utils = mockUtils))((acc: OrderBook, x: PlaceInstruction) => acc.placeOrder(x).result)

      orderBook.getOrders should be(expectedOrders)
    }
  }

  val sides = Table(
    ("instructionSide", "orderBookSide", "throws"),
    (Side.LAY,          Side.BACK,       true),
    (Side.BACK,         Side.LAY,        true),
    (Side.BACK,         Side.BACK,       false),
    (Side.LAY,          Side.LAY,        false)
  )

  "orderBook" should "throw an error if you placeOrder with different side to the orderBook" in {
    forAll(sides) { (orderSide: Side, orderBookSide: Side, throws: Boolean) =>
      val order = generateOrder("3", 5, 22, orderBookSide)

      val instruction = generatePlaceInstruction(orderSide, 4, 10)
      val orderBook = OrderBook(
        orderBookSide,
        orderFactory = mockOrderFactory,
        reportFactory = mockReportFactory,
        utils = mockUtils)

      if (!throws) {
        (mockOrderFactory.createOrder _).expects(instruction).returns(order)
        (mockReportFactory.getPlaceInstructionReport _).expects(instruction, order).once()
      }

      try {
        orderBook.placeOrder(instruction)
        if (throws) fail()
      }
      catch {
        case _: IllegalArgumentException => if (!throws) fail()// Expected, so continue
        case _: Throwable => if (throws) fail()
      }
    }
  }

  // TODO add scenarios for the LAY side


  val cancelScenarios = Table(
    ("cancelInstruction",                "succeeds", "sizedCancelled",    "expectedOrders"),

    (CancelInstruction("4", Some(10.0)), false,      0.0,                 List(generateOrder("2", 2, 5, Side.BACK),
                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (CancelInstruction("4", None),       false,      0.0,                 List(generateOrder("2", 2, 5, Side.BACK),
                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (CancelInstruction("1", Some(0.0)),  true,       0.0,                 List(generateOrder("2", 2, 5, Side.BACK),
                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                               generateOrder("3", 5, 22, Side.BACK))),            // TODO this should really generate an error

    (CancelInstruction("1", Some(5.0)),  true,       5.0,                 List(generateOrder("2", 2, 5, Side.BACK),
                                                                               generateOrder("1", 4, 10, Side.BACK, sizeRemaining = Some(5), sizeCancelled = 5),
                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (CancelInstruction("1", Some(10.0)), true,       10.0,                List(generateOrder("2", 2, 5, Side.BACK),
                                                                               generateOrder("1", 4, 10, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeRemaining = Some(0), sizeCancelled = 10),
                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (CancelInstruction("1", None),       true,       10.0,                List(generateOrder("2", 2, 5, Side.BACK),
                                                                               generateOrder("1", 4, 10, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeRemaining = Some(0), sizeCancelled = 10),
                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (CancelInstruction("1", Some(15.0)), true,       10.0,                List(generateOrder("2", 2, 5, Side.BACK),
                                                                               generateOrder("1", 4, 10, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeRemaining = Some(0), sizeCancelled = 10),
                                                                               generateOrder("3", 5, 22, Side.BACK)))
  )

  "orderBook" should "cancelOrder and maintain ordering" in {

    forAll(cancelScenarios) { (instruction: CancelInstruction, succeeds: Boolean, sizeCancelled: Double, expectedOrders: List[Order]) =>
      val orders = List(
        generateOrder("2", 2, 5, Side.BACK),
        generateOrder("1", 4, 10, Side.BACK),
        generateOrder("3", 5, 22, Side.BACK)
      )

      val mockReport = mock[MockCancelInstructionReport]
      val expectedResult = OrderBook(
        Side.BACK,
        orders = expectedOrders,
        orderFactory = mockOrderFactory,
        reportFactory = mockReportFactory,
        utils = mockUtils)

      if (succeeds)
        (mockReportFactory.getCancelInstructionReport _)
          .expects(InstructionReportStatus.SUCCESS, None, instruction, Some(sizeCancelled))
          .returns(mockReport)
      else
        (mockReportFactory.getCancelInstructionReport _)
          .expects(InstructionReportStatus.FAILURE, Some(InstructionReportErrorCode.INVALID_BET_ID), instruction, None)
          .returns(mockReport)

      val response = OrderBook(
        Side.BACK,
        orders = orders,
        orderFactory = mockOrderFactory,
        reportFactory = mockReportFactory,
        utils = mockUtils).cancelOrder(instruction)

      response.result should be (expectedResult)
      response.report should be (mockReport)
    }
  }

  val updateScenarios = Table(
    ("UpdateInstruction",                   "orderBookSide", "succeeds",  "expectedOutput"),

    (UpdateInstruction("2", lapse),         Side.LAY,        true,        List(generateOrder("3", 5, 22, Side.LAY),
                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                               generateOrder("2", 2, 5, Side.LAY))),       // TODO this should really generate an error as does nothing

    (UpdateInstruction("2", persist),       Side.LAY,        true,        List(generateOrder("3", 5, 22, Side.LAY),
                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                               generateOrder("2", 2, 5, Side.LAY, persistenceType = persist))),

    (UpdateInstruction("2", marketOnClose), Side.LAY,        true,        List(generateOrder("3", 5, 22, Side.LAY),
                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                               generateOrder("2", 2, 5, Side.LAY, persistenceType = PersistenceType.MARKET_ON_CLOSE))),

    (UpdateInstruction("2", lapse),         Side.BACK,       true,        List(generateOrder("2", 2, 5, Side.BACK),
                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (UpdateInstruction("2", persist),       Side.BACK,       true,        List(generateOrder("2", 2, 5, Side.BACK, persistenceType = PersistenceType.PERSIST),
                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (UpdateInstruction("2", marketOnClose), Side.BACK,       true,        List(generateOrder("2", 2, 5, Side.BACK, persistenceType = PersistenceType.MARKET_ON_CLOSE),
                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (UpdateInstruction("4", lapse),         Side.LAY,        false,       List(generateOrder("3", 5, 22, Side.LAY),
                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                               generateOrder("2", 2, 5, Side.LAY))),

    (UpdateInstruction("4", persist),       Side.LAY,        false,       List(generateOrder("3", 5, 22, Side.LAY),
                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                               generateOrder("2", 2, 5, Side.LAY))),

    (UpdateInstruction("4", marketOnClose), Side.LAY,        false,       List(generateOrder("3", 5, 22, Side.LAY),
                                                                               generateOrder("1", 4, 10, Side.LAY),
                                                                               generateOrder("2", 2, 5, Side.LAY))),

    (UpdateInstruction("4", lapse),         Side.BACK,       false,       List(generateOrder("2", 2, 5, Side.BACK),
                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (UpdateInstruction("4", persist),       Side.BACK,       false,       List(generateOrder("2", 2, 5, Side.BACK),
                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                               generateOrder("3", 5, 22, Side.BACK))),

    (UpdateInstruction("4", marketOnClose), Side.BACK,       false,       List(generateOrder("2", 2, 5, Side.BACK),
                                                                               generateOrder("1", 4, 10, Side.BACK),
                                                                               generateOrder("3", 5, 22, Side.BACK)))
  )

  "orderBook" should "updateOrder and maintain ordering" in {

    forAll(updateScenarios) { (instruction: UpdateInstruction, orderBookSide: Side, succeeds: Boolean, expectedOrders: List[Order]) =>
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

      val mockReport = mock[MockUpdateInstructionReport]
      val expectedResult = OrderBook(
        orderBookSide,
        orders = expectedOrders,
        orderFactory = mockOrderFactory,
        reportFactory = mockReportFactory,
        utils = mockUtils)

      if (succeeds)
        (mockReportFactory.getUpdateInstructionReport _)
          .expects(InstructionReportStatus.SUCCESS, None, instruction)
          .returns(mockReport)
      else
        (mockReportFactory.getUpdateInstructionReport _)
          .expects(InstructionReportStatus.FAILURE, Some(InstructionReportErrorCode.INVALID_BET_ID), instruction)
          .returns(mockReport)

      val response = OrderBook(
        orderBookSide,
        orders = orders,
        orderFactory = mockOrderFactory,
        reportFactory = mockReportFactory,
        utils = mockUtils).updateOrder(instruction)

      response.result should be (expectedResult)
      response.report should be (mockReport)
    }
  }

  val matchScenarios = Table(
    ("price", "orderBookSide", "matchesUpdated", "expectedOutput"),

    (6.0,     Side.LAY,         false,            List(generateOrder("3", 5, 22, Side.LAY),
                                                      generateOrder("1", 4, 10, Side.LAY),
                                                      generateOrder("2", 2, 5, Side.LAY))),

    (6.0,     Side.BACK,       true,             List(generateOrder("2", 2, 5, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 5, sizeRemaining = Some(0)),
                                                      generateOrder("1", 4, 10, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 10, sizeRemaining = Some(0)),
                                                      generateOrder("3", 5, 22, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 22, sizeRemaining = Some(0)))),

    (4.0,     Side.LAY,        true,             List(generateOrder("3", 5, 22, Side.LAY, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 22, sizeRemaining = Some(0)),
                                                      generateOrder("1", 4, 10, Side.LAY, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 10, sizeRemaining = Some(0)),
                                                      generateOrder("2", 2, 5, Side.LAY))),

    (4.0,     Side.BACK,       true,             List(generateOrder("2", 2, 5, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 5, sizeRemaining = Some(0)),
                                                      generateOrder("1", 4, 10, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 10, sizeRemaining = Some(0)),
                                                      generateOrder("3", 5, 22, Side.BACK))),

    (1.0,     Side.LAY,        true,             List(generateOrder("3", 5, 22, Side.LAY, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 22, sizeRemaining = Some(0)),
                                                      generateOrder("1", 4, 10, Side.LAY, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 10, sizeRemaining = Some(0)),
                                                      generateOrder("2", 2, 5, Side.LAY, status = OrderStatus.EXECUTION_COMPLETE, sizeMatched = 5, sizeRemaining = Some(0)))),

    (1.0,     Side.BACK,       false,            List(generateOrder("2", 2, 5, Side.BACK),
                                                      generateOrder("1", 4, 10, Side.BACK),
                                                      generateOrder("3", 5, 22, Side.BACK)))
  )

  "orderBook" should "matchOrders and maintain ordering" in {

    forAll(matchScenarios) { (price: Double, orderBookSide: Side, matchUpdated: Boolean, expectedResult: List[Order]) =>
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

      val matches = if (matchUpdated) {
        val _match = Match(None, None, orderBookSide, 10, 10, None)
        (mockUtils.getMatchFromOrders _).expects(expectedResult, orderBookSide).returns(_match)
        List(_match)
      } else List.empty[Match]


      val orderBook = OrderBook(
        orderBookSide,
        orders = orders,
        matches = matches,
        orderFactory = mockOrderFactory,
        reportFactory = mockReportFactory,
        utils = mockUtils).matchOrders(price)

      orderBook.getOrders should be (expectedResult)
    }
  }

  val hasBetScenarios = Table(
    ("betId", "expectedResult"),

    ("2",     true),
    ("4",     false)
  )

  "orderBook" should "hasBetId should return true if the bet exists" in {

    forAll(hasBetScenarios) { (betId: String, expectedResult: Boolean) =>
      val orders = List(
        generateOrder("2", 2, 5, Side.BACK),
        generateOrder("1", 4, 10, Side.BACK, status = OrderStatus.EXECUTION_COMPLETE),
        generateOrder("3", 5, 22, Side.BACK)
      )

      val orderBook = OrderBook(
        Side.BACK,
        orders = orders,
        orderFactory = mockOrderFactory,
        reportFactory = mockReportFactory,
        utils = mockUtils)

      orderBook.hasBetId(betId) should be (expectedResult)
    }
  }

}