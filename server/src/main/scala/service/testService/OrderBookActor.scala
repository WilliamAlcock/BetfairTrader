package service.testService

import akka.actor.Actor
import domain.ExecutionReportErrorCode.ExecutionReportErrorCode
import domain.ExecutionReportStatus.ExecutionReportStatus
import domain._
import service.BetfairServiceNGException
import service.testService.BetfairServiceNGOrderBook._

/**
 * Created by Alcock on 25/10/2015.
 */

class BetfairServiceNGOrderBook(orderBook: OrderBook) extends Actor {

  private def getExecutionReportStatus[T <: InstructionReport](reports: Set[T]): ExecutionReportStatus = {
    reports.count(x => x.status != Some(InstructionReportStatus.SUCCESS)) match {
      case 0 => ExecutionReportStatus.FAILURE
      case x => if (x == reports.size) ExecutionReportStatus.SUCCESS else ExecutionReportStatus.PROCESSED_WITH_ERRORS
    }
  }

  private def getExecutionReportErrorCode[T <: InstructionReport](reports: Set[T]): Option[ExecutionReportErrorCode] = {
    reports.count(x => x.errorCode != None) match {
      case 0 => None
      case x => Some(ExecutionReportErrorCode.PROCESSED_WITH_ERRORS)
    }
  }

  private def getPlaceExecutionReport(marketId: String,
                                      reports: Set[PlaceInstructionReport],
                                      customerRef: Option[String]): PlaceExecutionReport = {
    new PlaceExecutionReport(
      ExecutionReportStatus.SUCCESS,                  // Assume all orders are successful
      marketId,
      None,                                           // As we are assuming all orders are successful there will be no error codes
      reports,
      customerRef
    )
  }

  private def getCancelExecutionReport(marketId: String,
                                       reports: Set[CancelInstructionReport],
                                       customerRef: Option[String]): CancelExecutionReport = {
    new CancelExecutionReport(
      getExecutionReportStatus(reports),
      marketId,
      getExecutionReportErrorCode(reports),
      reports,
      customerRef
    )
  }

  private def getReplaceExecutionReport(marketId: String,
                                        reports: Set[ReplaceInstructionReport],
                                        customerRef: Option[String]): ReplaceExecutionReport = {
    new ReplaceExecutionReport(
      customerRef,
      getExecutionReportStatus(reports),
      getExecutionReportErrorCode(reports),
      marketId,
      reports
    )
  }

  private def fillRunnerOrders(marketId: String, runner: Runner): Runner = {
    new Runner (
      runner.selectionId,
      runner.handicap,
      runner.status,
      runner.adjustmentFactor,
      runner.lastPriceTraded,
      runner.totalMatched,
      runner.removalDate,
      runner.sp,
      runner.ex,
      runner.ex match {
        case Some(ex) =>
          orderBook.fillOrders(
            marketId,
            runner.selectionId,
            runner.ex.get.availableToBack.head,
            runner.ex.get.availableToLay.head
          )
        case None =>
          runner.orders
      },
      runner.matches
    )
  }

  private def fillMarketOrders(marketBook: MarketBook): MarketBook = {
    // TODO update lastmatch time, total Matched etc
    new MarketBook (
      marketBook.marketId,
      marketBook.isMarketDataDelayed,
      marketBook.status,
      marketBook.betDelay,
      marketBook.bspReconciled,
      marketBook.complete,
      marketBook.inplay,
      marketBook.numberOfWinners,
      marketBook.numberOfRunners,
      marketBook.numberOfActiveRunners,
      marketBook.lastMatchTime,
      marketBook.totalMatched,
      marketBook.totalAvailable,
      marketBook.crossMatching,
      marketBook.runnersVoidable,
      marketBook.version,
      marketBook.runners.map(runner => fillRunnerOrders(marketBook.marketId, runner))
    )
  }

  override def receive = {
    case placeOrders(marketId, instructions, customerRef) =>
      sender() ! getPlaceExecutionReport(
        marketId,
        instructions.map{instruction => orderBook.placeOrder(marketId, instruction)},
        customerRef
      )
    case cancelOrders(marketId, instructions, customerRef) =>
      sender() ! getCancelExecutionReport(
        marketId,
        instructions.map{instruction => orderBook.cancelOrder(marketId, instruction)},
        customerRef
      )
    case replaceOrders(marketId, instructions, customerRef) =>
      sender() ! getReplaceExecutionReport(
        marketId,
        instructions.map{instruction => orderBook.replaceOrder(marketId, instruction)},
        customerRef
      )
    case updateOrders(marketId, instructions, customerRef) =>
      // Update orders and return the execution report to the sender
      // TODO add implementation
      throw new Exception("Implementation pending")
      //sender() ! getUpdateExecutionReport(marketId, instructions.map{instruction => updateOrder(marketId, instruction)}, customerRef)
    case fillOrders(marketBooks) =>
      // Fill any orders that have been traded past in above markets
      sender() ! marketBooks.map(marketBook => fillMarketOrders(marketBook))
      throw new Exception("Implementation pending")
    case _ =>
      throw new BetfairServiceNGException("Invalid call to BetfairServiceNGOrderBook")
  }
}

object BetfairServiceNGOrderBook {
  case class placeOrders(marketId: String, instructions: Set[PlaceInstruction], customerRef: Option[String] = None)
  case class cancelOrders(marketId: String, instructions: Set[CancelInstruction], customerRef: Option[String] = None)
  case class replaceOrders(marketId: String, instructions: Set[ReplaceInstruction], customerRef: Option[String] = None)
  case class updateOrders(marketId: String, instructions: Set[UpdateInstruction], customerRef: Option[String] = None)
  case class fillOrders(marketBooks: List[MarketBook])
}