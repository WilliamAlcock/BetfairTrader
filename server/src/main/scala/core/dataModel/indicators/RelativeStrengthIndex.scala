package core.dataModel.indicators

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

object RelativeStrengthIndex {
  val period = 14 // look-back period

  def getRelativeStrengthIndex(closeDelta: Option[Double], prevData: List[TickData], period: Int = period): RelativeStrengthIndex = {
    val gain = getGain(closeDelta)
    val loss = getLoss(closeDelta)
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

  private def getAvgGain(gain: Option[Double], prevData: List[TickData], period: Int): Option[Double] = (prevData.headOption, gain) match {
    case (Some(x), Some(y)) if x.relativeStrengthIndex.isDefined && x.relativeStrengthIndex.get.avgGain.isDefined =>
      Some(((x.relativeStrengthIndex.get.avgGain.get * (period - 1)) + y) / period)
    case (Some(x), Some(y)) if prevData.take(period - 1).count(x => x.relativeStrengthIndex.isDefined && x.relativeStrengthIndex.get.gain.isDefined) == (period - 1) =>
      Some(QAT.getMean(y :: prevData.take(period - 1).map(_.relativeStrengthIndex.get.gain.get)))
    case _ => None
  }

  private def getAvgLoss(loss: Option[Double], prevData: List[TickData], period: Int): Option[Double] = (prevData.headOption, loss) match {
    case (Some(x), Some(y)) if x.relativeStrengthIndex.isDefined && x.relativeStrengthIndex.get.avgLoss.isDefined =>
      Some(((x.relativeStrengthIndex.get.avgLoss.get * (period - 1)) + y) / period)
    case (Some(x), Some(y)) if prevData.take(period - 1).count(x => x.relativeStrengthIndex.isDefined && x.relativeStrengthIndex.get.loss.isDefined) == (period - 1) =>
      Some(QAT.getMean(y :: prevData.take(period - 1).map(_.relativeStrengthIndex.get.loss.get)))
    case _ => None
  }
}
