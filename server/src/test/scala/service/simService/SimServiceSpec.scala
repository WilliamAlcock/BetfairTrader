package service.simService

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import domain._
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import server.Configuration
import service.simService.SimOrderBook._
import service.{BetfairServiceNG, BetfairServiceNGCommand}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

class SimServiceSpec extends TestKit(ActorSystem("TestSystem")) with FlatSpecLike with ImplicitSender with ScalaFutures
  with Matchers with BeforeAndAfterAll with MockFactory with BeforeAndAfterEach {

  var mockBetfairService: BetfairServiceNG = _
  var orderBook: TestProbe = _
  var simService: SimService = _
  val sessionToken = "12345"

  val config = Configuration("TestAppKey", "TestUsername", "TestPassword", "TestApiUrl", "TestIsoUrl", "TestNavUrl")
  class MockBetfairServiceNG extends BetfairServiceNG(config, new BetfairServiceNGCommand(config))

  override def afterAll: Unit = {
    system.shutdown()
  }

  override def beforeEach: Unit = {
    mockBetfairService = mock[MockBetfairServiceNG]
    orderBook = TestProbe()
    simService = new SimService(config, mockBetfairService, orderBook.ref)
  }

  // TODO check messages received from all tests!
  "SimService login" should "call BetfairService" in {
    (mockBetfairService.login _).expects().once()

    simService.login()
  }

  "SimService logout" should "call BetfairService" in {
    (mockBetfairService.logout _).expects(sessionToken).once()

    simService.logout(sessionToken)
  }

  "SimService getNavigationData" should "call BetfairService" in {
    (mockBetfairService.getNavigationData _).expects(sessionToken).once()

    simService.getNavigationData(sessionToken)
  }

  "SimService listCompetitions" should "call BetfairService" in {
    val marketFilter = mock[MarketFilter]

    (mockBetfairService.listCompetitions _).expects(sessionToken, marketFilter).once()

    simService.listCompetitions(sessionToken, marketFilter)
  }

  "SimService listCountries" should "call BetfairService" in {
    val marketFilter = mock[MarketFilter]

    (mockBetfairService.listCountries _).expects(sessionToken, marketFilter).once()

    simService.listCountries(sessionToken, marketFilter)
  }

//  "SimService listCurrentOrders" should "call BetfairService" in {
//
//  }
//
//  "SimService listClearedOrders" should "call BetfairService" in {
//
//  }

  "SimService listEvents" should "call BetfairService" in {
    val marketFilter = mock[MarketFilter]

    (mockBetfairService.listEvents _).expects(sessionToken, marketFilter).once()

    simService.listEvents(sessionToken, marketFilter)
  }

  "SimService listEventTypes" should "call BetfairService" in {
    val marketFilter = mock[MarketFilter]

    (mockBetfairService.listEventTypes _).expects(sessionToken, marketFilter).once()

    simService.listEventTypes(sessionToken, marketFilter)
  }

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

      (mockBetfairService.listMarketBook _)
        .expects(sessionToken, marketIds, priceProjection, orderProjection, matchProjection, currencyCode)
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

  "SimService listMarketCatalogue" should "call BetfairService" in {
    val marketFilter = mock[MarketFilter]
    val marketProjection = List(MarketProjection.COMPETITION)
    val sort = MarketSort.FIRST_TO_START
    val maxResults: Integer = 10

    (mockBetfairService.listMarketCatalogue _).expects(sessionToken, marketFilter, marketProjection, sort, maxResults).once()

    simService.listMarketCatalogue(sessionToken, marketFilter, marketProjection, sort, maxResults)
  }

//  def listMarketProfitAndLoss(sessionToken: String,
//                              marketIds: Set[String],
//                              includeSettledBets: Option[Boolean] = None,
//                              includeBspBets: Option[Boolean] = None,
//                              netOfCommission: Option[Boolean] = None): Future[Option[MarketProfitAndLossContainer]]
//

  "SimService listMarketTypes" should "call BetfairService" in {
    val marketFilter = mock[MarketFilter]

    (mockBetfairService.listMarketTypes _).expects(sessionToken, marketFilter).once()

    simService.listMarketTypes(sessionToken, marketFilter)
  }

  "SimService listTimeRanges" should "call BetfairService" in {
    val marketFilter = mock[MarketFilter]
    val granularity = TimeGranularity.DAYS

    (mockBetfairService.listTimeRanges _).expects(sessionToken, marketFilter, granularity).once()

    simService.listTimeRanges(sessionToken, marketFilter, granularity)
  }

  "SimService listVenues" should "call BetfairService" in {
    val marketFilter = mock[MarketFilter]

    (mockBetfairService.listVenues _).expects(sessionToken, marketFilter).once()

    simService.listVenues(sessionToken, marketFilter)
  }

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
