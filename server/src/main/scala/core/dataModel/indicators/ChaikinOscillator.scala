package core.dataModel.indicators

case class ChaikinOscillator(firstEMAofADL: Option[Double], secondEMAofADL: Option[Double]) {
  val oscillator: Option[Double] = (firstEMAofADL, secondEMAofADL) match {
    case (Some(x), Some(y)) => Some(x - y)
    case _ => None
  }
}

object ChaikinOscillator {
  val firstPeriod = 3 // first ema look-back period
  val secondPeriod = 10 // second ema look-back period
  val firstEMAMultiplier = QAT.getEMAMultiplier(firstPeriod)
  val secondEMAMultiplier = QAT.getEMAMultiplier(secondPeriod)

  def getChaikinOscillator(accDistLine: Double,
                           prevData: List[TickData],
                           firstPeriod: Int = firstPeriod,
                           secondPeriod: Int = secondPeriod,
                           firstEMAMultiplier: Double = firstEMAMultiplier,
                           secondEMAMultiplier: Double = secondEMAMultiplier): ChaikinOscillator = ChaikinOscillator(
    getFirstEMAofADL(accDistLine, prevData, firstPeriod, firstEMAMultiplier),
    getSecondEMAofADL(accDistLine, prevData, secondPeriod, secondEMAMultiplier)
  )

  private def getFirstEMAofADL(accDistLine: Double, prevData: List[TickData], period: Int, multiplier: Double): Option[Double] = prevData.headOption match {
    case Some(x) if x.chaikinOscillator.isDefined && x.chaikinOscillator.get.firstEMAofADL.isDefined =>
      Some(QAT.getExponentialMovingAverage(multiplier, accDistLine, x.chaikinOscillator.get.firstEMAofADL.get))
    case Some(x) if prevData.take(period - 1).count(x => x.accumulationDistribution.isDefined) == (period - 1) =>
      Some(QAT.getMean(accDistLine :: prevData.take(period - 1).map(x => x.accumulationDistribution.get.accDistLine)))
    case _ => None
  }

  private def getSecondEMAofADL(accDistLine: Double, prevData: List[TickData], period: Int, multiplier: Double): Option[Double] = prevData.headOption match {
    case Some(x) if x.chaikinOscillator.isDefined && x.chaikinOscillator.get.secondEMAofADL.isDefined =>
      Some(QAT.getExponentialMovingAverage(multiplier, accDistLine, x.chaikinOscillator.get.secondEMAofADL.get))
    case Some(x) if prevData.take(period - 1).count(x => x.accumulationDistribution.isDefined) == (period - 1) =>
      Some(QAT.getMean(accDistLine :: prevData.take(period - 1).map(x => x.accumulationDistribution.get.accDistLine)))
    case _ => None
  }
}

