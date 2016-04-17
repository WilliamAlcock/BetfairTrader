package indicators

import domain.ExchangePrices
import play.api.libs.json.Json
import scala.math.BigDecimal

case class Indicators(selectionId: Long,
                      timestamp: Long,
                      startTime: Long,
                      tick: Tick,
                      accumulationDistribution: AccumulationDistribution,
//                      avgTrueRange: AvgTrueRange,
//                      bollingerBands: BollingerBands,
                      chaikinOscillator: ChaikinOscillator,
                      commodityChannelIndex: CommodityChannelIndex,
//                      detrendedPriceOscillator: DetrendedPriceOscillator,
//                      easeOfMovement: EaseOfMovement,
                      macd: MACD,
//                      massIndex: MassIndex,
                      moneyFlowIndex: MoneyFlowIndex,
//                      onBalanceVolume: Double,
                      rateOfChange: Option[Double],
                      relativeStrengthIndex: RelativeStrengthIndex,
                      stochasticOscillator: StochasticOscillator,
                      williamsPercentR: WilliamsPercentR)
  extends TickData
  with AccumulationDistributionData
//  with AvgTrueRangeData
//  with BollingerBandData
  with ChaikinOscillatorData
  with CommodityChannelIndexData
//  with DetrendedPriceOscillatorData
//  with EaseOfMovementData
  with MACDData
//  with MassIndexData
  with MoneyFlowIndexData
//  with OnBalanceVolumeData
  with RateOfChangeData
  with RelativeStrengthIndexData
  with StochasticOscillatorData
  with WilliamsPercentRData

object Indicators {
  implicit val formatInterval = Json.format[Indicators]

  def getNext(selectionId: Long, timestamp: Long, startTime: Long, range: Range, close: Double, volume: Double, weightOfMoney: Double, prevData: List[Indicators], book: Option[ExchangePrices]): Indicators = {

    val tick = Tick.getNext(close, range, volume, weightOfMoney, prevData, book)
    val accumulationDistribution = AccumulationDistribution.getNext(tick, prevData)

    Indicators(
      selectionId,
      timestamp,
      startTime,
      tick,
      accumulationDistribution,
//      AvgTrueRange.getNext(tick, prevData),
//      BollingerBands.getNext(tick, prevData),
      ChaikinOscillator.getNext(accumulationDistribution, prevData),
      CommodityChannelIndex.getNext(tick, prevData),
//      DetrendedPriceOscillator.getNext(tick, prevData),
//      EaseOfMovement.getNext(tick, prevData),
      MACD.getNext(tick, prevData),
//      MassIndex.getNext(tick, prevData),
      MoneyFlowIndex.getNext(tick, prevData),
//      OnBalanceVolume.getNext(tick, prevData),
      RateOfChange.getNext(tick, prevData),
      RelativeStrengthIndex.getNext(tick, prevData),
      StochasticOscillator.getNext(tick, prevData),
      WilliamsPercentR.getNext(tick, prevData)
    )
  }

  private def applyToOption(o: Option[Double], f: (Double) => Double) = o match {
    case Some(x) => Some(f(x))
    case None => None
  }

  private def round(o: Option[Double], dp: Int): Option[Double] =
    applyToOption(o, (x) => BigDecimal(x).setScale(dp, BigDecimal.RoundingMode.HALF_UP).toDouble)

  private def add(o: Option[Double], a: Double): Option[Double] = applyToOption(o, (x) => x + a)

  private def div(o: Option[Double], di: Double): Option[Double] = applyToOption(o, (x) => x / di)

  private def mult(o: Option[Double], m: Double): Option[Double] = applyToOption(o, (x) => x * m)

  def getFeatures(i: Indicators): List[Option[Double]] = List(
    Some(i.accumulationDistribution.line),
    i.accumulationDistribution.lineDelta,
    i.chaikinOscillator.oscillator,
    i.chaikinOscillator.oscillatorDelta,
    i.commodityChannelIndex.index,
//    i.easeOfMovement.smaOfEMV,
    i.macd.histogram,
    i.moneyFlowIndex.index,
    i.rateOfChange,
    i.relativeStrengthIndex.rsi,
    i.stochasticOscillator.percentK,
    i.stochasticOscillator.percentD,
    i.stochasticOscillator.slowPercentD,
    i.williamsPercentR.percentR,
    Some(i.tick.weightOfMoney)
  ).map(round(_, 3))
}