package core.dataModel.indicators

case class RateOfChange(roc: Option[Double])

object RateOfChange {
  val period = 12 // look-back period

  def getRateOfChange(close: Double, prevData: List[TickData], period: Int = period): RateOfChange = prevData.size match {
    case x if x >= period => RateOfChange(Some(((close - prevData(period - 1).close) / prevData(period - 1).close) * 100))
    case _ => RateOfChange(None)
  }
}