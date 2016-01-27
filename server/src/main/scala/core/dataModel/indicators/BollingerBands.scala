package core.dataModel.indicators

case class BollingerBands(middle: Option[Double], stdDev: Option[Double], numStdDev: Int) {
  val upper: Option[Double] = (middle, stdDev) match {
    case (Some(x), Some(y)) => Some(x + (numStdDev * y))
    case _ => None
  }
  val lower: Option[Double] = (middle, stdDev) match {
    case (Some(x), Some(y)) => Some(x - (numStdDev * y))
    case _ => None
  }
  val bandWidth: Option[Double] = (upper, lower) match {
    case (Some(x), Some(y)) => Some(x - y)
    case _ => None
  }
}

object BollingerBands {
  val period = 20     // look-back period
  val numStdDev = 2   // distance between middle and outer bands

  def getBollingerBands(close: Double, prevData: List[TickData], period: Int = period, numStdDev: Int = numStdDev): BollingerBands = BollingerBands(
    getMiddle(close, prevData, period = period),
    getStdDev(close, prevData, period = period),
    numStdDev
  )

  // x period simple moving average of close
  private def getMiddle(close: Double, prevData: List[TickData], period: Int = period): Option[Double] = prevData.headOption match {
    case Some(x) if x.bollingerBands.isDefined && x.bollingerBands.get.middle.isDefined =>
      Some(QAT.getSimpleMovingAverage(period, close, prevData(period - 1).close, x.bollingerBands.get.middle.get))
    case Some(x) if prevData.take(period - 1).count(_.bollingerBands.isDefined) == (period - 1) =>
      Some(QAT.getMean(close :: prevData.take(period - 1).map(x => x.close)))
    case _ => None
  }

  // x period standard deviation of close
  private def getStdDev(close: Double, prevData: List[TickData], period: Int = period): Option[Double] = prevData.headOption match {
    case Some(x) if prevData.size >= (period - 1) => Some(QAT.getStandardDeviation(close :: prevData.take(period - 1).map(x => x.close)))
    case _ => None
  }
}
