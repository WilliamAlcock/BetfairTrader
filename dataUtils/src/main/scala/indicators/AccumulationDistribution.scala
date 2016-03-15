package indicators

import play.api.libs.json.Json

/*
   A volume-based indicator
   The line is a running total of each period's Money Flow Volume.
 */

trait AccumulationDistributionData extends TickData {
  val accumulationDistribution: AccumulationDistribution
}

case class AccumulationDistribution(moneyFlowMultiplier: Double, moneyFlowVolume: Double, line: Double, lineDelta: Option[Double])

object AccumulationDistribution {
  implicit val formatAccumulationDistribution = Json.format[AccumulationDistribution]

  def getNext(tick: Tick, prevData: List[AccumulationDistributionData]): AccumulationDistribution = {
    val moneyFlowMultiplier = getMoneyFlowMultiplier(tick.close, tick.range)
    val moneyFlowVolume = getMoneyFlowVolume(moneyFlowMultiplier, tick.volume)
    val line = getLine(moneyFlowVolume, prevData)
    val delta = getLineDelta(line, prevData)
    AccumulationDistribution(moneyFlowMultiplier, moneyFlowVolume, line, delta)
  }

  /*
    Fluctuates between +1 and -1
    The multiplier is positive when the close is in the upper half of the high-low range
    And negative when in the lower half
    This makes sense as buying pressure is stronger than selling pressure when prices close in the upper half of the period's range (and vice versa)

   */
  private def getMoneyFlowMultiplier(close: Double, range: Range): Double = range.high - range.low match {
    case x if x == 0 => 0.0
    case x => ((close - range.low) - (range.high - close)) / x
  }

  private def getMoneyFlowVolume(moneyFlowMultiplier: Double, volume: Double): Double = moneyFlowMultiplier * volume

  private def getLine(moneyFlowVolume: Double, prevData: List[AccumulationDistributionData]): Double = prevData.headOption match {
    case Some(x) => moneyFlowVolume + x.accumulationDistribution.line
    case _ => moneyFlowVolume
  }

  /*
    Percentage delta
   */
  private def getLineDelta(line: Double, prevData: List[AccumulationDistributionData]): Option[Double] =
    if (prevData.isEmpty) {
      None
    } else if (prevData.head.accumulationDistribution.line == 0.0) {
      Some(0.0)
    } else {
      Some(((line - prevData.head.accumulationDistribution.line) / prevData.head.accumulationDistribution.line) * 100)
    }
}
