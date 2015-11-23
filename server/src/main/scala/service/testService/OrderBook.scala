package service.testService

import domain.InstructionReportStatus.InstructionReportStatus
import domain._
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * Created by Alcock on 25/10/2015.
 */

class OrderBook(betIdCounter: BetIdCounter) {

  private val orders = mutable.HashMap[String, mutable.HashMap[Long, Set[Order]]]()

  def containsMarket(marketId: String): Boolean = orders.contains(marketId)
  def containsSelection(marketId: String, selectionId: Long) : Boolean =
    containsMarket(marketId) & orders(marketId).contains(selectionId)

  def getOrders(marketId: String, selectionId: Long): Set[Order] = orders(marketId)(selectionId)

  private def getOrderSelectionId(marketId: String, betId: String): Option[Long] = {
    orders(marketId).foreach(runner =>
      if (runner._2.exists(x => x.betId == betId)) {
        return Some(runner._1)
      }
    )
    return None
  }

  private def getReplaceInstructionReportStatus(cancelInstructionReport: CancelInstructionReport,
                                                placeInstructionReport: PlaceInstructionReport): InstructionReportStatus = {

    (cancelInstructionReport.status, placeInstructionReport.status) match {
      case (InstructionReportStatus.SUCCESS, InstructionReportStatus.SUCCESS) => InstructionReportStatus.SUCCESS
      case (InstructionReportStatus.TIMEOUT, InstructionReportStatus.TIMEOUT) => InstructionReportStatus.TIMEOUT
      case _ => InstructionReportStatus.FAILURE
    }
  }

  def getOrderFromPlaceInstruction(instruction: PlaceInstruction, betId: String): Order = {
    // TODO check all error codes returned match live api
    val (size, price, persistenceType, liability) = instruction.orderType match {
      case OrderType.LIMIT => (
        instruction.limitOrder.get.size,
        instruction.limitOrder.get.price,
        instruction.limitOrder.get.persistenceType,
        0.0                                                 // TODO Check this is correct
        )
      case OrderType.LIMIT_ON_CLOSE => (
        0.0,                                                // TODO find out what the size should be
        instruction.limitOnCloseOrder.get.price,
        PersistenceType.MARKET_ON_CLOSE,                    // TODO Check this is correct
        instruction.limitOnCloseOrder.get.liability
        )
      case OrderType.MARKET_ON_CLOSE => (
        0.0,                                                // TODO find out what the size should be
        0.0,                                                // Price will be market on close
        PersistenceType.MARKET_ON_CLOSE,
        instruction.marketOnCloseOrder.get.liability
        )
    }

    new Order(
      betId,
      instruction.orderType,
      OrderStatus.EXECUTABLE,         // Order is currently unmatched
      persistenceType,
      instruction.side,               // Lay or Back
      price,
      size,
      liability,
      DateTime.now(),                 // Placed date = Now
      0.0,                            // Average Price Matched
      0.0,                            // Matched
      size,                           // Remaining
      0.0,                            // Lapsed
      0.0,                            // Cancelled
      0.0                             // Voided
    )
  }

  // TODO check all error codes returned match live api
  def placeOrder(marketId: String, instruction: PlaceInstruction): PlaceInstructionReport = {
    // TODO handle market does not exist
    // TODO Validate orders: 1. is the order price correct ?
    val order = getOrderFromPlaceInstruction(instruction, betIdCounter.getNextBetId())

    if (!containsMarket(marketId)) orders(marketId) = mutable.HashMap[Long, Set[Order]]()
    if (!containsSelection(marketId, instruction.selectionId)) orders(marketId)(instruction.selectionId) = Set[Order]()

    orders(marketId)(instruction.selectionId) = orders(marketId)(instruction.selectionId) + order

    new PlaceInstructionReport(
      InstructionReportStatus.SUCCESS,            // Assume all orders are successful
      None,                                       // As we are assuming all orders are successful there will be no error codes
      instruction,
      Some(order.betId),                          // BetId
      Some(DateTime.now()),                       // Placed date = Now
      None,                                       // No price matched
      None                                        // No size matched
    )
  }

  // TODO check all error codes returned match live api
  def cancelOrder(marketId: String, instruction: CancelInstruction): CancelInstructionReport = {
    // TODO handle market does not exist
    val selectionId = getOrderSelectionId(marketId, instruction.betId)

    // check the order exists
    if (selectionId.isDefined) {
      val order = orders(marketId)(selectionId.get).find(x => x.betId == instruction.betId).get
      // check the reduction size is valid
      val sizeReduction = math.min(instruction.sizeReduction.getOrElse(order.sizeRemaining), order.sizeRemaining)
      if (sizeReduction > 0) {
        // Cancel the order
        val sizeRemaining = order.sizeRemaining - sizeReduction
        val orderStatus = if (sizeRemaining > 0) OrderStatus.EXECUTABLE else OrderStatus.EXECUTION_COMPLETE
        orders(marketId)(selectionId.get) -= order // remove the order
        orders(marketId)(selectionId.get) += new Order(
          order.betId,
          order.orderType,
          orderStatus,            // this changes to execution complete if there is no more size remaining
          order.persistenceType,
          order.side,             // Lay or Back
          order.price,
          order.size,
          order.bspLiability,
          order.placedDate,       // Placed date = Now
          order.avgPriceMatched,  // Average Price Matched
          order.sizeMatched,      // Matched
          sizeRemaining,          // Remaining
          order.sizeLapsed,       // Lapsed
          sizeReduction,          // Cancelled
          order.sizeVoided        // Voided
        )
        new CancelInstructionReport(
          InstructionReportStatus.SUCCESS,
          None,
          instruction,
          Some(sizeReduction),
          Some(DateTime.now())
        )
      } else {
        new CancelInstructionReport(
          InstructionReportStatus.FAILURE,
          Some(InstructionReportErrorCode.INVALID_BET_SIZE),
          instruction,
          None,
          None
        )
      }
    } else {
      new CancelInstructionReport(
        InstructionReportStatus.FAILURE,
        Some(InstructionReportErrorCode.INVALID_BET_ID),
        instruction,
        None,
        None
      )
    }
  }

  // TODO check all error codes returned match live api
  def replaceOrder(marketId: String, instruction: ReplaceInstruction): ReplaceInstructionReport = {
    val selectionId = getOrderSelectionId(marketId, instruction.betId)

    if (selectionId.isDefined) {
      val order = orders(marketId)(selectionId.get).find(x => x.betId == instruction.betId).get
      if (order.orderType != OrderType.MARKET_ON_CLOSE) {
        // cancel the order
        val cancelInstructionReport = cancelOrder(marketId, new CancelInstruction(instruction.betId))
        if (cancelInstructionReport.status == InstructionReportStatus.SUCCESS) {
          val placeInstructionReport = placeOrder(marketId, new PlaceInstruction(
            order.orderType,
            selectionId.get,
            0,
            order.side,
            if (order.orderType == OrderType.LIMIT)
              Some(LimitOrder(order.sizeRemaining, instruction.newPrice, order.persistenceType))
            else None,
            if (order.orderType == OrderType.LIMIT_ON_CLOSE)
              Some(LimitOnCloseOrder(order.bspLiability, order.price))
            else None,
            None
          ))
          val status = getReplaceInstructionReportStatus(cancelInstructionReport, placeInstructionReport)

          new ReplaceInstructionReport(
            status,
            if (status == InstructionReportStatus.SUCCESS)
              None
            else
              Some(InstructionReportErrorCode.CANCELLED_NOT_PLACED),
            cancelInstructionReport,
            placeInstructionReport
          )
        } else {
          new ReplaceInstructionReport(
            InstructionReportStatus.FAILURE,
            Some(InstructionReportErrorCode.BET_TAKEN_OR_LAPSED),
            cancelInstructionReport,
            new PlaceInstructionReport(
              InstructionReportStatus.FAILURE,
              Some(InstructionReportErrorCode.BET_TAKEN_OR_LAPSED),
              null, None, None, None, None
            )
          )
        }
      }
    }
    new ReplaceInstructionReport(
      InstructionReportStatus.FAILURE,
      Some(InstructionReportErrorCode.BET_TAKEN_OR_LAPSED),
      new CancelInstructionReport(
        InstructionReportStatus.FAILURE,
        Some(InstructionReportErrorCode.BET_TAKEN_OR_LAPSED),
        null, None, None
      ),
      new PlaceInstructionReport(
        InstructionReportStatus.FAILURE,
        Some(InstructionReportErrorCode.BET_TAKEN_OR_LAPSED),
        null, None, None, None, None
      )
    )
  }

  def updateOrder(marketId: String, instruction: UpdateInstruction): UpdateInstructionReport = {
    throw new Exception("Implementation pending")
  }

  // TODO take size of order into account
  // TODO does this account for instance of none
  def fillOrders(marketId: String, selectionId: Long, backPrice: PriceSize, layPrice: PriceSize): Option[Set[Order]] = {
    if (!containsSelection(marketId, selectionId))
      None
    else {
      val updatedOrders = getOrders(marketId, selectionId).map(order =>
        if (order.side == Side.BACK & order.price <= backPrice.price || order.side == Side.LAY & order.price >= layPrice.price)
          matchOrderSize(order)
        else
          order
      )
      orders(marketId)(selectionId) = updatedOrders
      Some(updatedOrders)
    }
  }

  private def matchOrderSize(order: Order): Order = {
    new Order(
      order.betId,
      order.orderType,
      OrderStatus.EXECUTION_COMPLETE,               // this changes to execution complete if there is no more size remaining
      order.persistenceType,
      order.side,                                   // Lay or Back
      order.price,
      order.size,
      order.bspLiability,
      order.placedDate,
      order.price,                                  // Average Price Matched // TODO this should be worked out as a weighted average
      order.sizeRemaining + order.sizeMatched,      // Matched
      0.0,                                          // Remaining
      order.sizeLapsed,                             // Lapsed
      order.sizeCancelled,                          // Cancelled
      order.sizeVoided                              // Voided
    )
  }
}