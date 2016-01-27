package core.dataModel.indicators

case class CommodityChannelIndex(smaOfTypicalPrice: Option[Double], meanDeviation: Option[Double], index: Option[Double])

object CommodityChannelIndex {
  val period = 20     // look-back period

  def getCommodityChannelIndex(typicalPrice: Double, prevData: List[TickData]): CommodityChannelIndex = {
    val smaOfTypicalPrice = getSMAOfTypicalPrice(typicalPrice, prevData, period = period)
    val meanDeviation = getMeanDeviation(typicalPrice, smaOfTypicalPrice, prevData, period)

    CommodityChannelIndex(
      smaOfTypicalPrice,
      meanDeviation,
      getIndex(typicalPrice, smaOfTypicalPrice, meanDeviation)
    )
  }

  private def getSMAOfTypicalPrice(typicalPrice: Double, prevData: List[TickData], period: Int = period): Option[Double] = prevData.headOption match {
    case Some(x) if x.commodityChannelIndex.isDefined && x.commodityChannelIndex.get.smaOfTypicalPrice.isDefined =>
      Some(QAT.getSimpleMovingAverage(period, typicalPrice, prevData(period - 1).typicalPrice, x.commodityChannelIndex.get.smaOfTypicalPrice.get))
    case Some(x) if prevData.size >= (period - 1) =>
      Some(QAT.getMean(typicalPrice :: prevData.take(period - 1).map(x => x.typicalPrice)))
    case _ => None
  }

  private def getMeanDeviation(typicalPrice: Double, smaOfTypicalPrice: Option[Double], prevData: List[TickData], period: Int = period): Option[Double] = smaOfTypicalPrice match {
    case Some(x) if prevData.size >= (period - 1) => Some((Math.abs(typicalPrice - x) :: prevData.take(period - 1).map(y => Math.abs(y.typicalPrice - x))).sum / period)
    case _ => None
  }

  private def getIndex(typicalPrice: Double, smaOfTypicalPrice: Option[Double], meanDeviation: Option[Double]): Option[Double] = (smaOfTypicalPrice, meanDeviation) match {
    case (Some(x), Some(y)) => Some((typicalPrice - x) / (0.015 * y))
    case _ => None
  }
}