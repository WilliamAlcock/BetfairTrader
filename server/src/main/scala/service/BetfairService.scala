package service

import domain.BetStatus._
import domain.GroupBy._
import domain.MarketProjection._
import domain.MarketSort._
import domain.MatchProjection._
import domain.OrderBy._
import domain.Side._
import domain.SortDir._
import domain.TimeGranularity._
import domain._
import domain.OrderProjection._

import scala.concurrent.Future

trait BetfairService {
  def login(): Future[Option[LoginResponse]]

  def logout(sessionToken: String)

  def getNavigationData(sessionToken: String): Future[String]

  def listCompetitions(sessionToken: String, marketFilter: MarketFilter): Future[Option[ListCompetitionsContainer]]

  def listCountries(sessionToken: String, marketFilter: MarketFilter): Future[Option[ListCountriesContainer]]

  def listCurrentOrders(sessionToken: String,
                        betIds: Option[(String, Set[String])] = None,
                        marketIds: Option[(String, Set[String])] = None,
                        orderProjection: Option[(String, OrderProjection)] = None,
                        placedDateRange: Option[(String, TimeRange)] = None,
                        dateRange: Option[(String, TimeRange)] = None,
                        orderBy: Option[(String, OrderBy)] = None,
                        sortDir: Option[(String, SortDir)] = None,
                        fromRecord: Option[(String, Integer)] = None,
                        recordCount: Option[(String, Integer)] = None): Future[Option[ListCurrentOrdersContainer]]

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
                        recordCount: Option[(String, Integer)] = None): Future[Option[ListClearedOrdersContainer]]

  def listEvents(sessionToken: String, marketFilter: MarketFilter): Future[Option[ListEventResultContainer]]

  def listEventTypes(sessionToken: String, marketFilter: MarketFilter): Future[Option[ListEventTypeResultContainer]]

  def listMarketBook(sessionToken: String,
                     marketIds: Set[String],
                     priceProjection: Option[(String, PriceProjection)] = None,
                     orderProjection: Option[(String, OrderProjection)] = None,
                     matchProjection: Option[(String, MatchProjection)] = None,
                     currencyCode: Option[(String, String)] = None): Future[Option[ListMarketBookContainer]]

  def listMarketCatalogue(sessionToken: String,
                          marketFilter: MarketFilter,
                          marketProjection: List[MarketProjection],
                          sort: MarketSort,
                          maxResults: Integer): Future[Option[ListMarketCatalogueContainer]]

  def listMarketProfitAndLoss(sessionToken: String,
                              marketIds: Set[String],
                              includeSettledBets: Option[Boolean] = None,
                              includeBspBets: Option[Boolean] = None,
                              netOfCommission: Option[Boolean] = None): Future[Option[MarketProfitAndLossContainer]]

  def listMarketTypes(sessionToken: String, marketFilter: MarketFilter): Future[Option[MarketTypeResultContainer]]

  def listTimeRanges(sessionToken: String, marketFilter: MarketFilter, granularity: TimeGranularity): Future[Option[TimeRangeResultContainer]]

  def listVenues(sessionToken: String, marketFilter: MarketFilter): Future[Option[VenueResultContainer]]

  def placeOrders(sessionToken: String,
                  marketId: String,
                  instructions: Set[PlaceInstruction],
                  customerRef: Option[String] = None): Future[Option[PlaceExecutionReportContainer]]

  def cancelOrders(sessionToken: String,
                   marketId: String,
                   instructions: Set[CancelInstruction],
                   customerRef: Option[String] = None): Future[Option[CancelExecutionReportContainer]]

  def replaceOrders(sessionToken: String,
                    marketId: String,
                    instructions: Set[ReplaceInstruction],
                    customerRef: Option[String] = None): Future[Option[ReplaceExecutionReportContainer]]

  def updateOrders(sessionToken: String,
                   marketId: String,
                   instructions: Set[UpdateInstruction],
                   customerRef: Option[String] = None): Future[Option[UpdateExecutionReportContainer]]
}

