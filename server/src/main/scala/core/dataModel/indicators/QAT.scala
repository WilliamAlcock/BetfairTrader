package core.dataModel.indicators

trait QAT {
  /*
  Mean or Simple Moving Average
 */
  def getMean(input: List[Double]): Double = input.sum / input.size

  /*
  Simple Moving Average calculated using previous simple moving average
 */
  def getSimpleMovingAverage(period: Double, price: Double, priceToRemove: Double, prevSMA: Double) =
    prevSMA + (price / period) - (priceToRemove / period)

  /*
    Exponential Moving Average calculated using previous exponential moving average
    first instance of prevEMA should be a simple moving average
   */

  def getEMAMultiplier(period: Double): Double = (2 / (period + 1))

  def getExponentialMovingAverage(multiplier: Double, price: Double, prevEMA: Double) =
    ((price - prevEMA) * multiplier) + prevEMA

  /*
    Standard Deviation of a series of data
   */
  def getStandardDeviation(data: List[Double]): Double = {
    val mean = getMean(data)
    val deviations = data.map(x => (x - mean) * (x - mean))
    Math.sqrt(deviations.sum / data.size)
  }

  def getHighestHigh(range: Range, prevData: List[TickData], period: Int): Option[Double] = prevData match {
    case x if x.size >= (period - 1) => Some(Math.max(prevData.take(period - 1).map(_.range.high).max, range.high))
    case _ => None
  }

  def getLowestLow(range: Range, prevData: List[TickData], period: Int): Option[Double] = prevData match {
    case x if x.size >= (period - 1) => Some(Math.min(prevData.take(period - 1).map(_.range.low).min, range.low))
    case _ => None
  }
}

object QAT extends QAT {}