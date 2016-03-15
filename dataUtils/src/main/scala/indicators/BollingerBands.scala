package indicators

import play.api.libs.json.Json

trait BollingerBandData extends TickData {
  val bollingerBands: BollingerBands
}

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

object BollingerBands extends QAT {
  implicit val formatBollingerBands = Json.format[BollingerBands]

  val period = 20     // look-back period
  val numStdDev = 2   // distance between middle and outer bands

  def getNext(tick: Tick, prevData: List[BollingerBandData], period: Int = period, numStdDev: Int = numStdDev): BollingerBands = BollingerBands(
    getMiddle(tick.close, prevData, period = period),
    getStdDev(tick.close, prevData, period = period),
    numStdDev
  )

  // x period simple moving average of close
  private def getMiddle(close: Double, prevData: List[BollingerBandData], period: Int = period): Option[Double] =
    getSMA(prevData, (b: BollingerBandData) => b.tick.close, period, close, if (prevData.isEmpty) None else prevData.head.bollingerBands.middle)

  // x period standard deviation of close
  private def getStdDev(close: Double, prevData: List[BollingerBandData], period: Int = period): Option[Double] = prevData.headOption match {
    case Some(x) if prevData.size >= (period - 1) => Some(getStandardDeviation(close :: prevData.take(period - 1).map(x => x.tick.close)))
    case _ => None
  }
}
