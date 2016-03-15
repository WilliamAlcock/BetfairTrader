package indicators

import play.api.libs.json.Json

trait StochasticOscillatorData extends TickData {
  val stochasticOscillator: StochasticOscillator
}

case class StochasticOscillator(highestHigh: Option[Double], lowestLow: Option[Double], percentK: Option[Double], percentD: Option[Double], slowPercentD: Option[Double])

object StochasticOscillator extends QAT {
  implicit val formatStochasticOscillator = Json.format[StochasticOscillator]

  val period = 14 // look-back period

  def getNext(tick: Tick, prevData: List[StochasticOscillatorData], period: Int = period): StochasticOscillator = {
    val highestHigh = getHighestHigh(tick.range, prevData, period)
    val lowestLow = getLowestLow(tick.range, prevData, period)
    val percentK = getPercentK(highestHigh, lowestLow, tick.close)
    val percentD = getPercentD(percentK, prevData)
    StochasticOscillator(highestHigh, lowestLow, percentK, percentD, getSlowPercentD(percentD, prevData))
  }

  private def getPercentK(highestHigh: Option[Double], lowestLow: Option[Double], close: Double): Option[Double] = (highestHigh, lowestLow) match {
    case (Some(h), Some(l)) => if (h - l == 0) Some(0.0) else Some((close - l) / (h - l) * 100)
    case _ => None
  }

  private def getPercentD(stochasticOscillatorK: Option[Double], prevData: List[StochasticOscillatorData]): Option[Double] = (stochasticOscillatorK, prevData.headOption) match {
    case (Some(x), Some(y)) if y.stochasticOscillator.percentK.isDefined && prevData.size > 1 && prevData(1).stochasticOscillator.percentK.isDefined =>
      Some((x + y.stochasticOscillator.percentK.get + prevData(1).stochasticOscillator.percentK.get) / 3)
    case _ => None
  }

  private def getSlowPercentD(percentD: Option[Double], prevData: List[StochasticOscillatorData]): Option[Double] = (percentD, prevData.headOption) match {
    case (Some(x), Some(y)) if y.stochasticOscillator.percentD.isDefined && prevData.size > 1 && prevData(1).stochasticOscillator.percentD.isDefined =>
      Some((x + y.stochasticOscillator.percentD.get + prevData(1).stochasticOscillator.percentD.get) / 3)
    case _ => None
  }
}

