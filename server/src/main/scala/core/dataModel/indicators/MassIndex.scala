package core.dataModel.indicators

case class MassIndex(emaOfRange: Option[Double], doubleEMA: Option[Double], ratio: Option[Double], index: Option[Double])

object MassIndex {
  val period = 9 // look-back period (ema + doubleEMA)
  val indexPeriod = 25 // look-back period (index)

  val emaMultiplier = QAT.getEMAMultiplier(period)

  def getMassIndex(range: Range, prevData: List[TickData], period: Int = period, indexPeriod: Int = indexPeriod): MassIndex = {
    val emaOfRange = getEMAOfRange(range, prevData, period = period)
    val doubleEMA = getDoubleEMA(emaOfRange, prevData, period = period)
    val ratio = getRatio(emaOfRange, doubleEMA)
    MassIndex(emaOfRange, doubleEMA, ratio, getIndex(ratio, prevData, period = indexPeriod))
  }

  private def getEMAOfRange(range: Range, prevData: List[TickData], period: Int): Option[Double] = prevData.headOption match {
    case Some(x) if x.massIndex.isDefined && x.massIndex.get.emaOfRange.isDefined =>
      Some(QAT.getExponentialMovingAverage(emaMultiplier, range.range, x.massIndex.get.emaOfRange.get))
    case Some(x) if prevData.size >= (period - 1) =>
      Some(QAT.getMean(range.range :: prevData.take(period - 1).map(_.range.range)))
    case _ => None
  }

  private def getDoubleEMA(emaOfRange: Option[Double], prevData: List[TickData], period: Int): Option[Double] = (emaOfRange, prevData.headOption) match {
    case (Some(x), Some(y)) if y.massIndex.isDefined && y.massIndex.get.doubleEMA.isDefined =>
      Some(QAT.getExponentialMovingAverage(emaMultiplier, x, y.massIndex.get.doubleEMA.get))
    case (Some(x), Some(y)) if prevData.take(period - 1).count(_.massIndex.get.emaOfRange.isDefined) == (period - 1) =>
      Some(QAT.getMean(x :: prevData.take(period - 1).map(_.massIndex.get.emaOfRange.get)))
    case _ => None
  }

  private def getRatio(emaOfRange: Option[Double], doubleEMA: Option[Double]): Option[Double] = (emaOfRange, doubleEMA) match {
    case (Some(x), Some(y)) => if (y == 0.0) Some(y) else Some(x / y)
    case _ => None
  }

  private def getIndex(ratio: Option[Double], prevData: List[TickData], period: Int): Option[Double] = ratio match {
    case Some(x) if prevData.take(period - 1).count(x => x.massIndex.isDefined && x.massIndex.get.ratio.isDefined) == (period - 1) =>
      Some(x + prevData.take(period - 1).map(_.massIndex.get.ratio.get).sum)
    case _ => None
  }
}


