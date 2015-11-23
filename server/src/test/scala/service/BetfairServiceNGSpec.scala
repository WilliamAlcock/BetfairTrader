package service

import akka.actor.ActorSystem
import com.betfair.Configuration
import com.betfair.domain._
import com.betfair.service.BetfairServiceNGCommand
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent._
import org.scalatest.{BeforeAndAfterEach, FlatSpec, ShouldMatchers}
import spray.httpx.unmarshalling._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BetfairServiceNGSpec extends FlatSpec with ShouldMatchers with MockFactory with ScalaFutures with BeforeAndAfterEach {

  implicit val system = ActorSystem("on-spray-can")

  // TODO make config implicit to avoid this workaround
  val testConfig = Configuration(
    appKey = "testAppKey",
    username = "testUsername",
    password = "testPassword",
    apiUrl = "localhost",
    isoUrl = "localhost"
  )
  class MockCommand extends BetfairServiceNGCommand(testConfig)

  val testCommand = mock[MockCommand]

  val sessionToken = "12345"

  "The BetfairServiceNGCommand" should "retrieve list of competitions based on supplied market filter" in {

    // Input
    val marketFilter = new MarketFilter()

    // Expected Calls
    val params = mutable.HashMap[String, Object]("filter" -> marketFilter)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listCompetitions", params = params)

    // Expected Response
    val testCompetition = new Competition("123", "testCompetition")
    val testCompetitionResult = new CompetitionResult(testCompetition, 1, "testRegion")
    val competitionResultContainer = new ListCompetitionsContainer(List(testCompetitionResult))

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[ListCompetitionsContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(competitionResultContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listCompetitions(sessionToken, marketFilter)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result.size should be(1)
          container.result(0) should be theSameInstanceAs testCompetitionResult
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "retrieve list of countries based on supplied market filter" in {

    // Input
    val marketFilter = new MarketFilter()

    // Expected Calls
    val params = mutable.HashMap[String, Object]("filter" -> marketFilter)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listCountries", params = params)

    // Expected Response
    val testCountryCodeResult = new CountryCodeResult("testCountryCode", 1)
    val testCountryCodeResultContainer = new ListCountriesContainer(List(testCountryCodeResult))

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[ListCountriesContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testCountryCodeResultContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listCountries(sessionToken, marketFilter)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result.size should be(1)
          container.result(0) should be theSameInstanceAs testCountryCodeResult
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "retrieve list of Current Orders based on supplied params" in {

    // Input
    val testBetIds = Some("betIds", Set("BetId1", "BetId2", "BetId3"))
    val testMarketIds = Some("marketIds", Set("MarketId1", "Marketid2", "MarketId3"))
    val testOrderProjection = Some("orderProjection", OrderProjection.ALL)
    val testPlacedDateRange = Some("placedDateRange", new TimeRange())
    val testDateRange = Some("dateRange", new TimeRange())
    val testOrderBy = Some("orderBy", OrderBy.BY_MARKET)
    val testSortDir = Some("sortDir", SortDir.EARLIEST_TO_LATEST)
    val testFromRecord = Some("fromRecord", 0: Integer)
    val testRecordCount = Some("recordCount", 10: Integer)

    // Expected Calls
    val ops = Seq(testBetIds, testMarketIds, testOrderProjection, testPlacedDateRange, testDateRange, testOrderBy, testSortDir, testFromRecord, testRecordCount).flatten
    val params = mutable.HashMap[String, Object]()
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listCurrentOrders", params = params ++ ops.map(i => i._1 -> i._2).toMap)

    // Expected Response
    val testCurrentOrderSummary = new CurrentOrderSummary("testBetId", "testMarketId", 123, 1, new PriceSize(1, 2), 3, Side.BACK,
      OrderStatus.EXECUTABLE, PersistenceType.LAPSE, OrderType.LIMIT, new DateTime(), None, 2, 3, 4, 5, 6, 7, "testRegulatorCode")

    val testCurrentOrderSummaryReport = new CurrentOrderSummaryReport(Set(testCurrentOrderSummary), false)
    val testCurrentOrderSummaryReportContainer = new ListCurrentOrdersContainer(testCurrentOrderSummaryReport)

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[ListCurrentOrdersContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testCurrentOrderSummaryReportContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listCurrentOrders(sessionToken, testBetIds, testMarketIds, testOrderProjection,
      testPlacedDateRange, testDateRange, testOrderBy, testSortDir, testFromRecord, testRecordCount)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result should be theSameInstanceAs testCurrentOrderSummaryReport
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "retrieve list of Cleared Orders based on supplied params" in {

    // Input
    val testBetStatus = Some("betStatus", BetStatus.CANCELLED)
    val testEventTypeIds = Some("eventTypeIds", Set("testEventTypeId"))
    val testEventIds = Some("eventids", Set("testEventId"))
    val testMarketIds = Some("marketIds", Set("testMarketId"))
    val testRunnerIds = Some("runnerIds", Set("testRunnerId"))
    val testBetIds = Some("betIds", Set("testBetId"))
    val testSide = Some("Side", Side.BACK)
    val testSettledDateRange = Some("settledDateRange", new TimeRange())
    val testGroupBy = Some("groupBy", GroupBy.EVENT)
    val testIncludeItemDescription = Some(true)
    val testFromRecord = Some("fromRecord", 1: Integer)
    val testRecordCount = Some("recordCount", 2: Integer)

    // Expected Calls
    val ops = Seq(testBetStatus, testEventTypeIds, testEventIds, testMarketIds, testRunnerIds, testBetIds, testSide, testSettledDateRange, testGroupBy,
      testFromRecord, testRecordCount).flatten
    val params = mutable.HashMap[String, Object]("includeItemDescription" -> testIncludeItemDescription)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listClearedOrders", params = params ++ ops.map(i => i._1 -> i._2).toMap)

    // Expected Response
    val testClearedOrderSummary = new ClearedOrderSummary("testEventTypeId", "testEventId", "testMarketId", 123, 1, "testBetId", new DateTime(),
      PersistenceType.LAPSE, OrderType.LIMIT, Side.BACK, new ItemDescription("testEventTypeDesc", "testEventDesc", "testMarketDesc", "testMarketType", new DateTime(), "testRunnerDesc", 1, 2),
      "testBetOutcome", 1, new DateTime(), new DateTime(), 2, 3, 4, true, 5, 6, 7)

    val testClearedOrderSummaryReport = new ClearedOrderSummaryReport(Set(testClearedOrderSummary), false)
    val testClearedOrderSummaryReportContainer = new ListClearedOrdersContainer(testClearedOrderSummaryReport)

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[ListClearedOrdersContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testClearedOrderSummaryReportContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listClearedOrders(sessionToken, testBetStatus, testEventTypeIds, testEventIds, testMarketIds, testRunnerIds, testBetIds,
      testSide, testSettledDateRange, testGroupBy, testIncludeItemDescription, testFromRecord, testRecordCount)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result should be theSameInstanceAs testClearedOrderSummaryReport
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "retrieve list of events based on supplied market filter" in {

    // Input
    val marketFilter = new MarketFilter()

    // Expected Calls
    val params = mutable.HashMap[String, Object]("filter" -> marketFilter)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listEvents", params = params)

    // Expected Response
    val testEvent = Event("123", "event", None, "GMT", None, new DateTime())
    val testEventResult = EventResult(testEvent, 1)
    val testEventResultContainer = ListEventResultContainer(List(testEventResult))

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[ListEventResultContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testEventResultContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listEvents(sessionToken, marketFilter)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result.size should be(1)
          container.result(0) should be theSameInstanceAs testEventResult
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "retrieve list of event types based on supplied market filter" in {

    // Input
    val marketFilter = new MarketFilter()

    // Expected Calls
    val params = mutable.HashMap[String, Object]("filter" -> marketFilter)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listEventTypes", params = params)

    // Expected Response
    val testEventType = new EventType("testId", "testName")
    val testEventTypeResult = new EventTypeResult(testEventType, 1)
    val testEventTypeResultContainer = new ListEventTypeResultContainer(List(testEventTypeResult))

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[ListEventTypeResultContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testEventTypeResultContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listEventTypes(sessionToken, marketFilter)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result.size should be(1)
          container.result(0) should be theSameInstanceAs testEventTypeResult
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "retrieve market book based on supplied params" in {

    // Input
    val testMarketIds = Set("testMarketId")
    val testPriceProjection = Some("priceProjection", new PriceProjection(Set(PriceData.EX_ALL_OFFERS)))
    val testOrderProjection = Some("orderProjection", OrderProjection.ALL)
    val testMatchProjection = Some("matchProjection", MatchProjection.NO_ROLLUP)
    val testCurrencyCode = Some("currencyCode", "testCurrencyCode")

    // Expected Calls
    val ops = Seq(testPriceProjection, testOrderProjection, testMatchProjection, testCurrencyCode).flatten
    val params = mutable.HashMap[String, Object]("marketIds" -> testMarketIds)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listMarketBook", params = params ++ ops)

    // Expected Response
    val testRunner = new Runner(1, 2, "testString")
    val testMarketBook = new MarketBook("testMarketId", true, "testStatus", 1, true, true, true, 2, 3, 4, Some(new DateTime()), 5, 6, true, true, 7, Set(testRunner))
    val testListMarketBookContainer = new ListMarketBookContainer(List(testMarketBook))

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[ListMarketBookContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testListMarketBookContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listMarketBook(sessionToken, testMarketIds, testPriceProjection, testOrderProjection, testMatchProjection, testCurrencyCode)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result.size should be(1)
          container.result(0) should be theSameInstanceAs testMarketBook
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "retrieve market catalogue based on supplied params" in {

    // Input
    val testMarketFilter = new MarketFilter()
    val testMarketProjection = List(MarketProjection.EVENT)
    val testMarketSort = MarketSort.FIRST_TO_START
    val testMaxResults = 10: Integer

    // Expected Calls
    val params = mutable.HashMap[String, Object]("filter" -> testMarketFilter, "marketProjection" -> testMarketProjection,
      "sort" -> testMarketSort, "maxResults" -> testMaxResults)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listMarketCatalogue", params = params)

    // Expected Response
    val testEvent = new Event("testId", "testName", None, "testTimeZone", None, new DateTime())
    val testMarketCatalogue = new MarketCatalogue("testMarketId", "testMarketName", None, None, 1, None, None, None, testEvent)
    val testListMarketCatalogueContainer = new ListMarketCatalogueContainer(List(testMarketCatalogue))

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[ListMarketCatalogueContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testListMarketCatalogueContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listMarketCatalogue(sessionToken, testMarketFilter, testMarketProjection, testMarketSort, testMaxResults)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result.size should be(1)
          container.result(0) should be theSameInstanceAs testMarketCatalogue
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "retrieve market profit and loss based on supplied params" in {

    // Input
    val testMarketIds = Set("testMarketId")
    val testIncludeSettledBets = None
    val testIncludeBspBets = None
    val testNetOfCommission = None

    // Expected Calls
    val params = mutable.HashMap[String, Object]("marketIds" -> testMarketIds, "includeSettledBets" -> testIncludeSettledBets,
      "includeBspBets" -> testIncludeBspBets, "netOfCommission" -> testIncludeBspBets)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listMarketProfitAndLoss", params = params)

    // Expected Response
    val testRunnerProfitAndLoss = new RunnerProfitAndLoss(1, 2, 3, 4)
    val testMarketProfitAndLoss = new MarketProfitAndLoss("testMarketId", 1, Set(testRunnerProfitAndLoss))
    val testMarketProfitAndLossContainer = new MarketProfitAndLossContainer(List(testMarketProfitAndLoss))

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[MarketProfitAndLossContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testMarketProfitAndLossContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listMarketProfitAndLoss(sessionToken, testMarketIds, testIncludeSettledBets, testIncludeBspBets, testNetOfCommission)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result.size should be(1)
          container.result(0) should be theSameInstanceAs testMarketProfitAndLoss
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "retrieve market types based on supplied market filter" in {

    // Input
    val testMarketFilter = new MarketFilter()

    // Expected Calls
    val params = mutable.HashMap[String, Object]("filter" -> testMarketFilter)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listMarketTypes", params = params)

    // Expected Response
    val testMarketTypeResult = new MarketTypeResult("testMarketType", 1)
    val testMarketTypeResultContainer = new MarketTypeResultContainer(List(testMarketTypeResult))

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[MarketTypeResultContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testMarketTypeResultContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listMarketTypes(sessionToken, testMarketFilter)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result.size should be(1)
          container.result(0) should be theSameInstanceAs testMarketTypeResult
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "retrieve time ranges based on supplied market filter and granularity" in {

    // Input
    val testMarketFilter = new MarketFilter()
    val testGranularity = TimeGranularity.DAYS

    // Expected Calls
    val params = mutable.HashMap[String, Object]("filter" -> testMarketFilter, "granularity" -> testGranularity)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listTimeRanges", params = params)

    // Expected Response
    val testTimeRangeResult = new TimeRangeResult(new TimeRange(), 1)
    val testTimeRangeResultContainer = new TimeRangeResultContainer(List(testTimeRangeResult))

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[TimeRangeResultContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testTimeRangeResultContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listTimeRanges(sessionToken, testMarketFilter, testGranularity)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result.size should be(1)
          container.result(0) should be theSameInstanceAs testTimeRangeResult
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "retrieve venues based on supplied market filter" in {

    // Input
    val testMarketFilter = new MarketFilter()

    // Expected Calls
    val params = mutable.HashMap[String, Object]("filter" -> testMarketFilter)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listVenues", params = params)

    // Expected Response
    val testVenueResult = new VenueResult("testVenue", 1)
    val testVenueResultContainer = new VenueResultContainer(List(testVenueResult))

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[VenueResultContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testVenueResultContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.listVenues(sessionToken, testMarketFilter)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result.size should be(1)
          container.result(0) should be theSameInstanceAs testVenueResult
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "place orders based on supplied params" in {

    // Input
    val testMarketId = "testMarketId"
    val testPlaceInstruction = new PlaceInstruction(OrderType.LIMIT, 1, 2, Side.BACK)
    val testCustomerRef = Some("testCustomerRef")

    // Expected Call
    val params = mutable.HashMap[String, Object](
      "marketId" -> testMarketId,
      "instructions" -> Set(testPlaceInstruction),
      "customerRef" -> testCustomerRef
    )
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/placeOrders", params = params)

    // Expected Response
    val testPlaceInstructionReport = new PlaceInstructionReport(InstructionReportStatus.FAILURE, Some(InstructionReportErrorCode.BET_IN_PROGRESS),
      testPlaceInstruction, Some("testBetId"), Some(new DateTime()), Some(1), Some(2))
    val testPlaceExecutionReport = new PlaceExecutionReport(ExecutionReportStatus.SUCCESS, "testMarketId", Some(ExecutionReportErrorCode.BET_ACTION_ERROR),
      Set(testPlaceInstructionReport), Some("testCustomerRef"))
    val testPlaceExecutionReportContainer = new PlaceExecutionReportContainer(testPlaceExecutionReport)

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[PlaceExecutionReportContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testPlaceExecutionReportContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.placeOrders(sessionToken, testMarketId, Set(testPlaceInstruction), testCustomerRef)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result should be theSameInstanceAs testPlaceExecutionReport
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "cancel orders based on supplied params" in {

    // Input
    val testMarketId = "testMarketId"
    val testCancelInstruction = new CancelInstruction("testBetId")
    val testCustomerRef = Some("testCustomerRef")

    // Expected Call
    val params = mutable.HashMap[String, Object](
      "marketId" -> testMarketId,
      "instructions" -> Set(testCancelInstruction),
      "customerRef" -> testCustomerRef
    )
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/cancelOrders", params = params)

    // Expected Response
    val testCancelInstructionReport = new CancelInstructionReport(InstructionReportStatus.FAILURE, Some(InstructionReportErrorCode.BET_IN_PROGRESS),
      testCancelInstruction, Some(1), Some(new DateTime()))
    val testCancelExecutionReport = new CancelExecutionReport(ExecutionReportStatus.SUCCESS, "testMarketId", Some(ExecutionReportErrorCode.BET_ACTION_ERROR),
      Set(testCancelInstructionReport), Some("testCustomerRef"))
    val testCancelExecutionReportContainer = new CancelExecutionReportContainer(testCancelExecutionReport)

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[CancelExecutionReportContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testCancelExecutionReportContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.cancelOrders(sessionToken, testMarketId, Set(testCancelInstruction), testCustomerRef)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result should be theSameInstanceAs testCancelExecutionReport
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "replace orders based on supplied params" in {

    // Input
    val testMarketId = "testMarketId"
    val testReplaceInstruction = new ReplaceInstruction("testBetId", 1)
    val testCustomerRef = Some("testCustomerRef")

    // Expected Call
    val params = mutable.HashMap[String, Object](
      "marketId" -> testMarketId,
      "instructions" -> Set(testReplaceInstruction),
      "customerRef" -> testCustomerRef
    )
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/replaceOrders", params = params)

    // Expected Response
    val testCancelInstruction = new CancelInstruction("testBetId")
    val testPlaceInstruction = new PlaceInstruction(OrderType.LIMIT, 1, 2, Side.BACK)
    val testCancelInstructionReport = new CancelInstructionReport(InstructionReportStatus.FAILURE, Some(InstructionReportErrorCode.BET_IN_PROGRESS),
      testCancelInstruction, Some(1), Some(new DateTime()))
    val testPlaceInstructionReport = new PlaceInstructionReport(InstructionReportStatus.SUCCESS, Some(InstructionReportErrorCode.BET_IN_PROGRESS),
      testPlaceInstruction, Some("testBetId"), Some(new DateTime()), Some(1), Some(2))
    val testReplaceInstructionReport = new ReplaceInstructionReport(InstructionReportStatus.FAILURE, Some(InstructionReportErrorCode.BET_IN_PROGRESS),
      testCancelInstructionReport, testPlaceInstructionReport)
    val testReplaceExecutionReport = new ReplaceExecutionReport(testCustomerRef, ExecutionReportStatus.SUCCESS, Some(ExecutionReportErrorCode.BET_ACTION_ERROR),
      "testMarketId", Set(testReplaceInstructionReport))
    val testReplaceExecutionReportContainer = new ReplaceExecutionReportContainer(testReplaceExecutionReport)

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[ReplaceExecutionReportContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testReplaceExecutionReportContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.replaceOrders(sessionToken, testMarketId, Set(testReplaceInstruction), testCustomerRef)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result should be theSameInstanceAs testReplaceExecutionReport
        case _ =>
          fail()
      }
    }
  }

  "The BetfairServiceNGCommand" should "update orders based on supplied params" in {

    // Input
    val testMarketId = "testMarketId"
    val testUpdateInstruction = new UpdateInstruction("testBetId", PersistenceType.LAPSE)
    val testCustomerRef = Some("testCustomerRef")

    // Expected Call
    val params = mutable.HashMap[String, Object](
      "marketId" -> testMarketId,
      "instructions" -> Set(testUpdateInstruction),
      "customerRef" -> testCustomerRef
    )
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/updateOrders", params = params)

    // Expected Response
    val testUpdateInstructionReport = new UpdateInstructionReport(InstructionReportStatus.FAILURE, Some(InstructionReportErrorCode.BET_IN_PROGRESS), testUpdateInstruction)
    val testUpdateExecutionReport = new UpdateExecutionReport(ExecutionReportStatus.FAILURE, "testMarketId", Some(ExecutionReportErrorCode.BET_ACTION_ERROR),
      Set(testUpdateInstructionReport),Some("testCustomerRef"))
    val testUpdateExecutionReportContainer = new UpdateExecutionReportContainer(testUpdateExecutionReport)

    // Mock
    (testCommand.makeAPIRequest(_: String, _: JsonrpcRequest)(_: FromResponseUnmarshaller[UpdateExecutionReportContainer]))
      .expects(sessionToken, request, *).returning(Future.successful(Some(testUpdateExecutionReportContainer)))

    val testService = new BetfairServiceNG(testConfig, testCommand)

    val result = testService.updateOrders(sessionToken, testMarketId, Set(testUpdateInstruction), testCustomerRef)

    whenReady(result) { res =>
      res match {
        case Some(container) =>
          container.result should be theSameInstanceAs testUpdateExecutionReport
        case _ =>
          fail()
      }
    }
  }
}