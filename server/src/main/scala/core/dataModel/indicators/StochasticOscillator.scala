package core.dataModel.indicators

case class StochasticOscillator(highestHigh: Option[Double], lowestLow: Option[Double], percentK: Option[Double], percentD: Option[Double])

object StochasticOscillator {
  val period = 14 // look-back period

  def getStochasticOscillator(range: Range, close: Double, prevData: List[TickData], period: Int = period): StochasticOscillator = {
    val highestHigh = QAT.getHighestHigh(range, prevData, period)
    val lowestLow = QAT.getLowestLow(range, prevData, period)
    val percentK = getPercentK(highestHigh, lowestLow, close)
    StochasticOscillator(highestHigh, lowestLow, percentK, getPercentD(percentK, prevData))
  }

  private def getPercentK(highestHigh: Option[Double], lowestLow: Option[Double], close: Double): Option[Double] = (highestHigh, lowestLow) match {
    case (Some(h), Some(l)) => Some((close - l) / (h - l) * 100)
    case _ => None
  }

  private def getPercentD(stochasticOscillatorK: Option[Double], prevData: List[TickData]): Option[Double] = (stochasticOscillatorK, prevData.headOption) match {
    case (Some(x), Some(y)) if y.stochasticOscillator.isDefined && y.stochasticOscillator.get.percentK.isDefined
                              && prevData.size > 1 && prevData(1).stochasticOscillator.isDefined && prevData(1).stochasticOscillator.get.percentK.isDefined =>
      Some((x + y.stochasticOscillator.get.percentK.get + prevData(1).stochasticOscillator.get.percentK.get) / 3)
    case _ => None
  }
}

