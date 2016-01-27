package core.dataModel.indicators

case class OnBalanceVolume(obv: Double)

object OnBalanceVolume {
  def getOBV(close: Double, volume: Double, prevData: List[TickData]): OnBalanceVolume = prevData.headOption match {
    case Some(x) => (close - x.close) match {
      case y if y > 0 => OnBalanceVolume(x.onBalanceVolume.getOrElse(OnBalanceVolume(0.0)).obv + volume)
      case y if y < 0 => OnBalanceVolume(x.onBalanceVolume.getOrElse(OnBalanceVolume(0.0)).obv - volume)
      case _ => OnBalanceVolume(x.onBalanceVolume.getOrElse(OnBalanceVolume(0.0)).obv)
    }
    case _ => OnBalanceVolume(0.0)
  }
}