package service.newTestService

import domain.Side.Side
import domain._
import org.joda.time.DateTime

case class OrderBook(side: Side, orders: List[Order] = List.empty, orderFactory: OrderFactory = OrderFactory) {

  private def p(newOrderPrice: Double, listOrderPrice: Double): Boolean = side match {
    case Side.LAY  => newOrderPrice > listOrderPrice
    case Side.BACK => newOrderPrice < listOrderPrice
  }

  private def isMatched(matchPrice: Double, orderPrice: Double): Boolean = side match {
    case Side.LAY   => matchPrice <= orderPrice
    case Side.BACK  => matchPrice >= orderPrice
  }

  private def insert(orders: List[Order], order: Order):List[Order] = orders match {
    case Nil => List(order)
    case h::t if p(order.price, h.price) => order::h::t
    case h::t => h::insert(t, order)
  }

  private def _cancelOrder(order: Order, sizeToCancel: Double): Order = {
    val status = if (sizeToCancel == order.sizeRemaining) OrderStatus.EXECUTION_COMPLETE else OrderStatus.EXECUTABLE
    order.copy(
      status = status,
      sizeRemaining = order.sizeRemaining - sizeToCancel,
      sizeCancelled = order.sizeCancelled + sizeToCancel
    )
  }

  private def _matchOrder(order: Order): Order = {
    order.copy(
      status = OrderStatus.EXECUTION_COMPLETE,
      sizeMatched = order.sizeRemaining,
      sizeRemaining = 0
    )
  }

  // TODO implement checking for incorrect instructions
  def placeOrder(instruction: PlaceInstruction): PlaceOrderResponse[OrderBook] = {
    require(instruction.side == side)
    val order = orderFactory.createOrder(instruction)
    PlaceOrderResponse(
      this.copy(orders = insert(orders, order)),
      PlaceInstructionReport(
        InstructionReportStatus.SUCCESS, None, instruction,
        Some(order.betId), Some(order.placedDate), Some(order.avgPriceMatched), Some(order.sizeMatched)
      )
    )
  }

  // TODO implement check for duplicate order Ids (possibly a set)
  // TODO implement check for trying to cancel order that is EXECUTION_COMPLETE
  def cancelOrder(instruction: CancelInstruction): CancelOrderResponse[OrderBook] = {
    // Check betId Exists
    // Get Size to cancel
    orders.find(x => x.betId == instruction.betId) match {
      case Some(x) =>
        val sizeToCancel = Math.min(x.sizeRemaining, instruction.sizeReduction.getOrElse(x.sizeRemaining))
        CancelOrderResponse(
          this.copy(orders = orders.map(x => if (x.betId == instruction.betId && x.status == OrderStatus.EXECUTABLE) _cancelOrder(x, sizeToCancel) else x)),
          CancelInstructionReport(
            InstructionReportStatus.SUCCESS,
            None,
            instruction,
            Some(sizeToCancel),
            Some(DateTime.now())
          )
        )
      case None => CancelOrderResponse(
        this,
        CancelInstructionReport(
          InstructionReportStatus.FAILURE,
          Some(InstructionReportErrorCode.INVALID_BET_ID),
          instruction,
          None,
          None
        )
      )   // Invalid BetId
    }
  }

  // TODO implement check for duplicate order Ids (possibly a set)
  // TODO implement check for trying to cancel order that is EXECUTION_COMPLETE
  def updateOrder(instruction: UpdateInstruction): UpdateOrderResponse[OrderBook] = {
    orders.find(x => x.betId == instruction.betId) match {
      case Some(x) => UpdateOrderResponse(
        this.copy(orders = orders.map(x => if (x.betId == instruction.betId) x.copy(persistenceType = instruction.newPersistenceType) else x)),
        UpdateInstructionReport(
          InstructionReportStatus.SUCCESS,
          None,
          instruction
        )
      )
      case None => UpdateOrderResponse(
        this,
        UpdateInstructionReport(
          InstructionReportStatus.FAILURE,
          Some(InstructionReportErrorCode.INVALID_BET_ID),
          instruction
        )
      )   // Invalid BetId
    }
  }

  def matchOrders(price: Double): OrderBook = {
    this.copy(orders = orders.map(x => if (x.status == OrderStatus.EXECUTABLE && isMatched(price, x.price)) _matchOrder(x) else x))
  }

  def hasBetId(betId: String):Boolean = orders.find(x => x.betId == betId).size > 0

  def getOrders(): List[Order] = orders

  def getSide(): Side = side
}