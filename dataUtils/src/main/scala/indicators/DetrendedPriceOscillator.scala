package indicators

import play.api.libs.json.Json

trait DetrendedPriceOscillatorData extends TickData {
  val detrendedPriceOscillator: DetrendedPriceOscillator
}

case class DetrendedPriceOscillator(smaOfClose: Option[Double], oscillator: Option[Double])

object DetrendedPriceOscillator extends QAT {
  implicit val formatDetrendedPriceOscillator = Json.format[DetrendedPriceOscillator]
  val period = 20 // look-back period

  def getNext(tick: Tick, prevData: List[DetrendedPriceOscillatorData], period: Int = period): DetrendedPriceOscillator = {
    val sma = getSMAOfClose(tick.close, prevData)
    DetrendedPriceOscillator(sma, getDetrendedPriceOscillator(sma, prevData))
  }

  private def getSMAOfClose(close: Double, prevData: List[DetrendedPriceOscillatorData], period: Int = period): Option[Double] =
    getSMA(prevData, (d: DetrendedPriceOscillatorData) => d.tick.close, period, close, if (prevData.isEmpty) None else prevData.head.detrendedPriceOscillator.smaOfClose)

  private def getDetrendedPriceOscillator(sma: Option[Double], prevData: List[DetrendedPriceOscillatorData], period: Int = period): Option[Double] = sma match {
    case Some(x) if prevData.size >= (period / 2) => Some(x - prevData(period / 2).tick.close)
    case _ => None
  }
}

