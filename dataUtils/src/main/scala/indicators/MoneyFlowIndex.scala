package indicators

import play.api.libs.json.Json

trait MoneyFlowIndexData extends TickData {
  val moneyFlowIndex: MoneyFlowIndex
}

case class MoneyFlowIndex(rawFlow: Option[Double],
                          posFlow: Option[Double],
                          negFlow: Option[Double],
                          avgPosFlow: Option[Double],
                          avgNegFlow: Option[Double]) {
  val ratio: Option[Double] = (avgPosFlow, avgNegFlow) match {
    case (Some(x), Some(y)) => if (y == 0.0) Some(y) else Some(x / y)
    case _ => None
  }
  val index: Option[Double] = ratio match {
    case Some(x) => Some(100 - (100 / (1 + x)))
    case _ => None
  }
}

object MoneyFlowIndex {
  implicit val formatMoneyFlowIndex = Json.format[MoneyFlowIndex]

  val period = 14 // look-back period

  def getNext(tick: Tick, prevData: List[MoneyFlowIndexData], period: Int = period): MoneyFlowIndex = {
    val rawFlow = getRawFlow(tick.closeDelta, tick.typicalPrice, tick.volume)
    val posFlow = getPosFlow(tick.closeDelta, rawFlow)
    val negFlow = getNegFlow(tick.closeDelta, rawFlow)
    val avgPosFlow = getAvgPosFlow(posFlow, prevData, period = period)
    val avgNegFlow = getAvgNegFlow(negFlow, prevData, period = period)
    MoneyFlowIndex(rawFlow, posFlow, negFlow, avgPosFlow, avgNegFlow)
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

  private def getAvgPosFlow(posFlow: Option[Double], prevData: List[MoneyFlowIndexData], period: Int): Option[Double] = (posFlow, prevData.take(period - 1)) match {
    case (Some(x), y) if y.count(x => x.moneyFlowIndex.posFlow.isDefined) == (period - 1) => Some(x + y.map(_.moneyFlowIndex.posFlow.get).sum)
    case _ => None
  }

  private def getAvgNegFlow(negFlow: Option[Double], prevData: List[MoneyFlowIndexData], period: Int): Option[Double] = (negFlow, prevData.take(period - 1)) match {
    case (Some(x), y) if y.count(x => x.moneyFlowIndex.negFlow.isDefined) == (period - 1) => Some(x + y.map(_.moneyFlowIndex.negFlow.get).sum)
    case _ => None
  }
}
