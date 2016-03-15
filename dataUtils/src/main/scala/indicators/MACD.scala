package indicators

import play.api.libs.json.Json

trait MACDData extends TickData {
  val macd: MACD
}

case class MACD(firstEMA: Option[Double], secondEMA: Option[Double], line: Option[Double], signalLine: Option[Double]) {
  val histogram: Option[Double] = (line, signalLine) match {
    case (Some(x), Some(y)) => Some(x - y)
    case _ => None
  }

  val histogramPercent: Option[Double] = (histogram, signalLine) match {
    case (Some(x), Some(y)) => Some((x / y) * 100)
    case _ => None
  }
}

object MACD extends QAT {
  implicit val formatMACD = Json.format[MACD]

  val firstPeriod = 12    // first look-back period
  val secondPeriod = 26   // second look-back period
  val signalPeriod = 9    // signal look-back period

  def getNext(tick: Tick,
              prevData: List[MACDData],
              firstPeriod: Int = firstPeriod,
              secondPeriod: Int = secondPeriod,
              signalPeriod: Int = signalPeriod): MACD = {
    val firstEMA = getFirstEMA(tick.close, prevData, period = firstPeriod)
    val secondEMA = getSecondEMA(tick.close, prevData, period = secondPeriod)
    val line = getLine(firstEMA, secondEMA)
    val signalLine = getSignalLine(line, prevData, period = signalPeriod)
    MACD(firstEMA, secondEMA, line, signalLine)
  }

  private def getFirstEMA(close: Double, prevData: List[MACDData], period: Int = firstPeriod): Option[Double] =
    getEMA(prevData, (m: MACDData) => m.tick.close, period, close, if (prevData.isEmpty) None else prevData.head.macd.firstEMA)

  private def getSecondEMA(close: Double, prevData: List[MACDData], period: Int = secondPeriod): Option[Double] =
    getEMA(prevData, (m: MACDData) => m.tick.close, period, close, if (prevData.isEmpty) None else prevData.head.macd.secondEMA)

  private def getLine(firstEMA: Option[Double], secondEMA: Option[Double]): Option[Double] = (firstEMA, secondEMA) match {
    case (Some(x), Some(y)) => Some(x - y)
    case _ => None
  }

  private def getSignalLine(line: Option[Double], prevData: List[MACDData], period: Int = signalPeriod): Option[Double] =
    getEMA(prevData, (m: MACDData) => m.macd.line, period, line, if (prevData.isEmpty) None else prevData.head.macd.signalLine)
}
