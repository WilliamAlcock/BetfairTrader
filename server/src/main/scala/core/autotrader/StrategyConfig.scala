package core.autotrader

import core.autotrader.Runner.Strategy
import domain.Side
import domain._
import domain.Side._
import org.joda.time.DateTime
import play.api.libs.json.Json

case class StrategyConfig(layPrice: Double, backPrice: Double, size: Double, startSide: Side, stopPrice: Double, startTime: Option[DateTime], stopTime: Option[DateTime]) {
  def getInstructions(selectionId: Long, handicap: Double, startSide: Side): List[PlaceInstruction] = startSide match {
    case Side.BACK => List(
      PlaceInstruction(OrderType.LIMIT, selectionId, handicap, Side.BACK, Some(LimitOrder(size, backPrice, PersistenceType.PERSIST))),
      PlaceInstruction(OrderType.LIMIT, selectionId, handicap, Side.LAY, Some(LimitOrder(size, layPrice, PersistenceType.PERSIST)))
    )
    case Side.LAY => List(
      PlaceInstruction(OrderType.LIMIT, selectionId, handicap, Side.LAY, Some(LimitOrder(size, layPrice, PersistenceType.PERSIST))),
      PlaceInstruction(OrderType.LIMIT, selectionId, handicap, Side.BACK, Some(LimitOrder(size, backPrice, PersistenceType.PERSIST)))
    )
  }

  def getStrategy(marketId: String, selectionId: Long, handicap: Double): Strategy = Strategy(
    marketId,
    selectionId,
    instructions = getInstructions(selectionId, handicap, startSide),
    stopPrice = stopPrice,
    stopSide = if (startSide == Side.BACK) Side.LAY else Side.BACK,
    startTime = startTime,
    stopTime = stopTime
  )
}

object StrategyConfig {
  implicit val formatStrategyConfig = Json.format[StrategyConfig]
}
