package core.dataModel.indicators

case class MoneyFlowIndex(rawFlow: Option[Double],
                          posFlow: Option[Double],
                          negFlow: Option[Double],
                          avgPosFlow: Option[Double],
                          avgNegFlow: Option[Double],
                          ratio: Option[Double],
                          index: Option[Double])

object MoneyFlowIndex {
  val period = 14 // look-back period

  def getMoneyFlowIndex(closeDelta: Option[Double], typicalPrice: Double, volume: Double, prevData: List[TickData], period: Int = period): MoneyFlowIndex = {
    val rawFlow = getRawFlow(closeDelta, typicalPrice, volume)
    val posFlow = getPosFlow(closeDelta, rawFlow)
    val negFlow = getNegFlow(closeDelta, rawFlow)
    val avgPosFlow = getAvgPosFlow(posFlow, prevData, period = period)
    val avgNegFlow = getAvgNegFlow(negFlow, prevData, period = period)
    val ratio = getRatio(avgPosFlow, avgNegFlow)
    MoneyFlowIndex(rawFlow, posFlow, negFlow, avgPosFlow, avgNegFlow, ratio, getIndex(ratio))
  }

  private def getRawFlow(closeDelta: Option[Double], typicalPrice: Double, volume: Double): Option[Double] = closeDelta match {
    case Some(x) if x == 0.0 => Some(0.0)
    case Some(x) => Some(typicalPrice * volume)
    case _ => None
  }

  private def getPosFlow(closeDelta: Option[Double], rawFlow: Option[Double]): Option[Double] = closeDelta match {
    case Some(x) if x > 0.0 => rawFlow
    case Some(x) => Some(0.0)
    case _ => None
  }

  private def getNegFlow(closeDelta: Option[Double], rawFlow: Option[Double]): Option[Double] = closeDelta match {
    case Some(x) if x < 0.0 => rawFlow
    case Some(x) => Some(0.0)
    case _ => None
  }

  private def getAvgPosFlow(posFlow: Option[Double], prevData: List[TickData], period: Int): Option[Double] = (posFlow, prevData.take(period - 1)) match {
    case (Some(x), y) if y.count(x => x.moneyFlowIndex.isDefined && x.moneyFlowIndex.get.posFlow.isDefined) == (period - 1) =>
      Some(x + y.map(_.moneyFlowIndex.get.posFlow.get).sum)
    case _ => None
  }

  private def getAvgNegFlow(negFlow: Option[Double], prevData: List[TickData], period: Int): Option[Double] = (negFlow, prevData.take(period - 1)) match {
    case (Some(x), y) if y.count(x => x.moneyFlowIndex.isDefined && x.moneyFlowIndex.get.negFlow.isDefined) == (period - 1) =>
      Some(x + y.map(_.moneyFlowIndex.get.negFlow.get).sum)
    case _ => None
  }

  private def getRatio(avgPosFlow: Option[Double], avgNegFlow: Option[Double]): Option[Double] = (avgPosFlow, avgNegFlow) match {
    case (Some(x), Some(y)) => if (y == 0.0) Some(y) else Some(x / y)
    case _ => None
  }

  private def getIndex(ratio: Option[Double]): Option[Double] = ratio match {
    case Some(x) => Some(100 - (100 / (1 + x)))
    case _ => None
  }
}
