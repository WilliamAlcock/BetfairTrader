package core.orderManager

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import core.api.commands.{PlaceOrders, SubscribeToMarkets}
import core.dataProvider.polling.BEST
import core.eventBus.EventBus
import domain.ExecutionReportStatus.ExecutionReportStatus
import domain._
import org.scalamock.function.MockFunction0
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}
import server.Configuration
import service.BetfairService

import scala.concurrent.Future

class OrderManagerSpec extends TestKit(ActorSystem("TestSystem")) with FlatSpecLike with Matchers with BeforeAndAfterEach with MockFactory with ImplicitSender{

  var controller: TestProbe = _
  var betfairService: BetfairService = _
  var eventBus: EventBus = _
  var orderManager: TestActorRef[OrderManager] = _

//  var _broadcast: MockFunction1[OrderManagerOutput, Unit] = _
  var _update: MockFunction0[Unit] = _
//  var _processOrder: MockFunction4[String, Long, Double, Order, Set[OrderManagerOutput]] = _
//  var _processRunner: MockFunction2[String, Runner, Set[OrderManagerOutput]] = _
//  var _processMarketBook: MockFunction1[MarketBook, Set[OrderManagerOutput]] = _
//  var _allOrdersCompleted: MockFunction1[MarketBook, Boolean] = _

  val sessionToken = "TEST_TOKEN"

  val testConfig = Configuration(
    appKey                    = "TEST_APP_KEY",
    username                  = "TEST_USERNAME",
    password                  = "TEST_PASSWORD",
    apiUrl                    = "TEST_API_URL",
    isoUrl                    = "TEST_ISO_URL",
    navUrl                    = "TEST_NAV_URL",
    systemAlertsChannel       = "TEST_SYSTEM_ALERTS",
    marketUpdateChannel       = "TEST_MARKET_UPDATES",
    orderUpdateChannel        = "TEST_ORDER_UPDATES",
    navDataInstructions       = "TEST_NAV_DATA_INSTRUCTIONS",
    dataModelInstructions     = "TEST_DATA_MODEL_INSTRUCTIONS",
    orderManagerInstructions  = "TEST_ORDER_MANAGER_INSTRUCTIONS",
    dataProviderInstructions  = "TEST_DATA_PROVIDER_INSTRUCTIONS"
  )

  class TestBetfairService extends BetfairService {
    val config = testConfig
    val command = null
  }

  override def beforeEach(): Unit = {
    controller = TestProbe()
    betfairService = mock[BetfairService]
    eventBus = mock[EventBus]

      //    _broadcast = mockFunction[OrderManagerOutput, Unit]
    _update = mockFunction[Unit]
//    _processOrder = mockFunction[String, Long, Double, Order, Set[OrderManagerOutput]]
//    _processRunner = mockFunction[String, Runner, Set[OrderManagerOutput]]
//    _processMarketBook = mockFunction[MarketBook, Set[OrderManagerOutput]]
//    _allOrdersCompleted = mockFunction[MarketBook, Boolean]

    _update.stubs()

    orderManager = TestActorRef(Props(new OrderManager(testConfig, sessionToken, controller.ref, betfairService, eventBus) {
      override def update() = _update.apply()
    }))
//      override def broadcast(o: OrderManagerOutput) = _broadcast.apply(o)

//      override def processOrder(m: String, s: Long, h: Double, o: Order) = _processOrder.apply(m, s, h, o)
//      override def processRunner(m: String, r: Runner) = _processRunner.apply(m, r)
//      override def processMarketBook(m: MarketBook) = _processMarketBook.apply(m)
//      override def allOrdersCompleted(m: MarketBook) = _allOrdersCompleted.apply(m)
//    }))

  }

  val placeOrders_Cases = Table(
    ("ReportStatus",                              "subscribeToMarkets"),

//    (ExecutionReportStatus.SUCCESS,               true)
//    (ExecutionReportStatus.PROCESSED_WITH_ERRORS, false)
    (ExecutionReportStatus.TIMEOUT,               false),
    (ExecutionReportStatus.FAILURE,               false)
  )

  forAll(placeOrders_Cases) { (reportStatus: ExecutionReportStatus, subscribeToMarkets: Boolean) =>
    "OrderManager.PlaceOrders" should "notify user when " + reportStatus in {
      val marketId = "TEST_MARKET_ID"
      val instructions = Set(PlaceInstruction(OrderType.LIMIT, 1L, 0.0, Side.BACK))
      val customerRef = Some("TEST_CUSTOMER_REF")

      val serviceOutput = PlaceExecutionReportContainer(PlaceExecutionReport(reportStatus, marketId, None, Set(), None))

      (betfairService.placeOrders _)
        .expects(sessionToken, marketId, instructions, customerRef)
        .returns(Future.successful(Some(serviceOutput)))

      orderManager ! PlaceOrders(marketId, instructions, customerRef)

      if (subscribeToMarkets) {
        controller.expectMsg(SubscribeToMarkets(Set(marketId), BEST))
      }

      expectMsg(serviceOutput)
    }
  }





//  "OrderManager" should "PlaceOrders otherwise" in {
//
//  }
//
//  "OrderManager" should "Cancel Orders when SUCCESS" in {
//
//  }
//
//  "OrderManager" should "Cancel Orders otherwise" in {
//
//  }
//
//  "OrderManager" should "Replace Orders when SUCCESS" in {
//
//  }
//
//  "OrderManager" should "Replace Orders otherwise" in {
//
//  }
//
//  "OrderManager" should "Update Orders when SUCCESS" in {
//
//  }
//
//  "OrderManager" should "Update Orders otherwise" in {
//
//  }
}
