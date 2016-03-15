package indicators

trait RateOfChangeData extends TickData {
  val rateOfChange: Option[Double]
}

object RateOfChange {
  val period = 12 // look-back period

  def getNext(tick: Tick, prevData: List[RateOfChangeData], period: Int = period): Option[Double] = prevData.size match {
    case x if x >= period => if (prevData(period - 1).tick.close == 0) Some(x) else Some(((tick.close - prevData(period - 1).tick.close) / prevData(period - 1).tick.close) * 100)
    case _ => None
  }
}