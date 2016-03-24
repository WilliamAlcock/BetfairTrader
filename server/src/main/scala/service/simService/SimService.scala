package service.simService

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import domain.BetStatus.BetStatus
import domain.GroupBy.GroupBy
import domain.MatchProjection.MatchProjection
import domain.OrderBy.OrderBy
import domain.OrderProjection.OrderProjection
import domain.Side.Side
import domain.SortDir.SortDir
import domain._
import server.Configuration
import service.simService.SimOrderBook._
import service.{BetfairService, BetfairServiceNGCommand}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.postfixOps
import scala.util.Success

class SimService(val config: Configuration, val command: BetfairServiceNGCommand, orderBook: ActorRef)
                          (implicit executionContext: ExecutionContext, system: ActorSystem) extends BetfairService {

  implicit val timeout = Timeout(5 seconds)

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
    val _betIds = if (betIds.isDefined) betIds.get._2.toSeq else Seq.empty[String]
    val _marketIds = if (marketIds.isDefined) marketIds.get._2.toSeq else Seq.empty[String]

    val promise = Promise[Option[ListCurrentOrdersContainer]]()
    (orderBook ? GetOrders(_betIds, _marketIds, OrderStatus.EXECUTABLE)) onComplete {
      case Success(x: Set[CurrentOrderSummary]) => promise.success(Some(ListCurrentOrdersContainer(CurrentOrderSummaryReport(x, moreAvailable = false))))
      case _ => promise.failure(SimException("getOrders call to SimOrderBook has failed"))
    }
    promise.future
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

  override def listMarketBook(sessionToken: String,
                              marketIds: Set[String],
                              priceProjection: Option[(String, PriceProjection)] = None,
                              orderProjection: Option[(String, OrderProjection)] = None,
                              matchProjection: Option[(String, MatchProjection)] = None,
                              currencyCode: Option[(String, String)] = None): Future[Option[ListMarketBookContainer]] = {

    val promise = Promise[Option[ListMarketBookContainer]]()
    super.listMarketBook(sessionToken, marketIds, priceProjection, orderProjection, matchProjection, currencyCode) onComplete {
      case Success(Some(x: ListMarketBookContainer)) => promise.completeWith(ask(orderBook, MatchOrders(x, config.orderProjection)).mapTo[Option[ListMarketBookContainer]])
      case _ => promise.failure(SimException("listMarketBook to BetfairService failed"))
    }
    promise.future
  }

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
