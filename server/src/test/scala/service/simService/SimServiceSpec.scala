package service.simService

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import server.Configuration
import service.BetfairServiceNGCommand
import service.simService.SimOrderBook._

import scala.collection.immutable.HashMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

class SimServiceSpec extends TestKit(ActorSystem("TestSystem")) with FlatSpecLike with ImplicitSender with ScalaFutures
  with Matchers with BeforeAndAfterAll with MockFactory with BeforeAndAfterEach {

  var mockBetfairService: BetfairServiceNGCommand = _
  var orderBook: TestProbe = _
  var simService: SimService = _
  val sessionToken = "12345"

  val config = Configuration("TestAppKey", "TestUsername", "TestPassword", "TestApiUrl", "TestIsoUrl", "TestNavUrl")
  class MockBetfairServiceNG extends BetfairServiceNGCommand(config)

  override def afterAll: Unit = {
    system.shutdown()
  }

  override def beforeEach: Unit = {
    mockBetfairService = mock[MockBetfairServiceNG]
    orderBook = TestProbe()
    simService = new SimService(config, mockBetfairService, orderBook.ref)
  }

//  "SimService listCurrentOrders" should "call BetfairService" in {
//
//  }
//
//  "SimService listClearedOrders" should "call BetfairService" in {
//
//  }

  "SimService listMarketBook" should "call BetfairService then update MarketBook using orderBook" in {

    List[Boolean](true, false).foreach(successful => {
      val marketIds = Set("TEST_ID")
      // TODO fix these params so they are not tuples
      val priceProjection = Some(("priceProjection", PriceProjection(Set.empty)))
      val orderProjection = Some(("orderProjection", OrderProjection.EXECUTABLE))           // TODO implement functionality
      val matchProjection = Some(("matchProjection", MatchProjection.NO_ROLLUP))            // TODO implement functionality
      val currencyCode = Some(("currencyCode", "TEST_CODE"))

      val returnedMarketBook = MarketBook("1", false, "TEST_STATUS", 0, false, false, false, 1, 1, 1, None, 0, 0, false, false, 1, Set.empty)
      val updatedMarketBook =  MarketBook("2", false, "TEST_STATUS", 0, false, false, false, 1, 1, 1, None, 0, 0, false, false, 1, Set.empty)

      // this simplifies the json serialisation of the Options when in the params HashMap
      val flattenedOpts = Seq(priceProjection, orderProjection, matchProjection, currencyCode).flatten

      val params = HashMap[String, Object]("marketIds" -> marketIds)

      val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listMarketBook",
        params = params ++ flattenedOpts.map(i => i._1 -> i._2).toMap)

      import spray.httpx.PlayJsonSupport._

      (mockBetfairService.makeAPIRequest[ListMarketBookContainer] _)
        .expects(sessionToken, request)
        .returns(if (successful) Future.successful(Some(ListMarketBookContainer(List(returnedMarketBook)))) else Future.successful(None))

      val future = simService.listMarketBook(sessionToken, marketIds, priceProjection, orderProjection, matchProjection, currencyCode)

      if (successful) {
        orderBook.expectMsg(500 millis, MatchOrders(ListMarketBookContainer(List(returnedMarketBook))))
        orderBook.reply(Some(ListMarketBookContainer(List(updatedMarketBook))))
        future.isCompleted should be (true)
        future.value.get should be (Success(Some(ListMarketBookContainer(List(updatedMarketBook)))))
      } else {
        future.isCompleted should be (false)
        future.value should be (None)
      }
    })
  }

//  def listMarketProfitAndLoss(sessionToken: String,
//                              marketIds: Set[String],
//                              includeSettledBets: Option[Boolean] = None,
//                              includeBspBets: Option[Boolean] = None,
//                              netOfCommission: Option[Boolean] = None): Future[Option[MarketProfitAndLossContainer]]
//

  "SimService placeOrders" should "call orderBook" in {
    val marketId = "3142"
    val instructions = Set.empty[PlaceInstruction]
    val customerRef = Some("TEST")

    val mockPlaceExecutionReportContainer = new MockPlaceExecutionReportContainer()
    val future = simService.placeOrders(sessionToken, marketId, instructions, customerRef)

    orderBook.expectMsg(500 millis, PlaceOrders(marketId, instructions, customerRef))
    orderBook.reply(mockPlaceExecutionReportContainer)
    whenReady(future) { res =>
      res match {
        case Some(container: PlaceExecutionReportContainer) =>
          container should be (mockPlaceExecutionReportContainer)
        case _ =>
          fail()
      }
    }
  }

  "SimService cancelOrders" should "call orderBook" in {
    val marketId = "3142"
    val instructions = Set.empty[CancelInstruction]
    val customerRef = Some("TEST")

    val mockCancelExecutionReportContainer = new MockCancelExecutionReportContainer()
    val future = simService.cancelOrders(sessionToken, marketId, instructions, customerRef)

    orderBook.expectMsg(500 millis, CancelOrders(marketId, instructions, customerRef))
    orderBook.reply(mockCancelExecutionReportContainer)
    whenReady(future) { res =>
      res match {
        case Some(container: CancelExecutionReportContainer) =>
          container should be (mockCancelExecutionReportContainer)
        case _ =>
          fail()
      }
    }
  }

  "SimService replaceOrders" should "call orderBook" in {
    val marketId = "3142"
    val instructions = Set.empty[ReplaceInstruction]
    val customerRef = Some("TEST")

    val mockReplaceExecutionReportContainer = new MockReplaceExecutionReportContainer()
    val future = simService.replaceOrders(sessionToken, marketId, instructions, customerRef)

    orderBook.expectMsg(500 millis, ReplaceOrders(marketId, instructions, customerRef))
    orderBook.reply(mockReplaceExecutionReportContainer)
    whenReady(future) { res =>
      res match {
        case Some(container: ReplaceExecutionReportContainer) =>
          container should be (mockReplaceExecutionReportContainer)
        case _ =>
          fail()
      }
    }
  }

  "SimService updateOrders" should "call orderBook" in {
    val marketId = "3142"
    val instructions = Set.empty[UpdateInstruction]
    val customerRef = Some("TEST")

    val mockUpdateExecutionReportContainer = new MockUpdateExecutionReportContainer()
    val future = simService.updateOrders(sessionToken, marketId, instructions, customerRef)

    orderBook.expectMsg(500 millis, UpdateOrders(marketId, instructions, customerRef))
    orderBook.reply(mockUpdateExecutionReportContainer)
    whenReady(future) { res =>
      res match {
        case Some(container: UpdateExecutionReportContainer) =>
          container should be (mockUpdateExecutionReportContainer)
        case _ =>
          fail()
      }
    }
  }
}
