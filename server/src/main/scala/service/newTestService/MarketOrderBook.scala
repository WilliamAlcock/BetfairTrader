package service.newTestService

import domain._

import scala.collection.immutable.HashMap

case class MarketOrderBook(runners: HashMap[String, RunnerOrderBook] = HashMap.empty) {

  def placeOrder(instruction: PlaceInstruction): PlaceOrderResponse[MarketOrderBook] = {
    val response = runners.getOrElse(instruction.uniqueId, new RunnerOrderBook()).placeOrder(instruction)
    PlaceOrderResponse(
      this.copy(runners = runners + (instruction.uniqueId -> response.result)),
      response.report
    )
  }

  def cancelOrder(instruction: CancelInstruction): CancelOrderResponse[MarketOrderBook] = {
    runners.find(x => x._2.hasBetId(instruction.betId)) match {
      case Some((id: String, runnerBook: RunnerOrderBook)) =>
        runners(id).cancelOrder(instruction) match {
          case x => CancelOrderResponse(this.copy(runners = runners + (id -> x.result)), x.report)
        }
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

  def updateOrder(instruction: UpdateInstruction): UpdateOrderResponse[MarketOrderBook] = {
    runners.find(x => x._2.hasBetId(instruction.betId)) match {
      case Some((id: String, runnerBook: RunnerOrderBook)) =>
        runners(id).updateOrder(instruction) match {
          case x => UpdateOrderResponse(this.copy(runners = runners + (id -> x.result)), x.report)
        }
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

  def matchOrders(uniqueId: String, availableToBack: Double, availableToLay: Double): MarketOrderBook = {
    runners.get(uniqueId) match {
      case Some(x) => this.copy(runners = runners + (uniqueId -> x.matchOrders(availableToBack, availableToLay)))
      case None => this
    }
  }

  def getOrders(uniqueId: String): List[Order] = {
    runners.get(uniqueId) match {
      case Some(x) => x.getOrders
      case None => List.empty
    }
  }
}
