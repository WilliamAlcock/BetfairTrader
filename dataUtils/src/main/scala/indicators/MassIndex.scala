package indicators

import play.api.libs.json.Json

trait MassIndexData extends TickData {
  val massIndex: MassIndex
}

case class MassIndex(emaOfRange: Option[Double], doubleEMA: Option[Double], ratio: Option[Double], index: Option[Double])

object MassIndex extends QAT {
  implicit val formatMassIndex = Json.format[MassIndex]

  val period = 9        // look-back period (ema + doubleEMA)
  val indexPeriod = 25  // look-back period (index)

  def getNext(tick: Tick, prevData: List[MassIndexData], period: Int = period, indexPeriod: Int = indexPeriod): MassIndex = {
    val emaOfRange = getEMAOfRange(tick.range, prevData, period = period)
    val doubleEMA = getDoubleEMA(emaOfRange, prevData, period = period)
    val ratio = getRatio(emaOfRange, doubleEMA)
    MassIndex(emaOfRange, doubleEMA, ratio, getIndex(ratio, prevData, period = indexPeriod))
  }

  private def getEMAOfRange(range: Range, prevData: List[MassIndexData], period: Int): Option[Double] =
    getEMA(prevData, (m: MassIndexData) => m.tick.range.range, period, range.range, if (prevData.isEmpty) None else prevData.head.massIndex.emaOfRange)

  private def getDoubleEMA(emaOfRange: Option[Double], prevData: List[MassIndexData], period: Int): Option[Double] =
    getEMA(prevData, (m: MassIndexData) => m.massIndex.emaOfRange, period, emaOfRange, if (prevData.isEmpty) None else prevData.head.massIndex.doubleEMA)

  private def getRatio(emaOfRange: Option[Double], doubleEMA: Option[Double]): Option[Double] = (emaOfRange, doubleEMA) match {
    case (Some(x), Some(y)) => if (y == 0.0) Some(y) else Some(x / y)
    case _ => None
  }

  private def getIndex(ratio: Option[Double], prevData: List[MassIndexData], period: Int): Option[Double] = ratio match {
    case Some(x) if prevData.take(period - 1).count(x => x.massIndex.ratio.isDefined) == (period - 1) =>
      Some(x + prevData.take(period - 1).map(_.massIndex.ratio.get).sum)
    case _ => None
  }
}


