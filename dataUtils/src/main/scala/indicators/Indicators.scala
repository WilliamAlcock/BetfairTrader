package indicators

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

  def getNext(selectionId: Long, timestamp: Long, startTime: Long, range: Range, close: Double, volume: Double, weightOfMoney: Double, prevData: List[Indicators]): Indicators = {

    val tick = Tick.getNext(close, range, volume, weightOfMoney, prevData)
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
    Some(i.accumulationDistribution.line),          // ?????
    i.accumulationDistribution.lineDelta,             // -> percentage delta on this period
    i.chaikinOscillator.oscillator,                 // 3day - 10day, + 3day above, - below, turn into percentage diff
    i.chaikinOscillator.oscillatorDelta,               // -> percentage delta on this period
    i.commodityChannelIndex.index,                    // increase look back period to 40, 0 Decimal Place
//    i.easeOfMovement.smaOfEMV,                      // ????
    i.macd.histogram,                      // -> * 100, 1 Decimal Place
    i.moneyFlowIndex.index,                           // 0 - 100 -> 1 Decimal Place
    i.rateOfChange,                 // -100 - 100 -> Add 100, Divide / 2, 1 Decimal Place
    i.relativeStrengthIndex.rsi,                      // 0 - 100 -> 1 Decimal Place
    i.stochasticOscillator.percentK,                  // 0 - 100 -> 1 Decimal Place
    i.stochasticOscillator.percentD,                  // 0 - 100 -> 1 Decimal Place
    i.stochasticOscillator.slowPercentD,              // 0 - 100 -> 1 Decimal Place
    i.williamsPercentR.percentR,             // -100 - 0  -> Add 100, 1 Decimal Place
    Some(i.tick.weightOfMoney)
  ).map(round(_, 3))
}