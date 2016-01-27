package core.dataModel.indicators

case class AccumulationDistribution(moneyFlowMultiplier: Double, moneyFlowVolume: Double, accDistLine: Double)

object AccumulationDistribution {
  def getAccumulationDistribution(close: Double,
                                  range: Range,
                                  volume: Double,
                                  prevData: List[TickData]): AccumulationDistribution = {
    val moneyFlowMultiplier = getMoneyFlowMultiplier(close, range)
    val moneyFlowVolume = moneyFlowMultiplier * volume
    AccumulationDistribution(moneyFlowMultiplier, moneyFlowVolume, getAccDistLine(moneyFlowVolume, prevData))
  }

  private def getMoneyFlowMultiplier(close: Double, range: Range): Double = ((close - range.low) - (range.high - close)) / (range.high - range.low)

  private def getAccDistLine(moneyFlowVolume: Double, prevData: List[TickData]): Double = prevData.headOption match {
    case Some(x) if x.accumulationDistribution.isDefined => moneyFlowVolume + x.accumulationDistribution.get.accDistLine
    case _ => moneyFlowVolume
  }
}
