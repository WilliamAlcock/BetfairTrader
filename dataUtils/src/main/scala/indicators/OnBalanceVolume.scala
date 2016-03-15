package indicators

trait OnBalanceVolumeData extends TickData {
  val onBalanceVolume: Double
}

object OnBalanceVolume {
  def getNext(tick: Tick, prevData: List[OnBalanceVolumeData]): Double = prevData.headOption match {
    case Some(x) => (tick.close - x.tick.close) match {
      case y if y > 0 => x.onBalanceVolume + tick.volume
      case y if y < 0 => x.onBalanceVolume - tick.volume
      case _ => x.onBalanceVolume
    }
    case _ => 0.0
  }
}