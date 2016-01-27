package core.dataModel.indicators

case class MACD(firstEMA: Option[Double], secondEMA: Option[Double], line: Option[Double], signalLine: Option[Double], histogram: Option[Double])

object MACD {
  val firstPeriod = 12         // first look-back period
  val secondPeriod = 26   // second look-back period
  val signalPeriod = 9    // signal look-back period

  val firstEMAMultiplier = QAT.getEMAMultiplier(firstPeriod)
  val secondEMAMultiplier = QAT.getEMAMultiplier(secondPeriod)
  val signalEMAMultiplier = QAT.getEMAMultiplier(signalPeriod)

  def getMACD(close: Double,
              prevData: List[TickData],
              firstPeriod: Int = firstPeriod,
              secondPeriod: Int = secondPeriod,
              signalPeriod: Int = signalPeriod,
              firstEMAMultiplier: Double = firstEMAMultiplier,
              secondEMAMultiplier: Double = secondEMAMultiplier,
              signalEMAMultiplier: Double = signalEMAMultiplier): MACD = {
    val firstEMA = getFirstEMA(close, prevData, period = firstPeriod, multiplier = firstEMAMultiplier)
    val secondEMA = getSecondEMA(close, prevData, period = secondPeriod, multiplier = secondEMAMultiplier)
    val line = getLine(firstEMA, secondEMA)
    val signalLine = getSignalLine(line, prevData, period = signalPeriod, multiplier = signalEMAMultiplier)
    MACD(firstEMA, secondEMA, line, signalLine, getHistogram(line, signalLine))
  }

  private def getFirstEMA(close: Double, prevData: List[TickData], period: Int, multiplier: Double): Option[Double] = prevData.headOption match {
    case Some(x) if x.macd.isDefined && x.macd.get.firstEMA.isDefined =>
      Some(QAT.getExponentialMovingAverage(multiplier, close, x.macd.get.firstEMA.get))
    case Some(x) if prevData.take(period - 1).count(x => x.macd.isDefined && x.macd.get.firstEMA.isDefined) == (period - 1) =>
      Some(QAT.getMean(close :: prevData.take(period - 1).map(x => x.close)))
    case _ => None
  }

  private def getSecondEMA(close: Double, prevData: List[TickData], period: Int, multiplier: Double): Option[Double] = prevData.headOption match {
    case Some(x) if x.macd.isDefined && x.macd.get.secondEMA.isDefined =>
      Some(QAT.getExponentialMovingAverage(multiplier, close, x.macd.get.secondEMA.get))
    case Some(x) if prevData.take(period - 1).count(x => x.macd.isDefined && x.macd.get.secondEMA.isDefined) == (period - 1) =>
      Some(QAT.getMean(close :: prevData.take(period - 1).map(x => x.close)))
    case _ => None
  }

  private def getLine(firstEMA: Option[Double], secondEMA: Option[Double]): Option[Double] = (firstEMA, secondEMA) match {
    case (Some(x), Some(y)) => Some(x - y)
    case _ => None
  }

  private def getSignalLine(line: Option[Double], prevData: List[TickData], period: Int, multiplier: Double): Option[Double] = (line, prevData.headOption) match {
    case (Some(x), Some(y)) if y.macd.isDefined && y.macd.get.signalLine.isDefined =>
      Some(QAT.getExponentialMovingAverage(multiplier, x, y.macd.get.signalLine.get))
    case (Some(x), Some(y)) if prevData.take(period - 1).count(x => x.macd.isDefined && x.macd.get.line.isDefined) == (period - 1) =>
      Some(QAT.getMean(x :: prevData.take(period - 1).map(_.macd.get.line.get)))
    case _ => None
  }

  private def getHistogram(line: Option[Double], signalLine: Option[Double]): Option[Double] = (line, signalLine) match {
    case (Some(x), Some(y)) => Some(x - y)
    case _ => None
  }
}
