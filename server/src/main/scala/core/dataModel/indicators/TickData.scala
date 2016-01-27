package core.dataModel.indicators

case class TickData(range: Range,
                    close: Double,
                    volume: Double,
                    typicalPrice: Double,
                    closeDelta: Option[Double],
                    accumulationDistribution: Option[AccumulationDistribution],
                    avgTrueRange: Option[AvgTrueRange],
                    bollingerBands: Option[BollingerBands],
                    chaikinOscillator: Option[ChaikinOscillator],
                    commodityChannelIndex: Option[CommodityChannelIndex],
                    detrendedPriceOscillator: Option[DetrendedPriceOscillator],
                    easeOfMovement: Option[EaseOfMovement],
                    macd: Option[MACD],
                    massIndex: Option[MassIndex],
                    moneyFlowIndex: Option[MoneyFlowIndex],
                    onBalanceVolume: Option[OnBalanceVolume],
                    rateOfChange: Option[RateOfChange],
                    relativeStrengthIndex: Option[RelativeStrengthIndex],
                    stochasticOscillator: Option[StochasticOscillator],
                    williamsPercentR: Option[WilliamsPercentR])

object TickData {
  def getNextTick(range: Range,
                  close: Double,
                  volume: Double,
                  prevData: List[TickData],
                  typicalPrice: Option[Double] = None,
                  accumulationDistribution: Boolean = false,
                  avgTrueRange: Boolean = false,
                  bollingerBands: Boolean = false,
                  chaikinOscillator: Boolean = false,
                  commodityChannelIndex: Boolean = false,
                  detrendedPriceOscillator: Boolean = false,
                  easeOfMovement: Boolean = false,
                  macd: Boolean = false,
                  massIndex: Boolean = false,
                  moneyFlowIndex: Boolean = false,
                  onBalanceVolume: Boolean = false,
                  rateOfChange: Boolean = false,
                  relativeStrengthIndex: Boolean = false,
                  stochasticOscillator: Boolean = false,
                  williamsPercentR: Boolean = false): TickData = {

    val accDist = if (accumulationDistribution || chaikinOscillator) {
      Some(AccumulationDistribution.getAccumulationDistribution(close, range, volume, prevData))
    } else None

    val _typicalPrice = typicalPrice.getOrElse(getTypicalPrice(close, range))
    val closeDelta = getCloseDelta(close, if (prevData.isEmpty) None else Some(prevData.head.close))

    TickData(
      range,
      close,
      volume,
      _typicalPrice,
      closeDelta,
      accDist,
      if (avgTrueRange) Some(AvgTrueRange.getAvgTrueRange(close, range, prevData)) else None,
      if (bollingerBands) Some(BollingerBands.getBollingerBands(close, prevData)) else None,
      if (chaikinOscillator) Some(ChaikinOscillator.getChaikinOscillator(accDist.get.accDistLine, prevData)) else None,
      if (commodityChannelIndex) Some(CommodityChannelIndex.getCommodityChannelIndex(_typicalPrice, prevData)) else None,
      if (detrendedPriceOscillator) Some(DetrendedPriceOscillator.getDetrendedPriceOscillator(close, prevData)) else None,
      if (easeOfMovement) Some(EaseOfMovement.getEaseOfMovement(range, volume, prevData)) else None,
      if (macd) Some(MACD.getMACD(close, prevData)) else None,
      if (massIndex) Some(MassIndex.getMassIndex(range, prevData)) else None,
      if (moneyFlowIndex) Some(MoneyFlowIndex.getMoneyFlowIndex(closeDelta, _typicalPrice, volume, prevData)) else None,
      if (onBalanceVolume) Some(OnBalanceVolume.getOBV(close, volume, prevData)) else None,
      if (rateOfChange) Some(RateOfChange.getRateOfChange(close, prevData)) else None,
      if (relativeStrengthIndex) Some(RelativeStrengthIndex.getRelativeStrengthIndex(closeDelta, prevData)) else None,
      if (stochasticOscillator) Some(StochasticOscillator.getStochasticOscillator(range, close, prevData)) else None,
      if (williamsPercentR) Some(WilliamsPercentR.getWilliamsPercentR(close, range, prevData)) else None
    )
  }

  private def getTypicalPrice(close: Double, range: Range) = (close + range.high + range.low) / 3

  private def getCloseDelta(close: Double, prevClose: Option[Double]): Option[Double] = prevClose match {
    case Some(x) => Some(close - x)
    case _ => None
  }
}