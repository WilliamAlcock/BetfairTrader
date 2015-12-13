package service.newTestService

import domain._

case class RunnerOrderBook(backOrderBook: OrderBook = OrderBook(Side.BACK),
                           layOrderBook: OrderBook = OrderBook(Side.LAY)) {

  def placeOrder(instruction: PlaceInstruction): PlaceOrderResponse[RunnerOrderBook] = instruction.side match {
    case Side.BACK =>
      val response = backOrderBook.placeOrder(instruction)
      PlaceOrderResponse(this.copy(backOrderBook = response.result), response.report)
    case Side.LAY =>
      val response = layOrderBook.placeOrder(instruction)
      PlaceOrderResponse(this.copy(layOrderBook = response.result), response.report)
  }

  def cancelOrder(instruction: CancelInstruction): CancelOrderResponse[RunnerOrderBook] = {
    if (backOrderBook.hasBetId(instruction.betId)) {
      backOrderBook.cancelOrder(instruction) match {
        case x => CancelOrderResponse(this.copy(backOrderBook = x.result), x.report)
      }
    } else {
      layOrderBook.cancelOrder(instruction) match {
        case x => CancelOrderResponse(this.copy(layOrderBook = x.result), x.report)
      }
    }
  }

  def updateOrder(instruction: UpdateInstruction): UpdateOrderResponse[RunnerOrderBook] = {
    if (backOrderBook.hasBetId(instruction.betId)) {
      backOrderBook.updateOrder(instruction) match {
        case x => UpdateOrderResponse(this.copy(backOrderBook = x.result), x.report)
      }
    } else {
      layOrderBook.updateOrder(instruction) match {
        case x => UpdateOrderResponse(this.copy(layOrderBook = x.result), x.report)
      }
    }
  }

  def matchOrders(availableToBack: Double, availableToLay: Double) : RunnerOrderBook = this.copy(
    backOrderBook = backOrderBook.matchOrders(availableToBack),
    layOrderBook = layOrderBook.matchOrders(availableToLay)
  )

  // TODO test
  def hasBetId(betId: String) = backOrderBook.hasBetId(betId) || layOrderBook.hasBetId(betId)

  // descending order, back and lay bets
  def getOrders(): List[Order] = {
    throw new NotImplementedError()
  }
}