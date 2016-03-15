package indicators

trait QAT {

  // ***** Simple Moving Average ******

  def getMean(input: List[Double]): Double = if (input.isEmpty) 0.0 else input.sum / input.size

  def getNextSMA(period: Int, newValue: Double, valueToRemove: Double, prevSMA: Double): Double = prevSMA + (newValue / period) - (valueToRemove / period)

  private def _getSMA[T](prevData: List[T], valueFunc: (T) => Double, period: Int, newValue: Double, prevSMA: Option[Double]): Double = prevSMA match {
    case Some(x) => getNextSMA(period, newValue, valueFunc(prevData(period - 1)), x)
    case None => getMean(newValue :: prevData.take(period - 1).map(valueFunc(_)))
  }

  def getSMA[T](prevData: List[T], valueFunc: (T) => Double, period: Int, newValue: Double, prevSMA: Option[Double]): Option[Double] = {
    if (prevData.size >= (period - 1)) {
      Some(_getSMA(prevData, valueFunc, period, newValue, prevSMA))
    } else {
      None
    }
  }

  def getSMA[T](prevData: List[T], valueFunc: (T) => Option[Double], period: Int, newValue: Option[Double], prevSMA: Option[Double]): Option[Double] = {
    if (prevData.size >= (period - 1) && valueFunc(prevData(period - 2)).isDefined && newValue.isDefined) {
      Some(_getSMA(prevData, valueFunc.andThen(x => x.get), period, newValue.get, prevSMA))
    } else {
      None
    }
  }

  // ***** Exponential Moving Average *****

  def getEMAMultiplier(period: Double): Double = (2 / (period + 1))

  def getNextEMA(period: Int, newValue: Double, prevEMA: Double): Double = ((newValue - prevEMA) * getEMAMultiplier(period)) + prevEMA

  private def _getEMA[T](prevData: => List[T], valueFunc: (T) => Double, period: Int, newValue: Double, prevEMA: Option[Double]): Double = prevEMA match {
    case Some(x) => getNextEMA(period, newValue, x)
    case None => getMean(newValue :: prevData.take(period - 1).map(valueFunc(_)))
  }

  def getEMA[T](prevData: => List[T], valueFunc: (T) => Double, period: Int, newValue: Double, prevEMA: Option[Double]): Option[Double] = {
    if (prevData.size >= (period - 1)) {
      Some(_getEMA(prevData, valueFunc, period, newValue, prevEMA))
    } else {
      None
    }
  }

  def getEMA[T](prevData: => List[T], valueFunc: (T) => Option[Double], period: Int, newValue: Option[Double], prevEMA: Option[Double]): Option[Double] = {
    if (prevData.size >= (period - 1) && valueFunc(prevData(period - 2)).isDefined && newValue.isDefined) {
      Some(_getEMA(prevData, valueFunc.andThen(x => x.get), period, newValue.get, prevEMA))
    } else {
      None
    }
  }

  // *****

  def getStandardDeviation(data: List[Double]): Double = {
    val mean = getMean(data)
    val deviations = data.map(x => (x - mean) * (x - mean))
    if (data.isEmpty) 0.0 else Math.sqrt(deviations.sum / data.size)
  }

  def getHighestHigh(range: Range, prevData: List[TickData], period: Int): Option[Double] = prevData match {
    case x if x.size >= (period - 1) => Some(Math.max(prevData.take(period - 1).map(_.tick.range.high).max, range.high))
    case _ => None
  }

  def getLowestLow(range: Range, prevData: List[TickData], period: Int): Option[Double] = prevData match {
    case x if x.size >= (period - 1) => Some(Math.min(prevData.take(period - 1).map(_.tick.range.low).min, range.low))
    case _ => None
  }
}