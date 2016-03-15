package indicators

import play.api.libs.json.Json

trait WilliamsPercentRData extends TickData {
  val williamsPercentR: WilliamsPercentR
}

case class WilliamsPercentR(highestHigh: Option[Double], lowestLow: Option[Double], percentR: Option[Double])

object WilliamsPercentR extends QAT {
  implicit val formatWilliamsPercentR = Json.format[WilliamsPercentR]

  val period = 14 // look-back period

  def getNext(tick: Tick, prevData: List[WilliamsPercentRData], period: Int = period): WilliamsPercentR = {
    val highestHigh = getHighestHigh(tick.range, prevData, period)
    val lowestLow = getLowestLow(tick.range, prevData, period)
    WilliamsPercentR(highestHigh, lowestLow, getPercentR(tick.close, highestHigh, lowestLow))
  }

  private def getPercentR(close: Double, highestHigh: Option[Double], lowestLow: Option[Double]): Option[Double] = (highestHigh, lowestLow) match {
    case (Some(x), Some(y)) => if (x - y == 0) Some(0) else Some(((x - close) / (x - y)) * - 100)
    case _ => None
  }
}