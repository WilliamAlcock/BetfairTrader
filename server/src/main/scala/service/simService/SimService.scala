package service.simService

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import domain.BetStatus.BetStatus
import domain.GroupBy.GroupBy
import domain.MarketProjection._
import domain.MarketSort._
import domain.MatchProjection.MatchProjection
import domain.OrderBy.OrderBy
import domain.OrderProjection.OrderProjection
import domain.Side.Side
import domain.SortDir.SortDir
import domain.TimeGranularity._
import domain._
import server.Configuration
import service.simService.SimOrderBook._
import service.{BetfairService, BetfairServiceNG}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.postfixOps
import scala.util.Success

/**
 * Created by Alcock on 23/10/2015.
 */

class SimService(config: Configuration, service: BetfairServiceNG, orderBook: ActorRef)
                          (implicit executionContext: ExecutionContext, system: ActorSystem) extends BetfairService {

  implicit val timeout = Timeout(5 seconds)

  override def login() = service.login()
  override def logout(sessionToken: String) = service.logout(sessionToken)
  override def getNavigationData(sessionToken: String) = service.getNavigationData(sessionToken)
  override def listCompetitions(sessionToken: String, marketFilter: MarketFilter) = service.listCompetitions(sessionToken, marketFilter)
  override def listCountries(sessionToken: String, marketFilter: MarketFilter) = service.listCountries(sessionToken, marketFilter)

  override def listCurrentOrders(sessionToken: String,
                        betIds: Option[(String, Set[String])] = None,
                        marketIds: Option[(String, Set[String])] = None,
                        orderProjection: Option[(String, OrderProjection)] = None,
                        placedDateRange: Option[(String, TimeRange)] = None,
                        dateRange: Option[(String, TimeRange)] = None,
                        orderBy: Option[(String, OrderBy)] = None,
                        sortDir: Option[(String, SortDir)] = None,
                        fromRecord: Option[(String, Integer)] = None,
                        recordCount: Option[(String, Integer)] = None): Future[Option[ListCurrentOrdersContainer]] = {

    // 2. return current orders in format to mimic live market
    // TODO add implementation
    throw new NotImplementedError()
  }

  override def listClearedOrders(sessionToken: String,
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

    // 2. return cleared orders in format to mimic live market
    // TODO add implementation
    throw new NotImplementedError()
  }

  override def listEvents(sessionToken: String, marketFilter: MarketFilter) = service.listEvents(sessionToken, marketFilter)

  override def listEventTypes(sessionToken: String, marketFilter: MarketFilter) = service.listEventTypes(sessionToken, marketFilter)


  override def listMarketBook(sessionToken: String,
                              marketIds: Set[String],
                              priceProjection: Option[(String, PriceProjection)] = None,
                              orderProjection: Option[(String, OrderProjection)] = None,
                              matchProjection: Option[(String, MatchProjection)] = None,
                              currencyCode: Option[(String, String)] = None): Future[Option[ListMarketBookContainer]] = {

    val promise = Promise[Option[ListMarketBookContainer]]
    service.listMarketBook(sessionToken, marketIds, priceProjection, orderProjection, matchProjection, currencyCode) onComplete {
      case Success(Some(x: ListMarketBookContainer)) => promise.completeWith(ask(orderBook, MatchOrders(x)).mapTo[Option[ListMarketBookContainer]])
      case _ => promise.failure(SimException("listMarketBook to BetfairService failed"))
    }
    promise.future
  }

  override def listMarketCatalogue(sessionToken: String,
                                  marketFilter: MarketFilter,
                                  marketProjection: List[MarketProjection],
                                  sort: MarketSort,
                                  maxResults: Integer) = service.listMarketCatalogue(sessionToken, marketFilter, marketProjection, sort, maxResults)


  override def listMarketProfitAndLoss(sessionToken: String,
                                       marketIds: Set[String],
                                       includeSettledBets: Option[Boolean] = None,
                                       includeBspBets: Option[Boolean] = None,
                                       netOfCommission: Option[Boolean] = None): Future[Option[MarketProfitAndLossContainer]] = {

    // 1. look at filled orders and calculate market profit and loss
    // 2. create response to mimic live market
    // TODO add implementation
    throw new NotImplementedError()
  }

  override def listMarketTypes(sessionToken: String, marketFilter: MarketFilter) = service.listMarketTypes(sessionToken, marketFilter)

  override def listTimeRanges(sessionToken: String, marketFilter: MarketFilter, granularity: TimeGranularity) = service.listTimeRanges(sessionToken, marketFilter, granularity)

  override def listVenues(sessionToken: String, marketFilter: MarketFilter) = service.listVenues(sessionToken, marketFilter)

  override def placeOrders(sessionToken: String,
                         marketId: String,
                         instructions: Set[PlaceInstruction],
                         customerRef: Option[String] = None): Future[Option[PlaceExecutionReportContainer]] = {

    (orderBook ? PlaceOrders(marketId, instructions, customerRef)).map{case x: PlaceExecutionReportContainer => Some(x)}
  }

  override def cancelOrders(sessionToken: String,
                            marketId: String,
                            instructions: Set[CancelInstruction],
                            customerRef: Option[String] = None): Future[Option[CancelExecutionReportContainer]] = {

    (orderBook ? CancelOrders(marketId, instructions, customerRef)).map{case x: CancelExecutionReportContainer => Some(x)}
  }

  override def replaceOrders(sessionToken: String,
                             marketId: String,
                             instructions: Set[ReplaceInstruction],
                             customerRef: Option[String]): Future[Option[ReplaceExecutionReportContainer]] = {
    (orderBook ? ReplaceOrders(marketId, instructions, customerRef)).map{case x: ReplaceExecutionReportContainer => Some(x)}
  }

  override def updateOrders(sessionToken: String,
                            marketId: String,
                            instructions: Set[UpdateInstruction],
                            customerRef: Option[String]): Future[Option[UpdateExecutionReportContainer]] = {

    (orderBook ? UpdateOrders(marketId, instructions, customerRef)).map{case x: UpdateExecutionReportContainer => Some(x)}
  }
}
