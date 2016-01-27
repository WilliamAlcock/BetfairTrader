package core.dataModel.indicators

case class AvgTrueRange(trueRange: Double, avg: Option[Double])

object AvgTrueRange {
  val period = 14 // look-back period

  def getAvgTrueRange(close: Double, range: Range, prevData: List[TickData], period: Int = period): AvgTrueRange = {
    val trueRange = getTrueRange(range, prevData)
    AvgTrueRange(trueRange, _getAvgTrueRange(trueRange, prevData, period = period))
  }

  private def getTrueRange(range: Range, prevData: List[TickData]): Double = prevData.headOption match {
    case Some(x) =>
      Math.max(Math.max(range.range, Math.abs(range.high - x.close)), Math.abs(range.low - x.close))
    case None => range.range
  }

  private def _getAvgTrueRange(trueRange: Double, prevData: List[TickData], period: Int): Option[Double] = prevData.headOption match {
    case Some(x) if x.avgTrueRange.isDefined && x.avgTrueRange.get.avg.isDefined =>
      Some(((x.avgTrueRange.get.avg.get * (period - 1)) + trueRange) / period)
    case Some(x) if prevData.take(period - 1).count(_.avgTrueRange.isDefined) == (period - 1) =>
      Some(QAT.getMean(trueRange :: prevData.take(period - 1).map(x => x.avgTrueRange.get.trueRange)))
    case _ => None
  }
}