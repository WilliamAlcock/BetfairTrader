package indicators

import play.api.libs.json.Json

trait ChaikinOscillatorData extends AccumulationDistributionData {
  val chaikinOscillator: ChaikinOscillator
}

case class ChaikinOscillator(firstEMAofADL: Option[Double], secondEMAofADL: Option[Double], oscillator: Option[Double], oscillatorDelta: Option[Double])

object ChaikinOscillator extends QAT {
  implicit val formatChaikinOscillator = Json.format[ChaikinOscillator]

  val firstPeriod = 3 // first ema look-back period
  val secondPeriod = 10 // second ema look-back period

  def getNext(accumulationDistribution: AccumulationDistribution,
              prevData: List[ChaikinOscillatorData],
              firstPeriod: Int = firstPeriod,
              secondPeriod: Int = secondPeriod): ChaikinOscillator = {
    val firstEMAofADL = getFirstEMAofADL(accumulationDistribution.line, prevData, firstPeriod)
    val secondEMAofADL = getSecondEMAofADL(accumulationDistribution.line, prevData, secondPeriod)
    val oscillator = getOscillator(firstEMAofADL, secondEMAofADL)
    val delta = getOscillatorDelta(oscillator, prevData)
    ChaikinOscillator(firstEMAofADL, secondEMAofADL, oscillator, delta)
  }

  private def getFirstEMAofADL(accDistLine: Double, prevData: List[ChaikinOscillatorData], period: Int = firstPeriod): Option[Double] =
    getEMA(prevData, (c: ChaikinOscillatorData) => c.accumulationDistribution.line, period, accDistLine, if (prevData.isEmpty) None else prevData.head.chaikinOscillator.firstEMAofADL)

  private def getSecondEMAofADL(accDistLine: Double, prevData: List[ChaikinOscillatorData], period: Int = secondPeriod): Option[Double] =
    getEMA(prevData, (c: ChaikinOscillatorData) => c.accumulationDistribution.line, period, accDistLine, if (prevData.isEmpty) None else prevData.head.chaikinOscillator.secondEMAofADL)

  private def getOscillator(firstEMAofADL: Option[Double], secondEMAofADL: Option[Double]): Option[Double] = (firstEMAofADL, secondEMAofADL) match {
    case (Some(x), Some(y)) => Some(x - y)
    case _ => None
  }

  /*
    Percentage delta
   */
  private def getOscillatorDelta(oscillator: Option[Double], prevData: List[ChaikinOscillatorData]): Option[Double] = (oscillator, prevData.headOption) match {
    case (Some(x), Some(y)) if y.chaikinOscillator.oscillator.isDefined =>
      if (y.chaikinOscillator.oscillator.get == 0.0) {
        Some(0.0)
      } else {
        Some(((x - y.chaikinOscillator.oscillator.get) / y.chaikinOscillator.oscillator.get) * 100)
      }
    case _ => None
  }
}

