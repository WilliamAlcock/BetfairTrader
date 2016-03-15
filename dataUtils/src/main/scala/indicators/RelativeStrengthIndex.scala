package indicators

import play.api.libs.json.Json

trait RelativeStrengthIndexData extends TickData {
  val relativeStrengthIndex: RelativeStrengthIndex
}

case class RelativeStrengthIndex(gain: Option[Double], loss: Option[Double], avgGain: Option[Double], avgLoss: Option[Double]) {
  val rs: Option[Double] = (avgGain, avgLoss) match {
    case (Some(x), Some(y)) => if (y == 0) Some(y) else Some(x / y)
    case _ => None
  }
  val rsi: Option[Double] = rs match {
    case Some(x) => Some(100 - (100 / (1 + x)))
    case _ => None
  }
}

object RelativeStrengthIndex extends QAT {
  implicit val formatRelativeStrengthIndex = Json.format[RelativeStrengthIndex]

  val period = 14 // look-back period

  def getNext(tick: Tick, prevData: List[RelativeStrengthIndexData], period: Int = period): RelativeStrengthIndex = {
    val gain = getGain(tick.closeDelta)
    val loss = getLoss(tick.closeDelta)
    RelativeStrengthIndex(gain, loss, getAvgGain(gain, prevData, period = period), getAvgLoss(loss, prevData, period = period))
  }

  private def getGain(closeDelta: Option[Double]): Option[Double] = closeDelta match {
    case Some(x) => Some(Math.max(0, x))
    case _ => None
  }

  private def getLoss(closeDelta: Option[Double]): Option[Double] = closeDelta match {
    case Some(x) => Some(Math.abs(Math.min(0, x)))
    case _ => None
  }

  private def getAvg(newValue: Option[Double],
                     prevData: List[RelativeStrengthIndexData],
                     prevAvg: Option[Double],
                     valueFunc: (RelativeStrengthIndexData) => Option[Double]): Option[Double] = (prevAvg, newValue) match {
    case (Some(x), Some(y)) => Some(((x * (period - 1)) + y) / period)
    case (None, Some(y)) if prevData.take(period - 1).count(valueFunc(_).isDefined) == (period - 1) => Some(getMean(y :: prevData.take(period - 1).map(valueFunc(_).get)))
    case _ => None
  }

  private def getAvgGain(gain: Option[Double], prevData: List[RelativeStrengthIndexData], period: Int): Option[Double] =
    getAvg(gain, prevData, if (prevData.isEmpty) None else prevData.head.relativeStrengthIndex.avgGain, (r: RelativeStrengthIndexData) => r.relativeStrengthIndex.gain)

  private def getAvgLoss(loss: Option[Double], prevData: List[RelativeStrengthIndexData], period: Int): Option[Double] =
    getAvg(loss, prevData, if (prevData.isEmpty) None else prevData.head.relativeStrengthIndex.avgLoss, (r: RelativeStrengthIndexData) => r.relativeStrengthIndex.loss)
}
