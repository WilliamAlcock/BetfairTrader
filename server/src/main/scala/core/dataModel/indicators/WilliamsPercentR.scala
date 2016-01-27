package core.dataModel.indicators

case class WilliamsPercentR(highestHigh: Option[Double], lowestLow: Option[Double], percentR: Option[Double])

object WilliamsPercentR {
  val period = 14 // look-back period

  def getWilliamsPercentR(close: Double, range: Range, prevData: List[TickData], period: Int = period): WilliamsPercentR = {
    val highestHigh = QAT.getHighestHigh(range, prevData, period)
    val lowestLow = QAT.getLowestLow(range, prevData, period)
    WilliamsPercentR(highestHigh, lowestLow, getPercentR(close, highestHigh, lowestLow))
  }

  private def getPercentR(close: Double, highestHigh: Option[Double], lowestLow: Option[Double]): Option[Double] = (highestHigh, lowestLow) match {
    case (Some(x), Some(y)) => Some(((x - close) / (x - y)) * - 100)
    case _ => None
  }
}