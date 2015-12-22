package service

import akka.actor.ActorSystem
import domain.BetStatus.BetStatus
import domain.GroupBy.GroupBy
import domain.MarketProjection.MarketProjection
import domain.MarketSort.MarketSort
import domain.MatchProjection.MatchProjection
import domain.OrderBy.OrderBy
import domain.OrderProjection.OrderProjection
import domain.Side.Side
import domain.SortDir.SortDir
import domain.TimeGranularity.TimeGranularity
import domain._
import server.Configuration

import scala.collection.mutable.HashMap
import scala.concurrent._
import scala.language.postfixOps

class BetfairServiceNG(val config: Configuration, command: BetfairServiceNGCommand)
                      (implicit executionContext: ExecutionContext, system: ActorSystem) extends BetfairService{


  def login(): Future[Option[LoginResponse]] = {

    import spray.httpx.PlayJsonSupport._

    val request = LoginRequest(config.username, config.password)
    command.makeLoginRequest(request)
  }

  def logout(sessionToken: String) {

    import spray.httpx.PlayJsonSupport._

    command.makeLogoutRequest(sessionToken)
  }

  def getNavigationData(sessionToken: String): Future[String] = command.makeNavigationDataRequest(sessionToken)

  def listCompetitions(sessionToken: String, marketFilter: MarketFilter): Future[Option[ListCompetitionsContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("filter" -> marketFilter)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listCompetitions", params = params)
    command.makeAPIRequest[ListCompetitionsContainer](sessionToken, request)
  }

  def listCountries(sessionToken: String, marketFilter: MarketFilter): Future[Option[ListCountriesContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("filter" -> marketFilter)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listCountries", params = params)
    command.makeAPIRequest[ListCountriesContainer](sessionToken, request)
  }

  def listCurrentOrders(sessionToken: String,
                        betIds: Option[(String, Set[String])] = None,
                        marketIds: Option[(String, Set[String])] = None,
                        orderProjection: Option[(String, OrderProjection)] = None,
                        placedDateRange: Option[(String, TimeRange)] = None,
                        dateRange: Option[(String, TimeRange)] = None,
                        orderBy: Option[(String, OrderBy)] = None,
                        sortDir: Option[(String, SortDir)] = None,
                        fromRecord: Option[(String, Integer)] = None,
                        recordCount: Option[(String, Integer)] = None): Future[Option[ListCurrentOrdersContainer]] = {

    import spray.httpx.PlayJsonSupport._

    // this simplifies the json serialisation of the Options when in the params HashMap
    val flattenedOpts = Seq(betIds, marketIds, orderProjection, placedDateRange, dateRange,
      orderBy, sortDir, fromRecord, recordCount).flatten

    val params = HashMap[String, Object]()

    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listCurrentOrders",
      params = params ++ flattenedOpts.map(i => i._1 -> i._2).toMap)
    command.makeAPIRequest[ListCurrentOrdersContainer](sessionToken, request)
  }

  def listClearedOrders(sessionToken: String,
                        betStatus: Option[(String, BetStatus)] = None,
                        eventTypeIds: Option[(String, Set[String])] = None,
                        eventIds: Option[(String, Set[String])] = None,
                        marketIds: Option[(String, Set[String])] = None,
                        runnerIds: Option[(String, Set[String])] = None,
                        betIds: Option[(String, Set[String])] = None,
                        side: Option[(String, Side)] = None,
                        settledDateRange: Option[(String, TimeRange)] = None,
                        groupBy: Option[(String, GroupBy)] = None,
                        includeItemDescription: Option[Boolean] = None,
                        fromRecord: Option[(String, Integer)] = None,
                        recordCount: Option[(String, Integer)] = None): Future[Option[ListClearedOrdersContainer]] = {

    import spray.httpx.PlayJsonSupport._

    // this simplifies the json serialisation of the Options when in the params HashMap
    val flattenedOpts = Seq(betStatus, eventTypeIds, eventIds, marketIds, runnerIds, betIds,
      side, settledDateRange, groupBy, fromRecord, recordCount).flatten

    val params = HashMap[String, Object]("includeItemDescription" -> includeItemDescription)

    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listClearedOrders",
      params = params ++ flattenedOpts.map(i => i._1 -> i._2).toMap)
    command.makeAPIRequest[ListClearedOrdersContainer](sessionToken, request)
  }

  def listEvents(sessionToken: String, marketFilter: MarketFilter): Future[Option[ListEventResultContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("filter" -> marketFilter)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listEvents", params = params)
    command.makeAPIRequest[ListEventResultContainer](sessionToken, request)
  }

  def listEventTypes(sessionToken: String, marketFilter: MarketFilter): Future[Option[ListEventTypeResultContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("filter" -> marketFilter)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listEventTypes", params = params)
    command.makeAPIRequest[ListEventTypeResultContainer](sessionToken, request)
  }

  def listMarketBook(sessionToken: String,
                     marketIds: Set[String],
                     priceProjection: Option[(String, PriceProjection)] = None,
                     orderProjection: Option[(String, OrderProjection)] = None,
                     matchProjection: Option[(String, MatchProjection)] = None,
                     currencyCode: Option[(String, String)] = None): Future[Option[ListMarketBookContainer]] = {

    import spray.httpx.PlayJsonSupport._

    // this simplifies the json serialisation of the Options when in the params HashMap
    val flattenedOpts = Seq(priceProjection, orderProjection, matchProjection, currencyCode).flatten

    val params = HashMap[String, Object]("marketIds" -> marketIds)

    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listMarketBook",
      params = params ++ flattenedOpts.map(i => i._1 -> i._2).toMap)
    command.makeAPIRequest[ListMarketBookContainer](sessionToken, request)
  }

  def listMarketCatalogue(sessionToken: String,
                          marketFilter: MarketFilter,
                          marketProjection: List[MarketProjection],
                          sort: MarketSort,
                          maxResults: Integer): Future[Option[ListMarketCatalogueContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("filter" -> marketFilter, "marketProjection" -> marketProjection,
      "sort" -> sort, "maxResults" -> maxResults)
    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listMarketCatalogue", params = params)
    command.makeAPIRequest[ListMarketCatalogueContainer](sessionToken, request)
  }

  def listMarketProfitAndLoss(sessionToken: String,
                              marketIds: Set[String],
                              includeSettledBets: Option[Boolean] = None,
                              includeBspBets: Option[Boolean] = None,
                              netOfCommission: Option[Boolean] = None): Future[Option[MarketProfitAndLossContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("marketIds" -> marketIds, "includeSettledBets" -> includeSettledBets,
                                          "includeBspBets" -> includeBspBets, "netOfCommission" -> includeBspBets)

    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listMarketProfitAndLoss", params = params)
    command.makeAPIRequest[MarketProfitAndLossContainer](sessionToken, request)
  }

  def listMarketTypes(sessionToken: String, marketFilter: MarketFilter): Future[Option[MarketTypeResultContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("filter" -> marketFilter)

    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listMarketTypes", params = params)
    command.makeAPIRequest[MarketTypeResultContainer](sessionToken, request)
  }

  def listTimeRanges(sessionToken: String, marketFilter: MarketFilter, granularity: TimeGranularity): Future[Option[TimeRangeResultContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("filter" -> marketFilter, "granularity" -> granularity)

    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listTimeRanges", params = params)
    command.makeAPIRequest[TimeRangeResultContainer](sessionToken, request)
  }

  def listVenues(sessionToken: String, marketFilter: MarketFilter): Future[Option[VenueResultContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("filter" -> marketFilter)

    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listVenues", params = params)
    command.makeAPIRequest[VenueResultContainer](sessionToken, request)
  }

  def placeOrders(sessionToken: String,
                  marketId: String,
                  instructions: Set[PlaceInstruction],
                  customerRef: Option[String] = None): Future[Option[PlaceExecutionReportContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("marketId" -> marketId, "instructions" -> instructions, "customerRef" -> customerRef)

    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/placeOrders", params = params)
    command.makeAPIRequest[PlaceExecutionReportContainer](sessionToken, request)
  }

  def cancelOrders(sessionToken: String,
                   marketId: String,
                   instructions: Set[CancelInstruction],
                   customerRef: Option[String] = None): Future[Option[CancelExecutionReportContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("marketId" -> marketId, "instructions" -> instructions, "customerRef" -> customerRef)

    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/cancelOrders", params = params)
    command.makeAPIRequest[CancelExecutionReportContainer](sessionToken, request)
  }

  def replaceOrders(sessionToken: String,
                    marketId: String,
                    instructions: Set[ReplaceInstruction],
                    customerRef: Option[String] = None): Future[Option[ReplaceExecutionReportContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("marketId" -> marketId, "instructions" -> instructions, "customerRef" -> customerRef)

    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/replaceOrders", params = params)
    command.makeAPIRequest[ReplaceExecutionReportContainer](sessionToken, request)
  }

  def updateOrders(sessionToken: String,
                   marketId: String,
                   instructions: Set[UpdateInstruction],
                   customerRef: Option[String] = None): Future[Option[UpdateExecutionReportContainer]] = {

    import spray.httpx.PlayJsonSupport._

    val params = HashMap[String, Object]("marketId" -> marketId, "instructions" -> instructions, "customerRef" -> customerRef)

    val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/updateOrders", params = params)
    command.makeAPIRequest[UpdateExecutionReportContainer](sessionToken, request)
  }
}