package service.testService

import akka.actor.{ActorSystem, ActorRef}
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
import service._
import service.testService.BetfairServiceNGOrderBook.fillOrders
import service.{BetfairServiceNGException, BetfairServiceNG, BetfairServiceNGCommand}

import scala.concurrent.{Promise, Future, ExecutionContext}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
 * Created by Alcock on 23/10/2015.
 */

class BetfairServiceNGTest(config: Configuration, command: BetfairServiceNGCommand, orderBook: ActorRef)
                          (implicit executionContext: ExecutionContext, system: ActorSystem)
  extends BetfairServiceNG(config, command) {

  implicit val timeout = Timeout(5 seconds)

  // ***** Override functions *****

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
    throw new Exception("Implementation pending")
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
    throw new Exception("Implementation pending")
  }

  override def listMarketBook(sessionToken: String,
                              marketIds: Set[String],
                              priceProjection: Option[(String, PriceProjection)] = None,
                              orderProjection: Option[(String, OrderProjection)] = None,
                              matchProjection: Option[(String, MatchProjection)] = None,
                              currencyCode: Option[(String, String)] = None): Future[Option[ListMarketBookContainer]] = {

    val promise = Promise[Option[ListMarketBookContainer]]()

    // 1. call listMarketBook on parent object
    super.listMarketBook(sessionToken, marketIds, priceProjection, orderProjection, matchProjection, currencyCode) onComplete {
      case Success(Some(listMarketBookContainer: ListMarketBookContainer)) =>
        orderBook ? fillOrders(listMarketBookContainer.result) onComplete {
          case Success(marketBooks) =>
            promise.success(Some(new ListMarketBookContainer(marketBooks.asInstanceOf[List[MarketBook]])))
          case Failure(error) =>
            promise.failure(error)
          case _ =>
            promise.failure(new BetfairServiceNGException("Unknown failure"))
        }
      case Failure(error) =>
        promise.failure(error)
      case _ =>
        promise.failure(new BetfairServiceNGException("Unknown failure"))
    }

    return promise.future
  }

  override def listMarketProfitAndLoss(sessionToken: String,
                                       marketIds: Set[String],
                                       includeSettledBets: Option[Boolean] = None,
                                       includeBspBets: Option[Boolean] = None,
                                       netOfCommission: Option[Boolean] = None): Future[Option[MarketProfitAndLossContainer]] = {

    // 1. look at filled orders and calculate market profit and loss
    // 2. create response to mimic live market
    // TODO add implementation
    throw new Exception("Implementation pending")
  }

  override def placeOrders(sessionToken: String,
                           marketId: String,
                           instructions: Set[PlaceInstruction],
                           customerRef: Option[String] = None): Future[Option[PlaceExecutionReportContainer]] = {

    // Create a new promise to return as the promise returned from orderBook actor is of type Future[Any]
    val promise = Promise[Option[PlaceExecutionReportContainer]]()

    orderBook ? BetfairServiceNGOrderBook.placeOrders(marketId, instructions, customerRef) onComplete {
      case Success(placeExecutionReport: PlaceExecutionReport) =>
        promise.success(Some(new PlaceExecutionReportContainer(placeExecutionReport)))
      case Failure(error) =>
        promise.failure(error)
      case _ =>
        promise.failure(new BetfairServiceNGException("Unknown failure"))
    }

    return promise.future
  }

  override def cancelOrders(sessionToken: String,
                            marketId: String,
                            instructions: Set[CancelInstruction],
                            customerRef: Option[String] = None): Future[Option[CancelExecutionReportContainer]] = {

    val promise = Promise[Option[CancelExecutionReportContainer]]()

    orderBook ? testService.BetfairServiceNGOrderBook.cancelOrders(marketId, instructions, customerRef) onComplete {
      case Success(cancelExecutionReport: CancelExecutionReport) =>
        promise.success(Some(new CancelExecutionReportContainer(cancelExecutionReport)))
      case Failure(error) =>
        promise.failure(error)
      case _ =>
        promise.failure(new BetfairServiceNGException("Unknown failure"))
    }

    return promise.future
  }

  override def replaceOrders(sessionToken: String,
                             marketId: String,
                             instructions: Set[ReplaceInstruction],
                             customerRef: Option[String]): Future[Option[ReplaceExecutionReportContainer]] = {

    val promise = Promise[Option[ReplaceExecutionReportContainer]]()

    orderBook ? testService.BetfairServiceNGOrderBook.replaceOrders(marketId, instructions, customerRef) onComplete {
      case Success(replaceExecutionReport: ReplaceExecutionReport) =>
        promise.success(Some(new ReplaceExecutionReportContainer(replaceExecutionReport)))
      case Failure(error) =>
        promise.failure(error)
      case _ =>
        promise.failure(new BetfairServiceNGException("Unknown failure"))
    }

    return promise.future
  }

  override def updateOrders(sessionToken: String,
                            marketId: String,
                            instructions: Set[UpdateInstruction],
                            customerRef: Option[String]): Future[Option[UpdateExecutionReportContainer]] = {

    // TODO add implementation
    throw new Exception("Implementation pending")
  }
}
