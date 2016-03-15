package indicators

import play.api.libs.json.Json

trait CommodityChannelIndexData extends TickData {
  val commodityChannelIndex: CommodityChannelIndex
}

case class CommodityChannelIndex(smaOfTypicalPrice: Option[Double], meanDeviation: Option[Double], index: Option[Double])

object CommodityChannelIndex extends QAT {
  implicit val formatCommodityChannelIndex = Json.format[CommodityChannelIndex]

  def getNext(tick: Tick, prevData: List[CommodityChannelIndexData], period: Int = 40 /* look-back period */): CommodityChannelIndex = {
    val smaOfTypicalPrice = getSMAOfTypicalPrice(tick.typicalPrice, prevData, period = period)
    val meanDeviation = getMeanDeviation(tick.typicalPrice, smaOfTypicalPrice, prevData, period)

    CommodityChannelIndex(
      smaOfTypicalPrice,
      meanDeviation,
      getIndex(tick.typicalPrice, smaOfTypicalPrice, meanDeviation)
    )
  }

  private def getSMAOfTypicalPrice(typicalPrice: Double, prevData: List[CommodityChannelIndexData], period: Int): Option[Double] =
    getSMA(prevData, (c: CommodityChannelIndexData) => c.tick.typicalPrice, period, typicalPrice, if (prevData.isEmpty) None else prevData.head.commodityChannelIndex.smaOfTypicalPrice)

  private def getMeanDeviation(typicalPrice: Double, smaOfTypicalPrice: Option[Double], prevData: List[CommodityChannelIndexData], period: Int): Option[Double] = smaOfTypicalPrice match {
    case Some(x) if prevData.size >= (period - 1) => Some((Math.abs(typicalPrice - x) :: prevData.take(period - 1).map(y => Math.abs(y.tick.typicalPrice - x))).sum / period)
    case _ => None
  }

  private def getIndex(typicalPrice: Double, smaOfTypicalPrice: Option[Double], meanDeviation: Option[Double]): Option[Double] = (smaOfTypicalPrice, meanDeviation) match {
    case (Some(x), Some(y)) => if (y == 0) Some(y) else Some((typicalPrice - x) / (0.015 * y))
    case _ => None
  }
}