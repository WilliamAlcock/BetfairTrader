package core.dataStore.marketScraper

import scala.language.postfixOps

//class MarketScraperSpec extends TestKit(ActorSystem("TestSystem")) with FlatSpecLike with Matchers with MockFactory with BeforeAndAfterEach with BeforeAndAfterAll {
//
//  var mockDBIO: DBIO = _
//  var mockController: TestProbe = _
//  var time: VirtualTime = _
//
//  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-ddHH:mm:ss") //.withZone(DateTimeZone.UTC)
//
//  override def afterAll = {
//    system.shutdown()
//  }
//
//  override def beforeEach = {
//    mockDBIO = mock[DBIO]
//    mockController = TestProbe()
//    time = new VirtualTime
//  }
//
//  def getMockMarketScraper(): TestActorRef[MarketScraper] = TestActorRef(new MarketScraper(mockController.ref) {
//    override val scheduler = time.scheduler
//    override val dbIO = mockDBIO
//  })
//
//  "marketScraper" should "request all events for the day on initialisation and at the beginning of the next day" in {
//
//    // Set the time to 2 seconds before the start of the next day
//    val now = DateTime.parse("2014-01-3023:59:58", dateFormat)
//
//    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
//
//    val marketScraper = getMockMarketScraper()
//
//    mockController.expectMsg(ListEvents(
//      MarketFilter(marketStartTime = Some(TimeRange(Some(DateTime.parse("2014-01-3000:00:00", dateFormat)), Some(DateTime.parse("2014-01-3100:00:00", dateFormat))))),
//      marketScraper
//    ))
//    // advance time 2 seconds to the start of the next day
//    time.advance(2100)
//    mockController.expectMsg(ListEvents(
//      MarketFilter(marketStartTime = Some(TimeRange(Some(DateTime.parse("2014-01-3000:00:00", dateFormat)), Some(DateTime.parse("2014-01-3100:00:00", dateFormat))))),
//      marketScraper
//    ))
//
//    DateTimeUtils.setCurrentMillisSystem()
//  }
//
//  "marketScraper" should "request market catalogues for all the events received" in {
//    val now = DateTime.parse("2014-01-3023:00:00", dateFormat)
//
//    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
//
//    val marketScraper = getMockMarketScraper()
//
//    val events = List(
//      Event("TEST_EVENT_ID_1", "", None, "", None, DateTime.now()),
//      Event("TEST_EVENT_ID_2", "", None, "", None, DateTime.now()),
//      Event("TEST_EVENT_ID_3", "", None, "", None, DateTime.now())
//    )
//
//    // on initialisation
//    mockController.expectMsg(ListEvents(
//      MarketFilter(marketStartTime = Some(TimeRange(Some(DateTime.parse("2014-01-3000:00:00", dateFormat)), Some(DateTime.parse("2014-01-3100:00:00", dateFormat))))),
//      marketScraper
//    ))
//
//    marketScraper ! ListEventResultContainer(events.map(x => EventResult(x, 1)))
//
//    mockController.expectMsg(ListMarketCatalogue(
//      MarketFilter(eventIds = Set("TEST_EVENT_ID_1", "TEST_EVENT_ID_2", "TEST_EVENT_ID_3")),
//      MarketSort.FIRST_TO_START,
//      sender = marketScraper
//    ))
//
//    DateTimeUtils.setCurrentMillisSystem()
//  }
//
//  "marketScraper" should "write all marketCatalogues received with a startTime to the database and cache their startTime" in {
//    val now = DateTime.parse("2014-01-3023:00:00", dateFormat)
//
//    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
//
//    val marketCatalogues = List(
//      MarketCatalogue("TEST_MARKET_CATALOGUE_ID_1", "", Some(now), None, 1.0, None, None, None, Event("", "", None, "", None, DateTime.now())),
//      MarketCatalogue("TEST_MARKET_CATALOGUE_ID_2", "", Some(now), None, 2.0, None, None, None, Event("", "", None, "", None, DateTime.now())),
//      MarketCatalogue("TEST_MARKET_CATALOGUE_ID_3", "", Some(now), None, 3.0, None, None, None, Event("", "", None, "", None, DateTime.now()))
//    )
//
//    val marketScraper = getMockMarketScraper()
//
//    (mockDBIO.writeMarketCatalogue _).expects(marketCatalogues(0))
//    (mockDBIO.writeMarketCatalogue _).expects(marketCatalogues(1))
//    (mockDBIO.writeMarketCatalogue _).expects(marketCatalogues(2))
//
//    marketScraper ! ListMarketCatalogueContainer(marketCatalogues)
//
//    marketScraper.underlyingActor.marketStartTimes should be(HashMap(
//      "TEST_MARKET_CATALOGUE_ID_1" -> now,
//      "TEST_MARKET_CATALOGUE_ID_2" -> now,
//      "TEST_MARKET_CATALOGUE_ID_3" -> now
//    ))
//
//    DateTimeUtils.setCurrentMillisSystem()
//  }
//
//  "marketScraper" should "NOT write marketCatalogues received with NO startTime to the database or cache them" in {
//    val now = DateTime.parse("2014-01-3023:00:00", dateFormat)
//
//    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
//
//    val marketCatalogues = List(
//      MarketCatalogue("TEST_MARKET_CATALOGUE_ID_1", "", None, None, 1.0, None, None, None, Event("", "", None, "", None, DateTime.now())),
//      MarketCatalogue("TEST_MARKET_CATALOGUE_ID_2", "", None, None, 2.0, None, None, None, Event("", "", None, "", None, DateTime.now())),
//      MarketCatalogue("TEST_MARKET_CATALOGUE_ID_3", "", None, None, 3.0, None, None, None, Event("", "", None, "", None, DateTime.now()))
//    )
//
//    val marketScraper = getMockMarketScraper()
//
//    marketScraper ! ListMarketCatalogueContainer(marketCatalogues)
//
//    marketScraper.underlyingActor.marketStartTimes should be(HashMap.empty[String, DateTime])
//
//    DateTimeUtils.setCurrentMillisSystem()
//  }
//
//  "marketScraper" should "schedule scraping for all marketCatalogues with a start time > 20 minutes from now and cache the cancellable object for clean shutdown" in {
//    val now = DateTime.parse("2014-01-3023:00:00", dateFormat)
//
//    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
//
//    val marketStartTimes = List(now.plusMinutes(21), now.plusMinutes(20).plusSeconds(1))
//
//    val marketCatalogues = List(
//      MarketCatalogue("TEST_MARKET_CATALOGUE_ID_1", "", Some(marketStartTimes(0)), None, 1.0, None, None, None, Event("", "", None, "", None, DateTime.now())),
//      MarketCatalogue("TEST_MARKET_CATALOGUE_ID_2", "", Some(marketStartTimes(1)), None, 2.0, None, None, None, Event("", "", None, "", None, DateTime.now()))
//    )
//
//    (mockDBIO.writeMarketCatalogue _).expects(marketCatalogues(0))
//    (mockDBIO.writeMarketCatalogue _).expects(marketCatalogues(1))
//
//    val marketScraper = getMockMarketScraper()
//
//    // on initialisation
//    mockController.expectMsg(ListEvents(
//      MarketFilter(marketStartTime = Some(TimeRange(Some(DateTime.parse("2014-01-3000:00:00", dateFormat)), Some(DateTime.parse("2014-01-3100:00:00", dateFormat))))),
//      marketScraper
//    ))
//
//    marketScraper ! ListMarketCatalogueContainer(marketCatalogues)
//
//    marketScraper.underlyingActor.scheduledScraping("TEST_MARKET_CATALOGUE_ID_1").isCancelled should be(false)
//    marketScraper.underlyingActor.scheduledScraping("TEST_MARKET_CATALOGUE_ID_2").isCancelled should be(false)
//
//    time.advance(1201000) // fast forward 20 minutes 1 second
//    mockController.expectMsg(SubscribeToMarkets(markets = Set("TEST_MARKET_CATALOGUE_ID_2"), BEST_AND_TRADED, marketScraper))
//    marketScraper.underlyingActor.scheduledScraping.get("TEST_MARKET_CATALOGUE_ID_2").isDefined should be(false)
//
//    time.advance(59000)
//    mockController.expectMsg(SubscribeToMarkets(markets = Set("TEST_MARKET_CATALOGUE_ID_1"), BEST_AND_TRADED, marketScraper))
//    marketScraper.underlyingActor.scheduledScraping.get("TEST_MARKET_CATALOGUE_ID_1").isDefined should be(false)
//
//    DateTimeUtils.setCurrentMillisSystem()
//  }
//
//  "marketScraper" should "start scraping for all marketCatalogues with a (0 < start time < 20) from now" in {
//    val now = DateTime.parse("2014-01-3023:00:00", dateFormat)
//
//    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
//
//    val marketStartTimes = List(now.plusMinutes(10), now.plusMinutes(19), now.minusMinutes(10))
//
//    val marketCatalogues = List(
//      MarketCatalogue("TEST_MARKET_CATALOGUE_ID_1", "", Some(marketStartTimes(0)), None, 1.0, None, None, None, Event("", "", None, "", None, DateTime.now())),
//      MarketCatalogue("TEST_MARKET_CATALOGUE_ID_2", "", Some(marketStartTimes(1)), None, 2.0, None, None, None, Event("", "", None, "", None, DateTime.now())),
//      MarketCatalogue("TEST_MARKET_CATALOGUE_ID_3", "", Some(marketStartTimes(2)), None, 3.0, None, None, None, Event("", "", None, "", None, DateTime.now()))
//    )
//
//    // should save the marketCatalogues
//    (mockDBIO.writeMarketCatalogue _).expects(marketCatalogues(0))
//    (mockDBIO.writeMarketCatalogue _).expects(marketCatalogues(1))
//    (mockDBIO.writeMarketCatalogue _).expects(marketCatalogues(2))
//
//    val marketScraper = getMockMarketScraper()
//
//    // on initialisation
//    mockController.expectMsg(ListEvents(
//      MarketFilter(marketStartTime = Some(TimeRange(Some(DateTime.parse("2014-01-3000:00:00", dateFormat)), Some(DateTime.parse("2014-01-3100:00:00", dateFormat))))),
//      marketScraper
//    ))
//
//    marketScraper ! ListMarketCatalogueContainer(marketCatalogues)
//
//    marketScraper.underlyingActor.scheduledScraping should be(HashMap.empty[String, Cancellable])
//
//    // but not subscribe to TEST_MARKET_CATALOGUE_ID_3 as it has already started
//    mockController.expectMsg(SubscribeToMarkets(markets = Set("TEST_MARKET_CATALOGUE_ID_1"), BEST_AND_TRADED, marketScraper))
//    mockController.expectMsg(SubscribeToMarkets(markets = Set("TEST_MARKET_CATALOGUE_ID_2"), BEST_AND_TRADED, marketScraper))
//    mockController.expectNoMsg()
//
//    DateTimeUtils.setCurrentMillisSystem()
//  }
//
//  "marketScraper" should "write marketBookUpdate to the database if there is a startTime stored for the market" in {
//    val now = DateTime.parse("2014-01-3023:00:00", dateFormat)
//
//    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
//
//    val marketScraper = getMockMarketScraper()
//
//    val marketBookUpdate = MarketBookUpdate(now.plusMinutes(2), MarketBook("TEST_ID_1", false, "", 1, false, false, false, 1, 1, 1, None, 1, 1, false, false, 1L, Set()))
//
//    marketScraper.underlyingActor.marketStartTimes = HashMap("TEST_ID_1" -> now)
//
//    (mockDBIO.writeMarketBookUpdate _).expects(marketBookUpdate)
//
//    marketScraper ! marketBookUpdate
//
//    DateTimeUtils.setCurrentMillisSystem()
//  }
//
//  "marketScraper" should "NOT write marketBookUpdate to the database if there is NO startTime stored for the market" in {
//    val now = DateTime.parse("2014-01-3023:00:00", dateFormat)
//
//    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
//
//    val marketScraper = getMockMarketScraper()
//
//    val marketBookUpdate = MarketBookUpdate(now.plusMinutes(2), MarketBook("TEST_ID_1", false, "", 1, false, false, false, 1, 1, 1, None, 1, 1, false, false, 1L, Set()))
//
//    marketScraper ! marketBookUpdate
//
//    DateTimeUtils.setCurrentMillisSystem()
//  }
//
//  "marketScraper" should "cancel all scheduled tasks on shutdown" in {
//    import scala.concurrent.ExecutionContext.Implicits.global
//
//    val now = DateTime.parse("2014-01-3023:00:00", dateFormat)
//
//    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
//
//    val marketScraper = getMockMarketScraper()
//
//    val cancellables = List(
//      time.scheduler.scheduleOnce(1 minute)(),
//      time.scheduler.scheduleOnce(2 minutes)()
//    )
//
//    val initDayCancellable = marketScraper.underlyingActor.initDayCancel
//
//    marketScraper.underlyingActor.scheduledScraping = HashMap(
//      "TASK_1" -> cancellables(0),
//      "TASK_2" -> cancellables(1)
//    )
//
//    marketScraper ! PoisonPill
//
//    cancellables(0).isCancelled should be(true)
//    cancellables(1).isCancelled should be(true)
//    initDayCancellable.isCancelled should be(true)
//
//    DateTimeUtils.setCurrentMillisSystem()
//  }
//}
//
