package indicators

import play.api.libs.json.Json

trait EaseOfMovementData extends TickData {
  val easeOfMovement: EaseOfMovement
}

case class EaseOfMovement(distanceMoved: Option[Double], boxRatio: Double, emv: Option[Double], smaOfEMV: Option[Double])

object EaseOfMovement extends QAT {
  implicit val formatEaseOfMovement = Json.format[EaseOfMovement]

  val period = 14 // 14 period SMA of EMV

  def getNext(tick: Tick, prevData: List[EaseOfMovementData]): EaseOfMovement = {
    val distanceMoved = getDistanceMoved(tick.range, prevData)
    val boxRatio = getBoxRatio(tick.range, tick.volume)
    val emv = getEMV(distanceMoved, boxRatio)
    EaseOfMovement(distanceMoved, tick.volume, emv, getSMAofEMV(emv, prevData, period = period))
  }

  private def getDistanceMoved(range: Range, prevData: List[EaseOfMovementData]): Option[Double] = prevData.headOption match {
    case Some(x) => Some(((range.high + range.low) / 2) - ((x.tick.range.high + x.tick.range.low) / 2))
    case _ => None
  }

  private def getBoxRatio(range: Range, volume: Double): Double = if (range.range == 0) 0.0 else (volume / 100000000) / range.range

  private def getEMV(distanceMoved: Option[Double], boxRatio: Double): Option[Double] = distanceMoved match {
    case Some(x) => Some(if (boxRatio == 0) 0.0 else x / boxRatio)
    case _ => None
  }

  private def getSMAofEMV(emv: Option[Double], prevData: List[EaseOfMovementData], period: Int = period): Option[Double] =
    getSMA(prevData, (e: EaseOfMovementData) => e.easeOfMovement.emv, period, emv, if (prevData.isEmpty) None else prevData.head.easeOfMovement.smaOfEMV)
}


