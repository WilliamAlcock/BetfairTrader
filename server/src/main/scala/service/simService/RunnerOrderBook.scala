package service.simService

import domain.Side.Side
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

  // throw an error if the id exists in both books
  def hasBetId(betId: String) = if (backOrderBook.hasBetId(betId)) {
    require(!layOrderBook.hasBetId(betId))
    true
  } else layOrderBook.hasBetId(betId)

  // back and lay orders
  def getOrders: Set[Order] = (backOrderBook.getOrders ++ layOrderBook.getOrders).toSet[Order]

  def getMatches: Set[Match] = (backOrderBook.getMatches ++ layOrderBook.getMatches).toSet[Match]

  def getOrderSide(betId: String): Option[Side] = betId match {
    case x if backOrderBook.hasBetId(x) => Some(Side.BACK)
    case x if layOrderBook.hasBetId(x) => Some(Side.LAY)
    case _ => None
  }
}