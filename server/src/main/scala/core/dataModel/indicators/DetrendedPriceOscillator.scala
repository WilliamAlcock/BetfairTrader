package core.dataModel.indicators

case class DetrendedPriceOscillator(sma: Option[Double], oscillator: Option[Double])

object DetrendedPriceOscillator {
  val period = 20 // look-back period

  def getDetrendedPriceOscillator(close: Double, prevData: List[TickData], period: Int = period): DetrendedPriceOscillator = {
    val sma = getSMA(close, prevData, period = period)
    DetrendedPriceOscillator(sma, getDPO(sma, prevData, period = period))
  }

  private def getSMA(close: Double, prevData: List[TickData], period: Int = period): Option[Double] = prevData.headOption match {
    case Some(x) if x.detrendedPriceOscillator.isDefined && x.detrendedPriceOscillator.get.sma.isDefined =>
      Some(QAT.getSimpleMovingAverage(period, close, prevData(period - 1).close, x.detrendedPriceOscillator.get.sma.get))
    case Some(x) if prevData.size >= (period - 1) =>
      Some(QAT.getMean(close :: prevData.take(period - 1).map(_.close)))
    case _ => None
  }

  def getDPO(sma: Option[Double], prevData: List[TickData], period: Int = period): Option[Double] = sma match {
    case Some(x) if prevData.size >= (period / 2) => Some(x - prevData(period / 2).close)
    case _ => None
  }
}

