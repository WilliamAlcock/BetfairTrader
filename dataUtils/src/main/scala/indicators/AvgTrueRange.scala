package indicators

import play.api.libs.json.Json

trait AvgTrueRangeData extends TickData {
  val avgTrueRange: AvgTrueRange
}

case class AvgTrueRange(trueRange: Double, avg: Option[Double])

object AvgTrueRange extends QAT {
  implicit val formatAvgTrueRange = Json.format[AvgTrueRange]

  val period = 14 // look-back period

  def getNext(tick: Tick, prevData: List[AvgTrueRangeData], period: Int = period): AvgTrueRange = {
    val trueRange = getTrueRange(tick.range, prevData)
    AvgTrueRange(trueRange, getAvg(trueRange, prevData, period = period))
  }

  private def getTrueRange(range: Range, prevData: List[AvgTrueRangeData]): Double = prevData.headOption match {
    case Some(x) => Math.max(Math.max(range.range, Math.abs(range.high - x.tick.close)), Math.abs(range.low - x.tick.close))
    case None => range.range
  }

  private def getAvg(trueRange: Double, prevData: List[AvgTrueRangeData], period: Int = period): Option[Double] = prevData.headOption match {
    case Some(x) if x.avgTrueRange.avg.isDefined => Some(((x.avgTrueRange.avg.get * (period - 1)) + trueRange) / period)
    case Some(x) if prevData.size >= (period - 1) => Some(getMean(trueRange :: prevData.take(period - 1).map(x => x.avgTrueRange.trueRange)))
    case _ => None
  }
}