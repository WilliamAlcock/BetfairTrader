package core.dataModel.indicators

case class EaseOfMovement(distanceMoved: Option[Double], boxRatio: Double, emv: Option[Double], smaOfEMV: Option[Double])

object EaseOfMovement {
  val period = 14 // 14 period SMA of EMV

  def getEaseOfMovement(range: Range, volume: Double, prevData: List[TickData]): EaseOfMovement = {
    val distanceMoved = getDistanceMoved(range, prevData)
    val boxRatio = getBoxRatio(range, volume)
    val emv = getEMV(distanceMoved, boxRatio)
    EaseOfMovement(distanceMoved, volume, emv, getSMAofEMV(emv, prevData, period = period))
  }

  private def getDistanceMoved(range: Range, prevData: List[TickData]): Option[Double] = prevData.headOption match {
    case Some(x) => Some(((range.high + range.low) / 2) - ((x.range.high + x.range.low) / 2))
    case _ => None
  }

  private def getBoxRatio(range: Range, volume: Double): Double = if (range.range == 0) 0.0 else (volume / 100000000) / range.range

  private def getEMV(distanceMoved: Option[Double], boxRatio: Double): Option[Double] = distanceMoved match {
    case Some(x) => Some(if (boxRatio == 0) 0.0 else x / boxRatio)
    case _ => None
  }

  private def getSMAofEMV(emv: Option[Double], prevData: List[TickData], period: Int = period): Option[Double] = (emv, prevData.headOption) match {
    case (Some(x), Some(y)) if y.easeOfMovement.isDefined && y.easeOfMovement.get.smaOfEMV.isDefined =>
      Some(QAT.getSimpleMovingAverage(period, x, prevData(period - 1).easeOfMovement.get.emv.get, y.easeOfMovement.get.smaOfEMV.get))
    case (Some(x), Some(y)) if prevData.take(period - 1).count(x => x.easeOfMovement.isDefined && x.easeOfMovement.get.emv.isDefined) == (period - 1) =>  // check emv is defined for all 14 previous records before calculating mean
      Some(QAT.getMean(x :: prevData.take(period - 1).map(x => x.easeOfMovement.get.emv.get)))
    case _ => None
  }
}


