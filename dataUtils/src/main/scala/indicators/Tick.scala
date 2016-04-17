package indicators

import domain.ExchangePrices
import play.api.libs.json.Json

trait TickData {
  val tick: Tick
}

case class Tick(close: Double, range: Range, volume: Double, closeDelta: Option[Double], weightOfMoney: Double, book: Option[ExchangePrices]) {
  val typicalPrice = (close + range.high + range.low) / 3
}

object Tick {

  implicit val formatTick = Json.format[Tick]

  def getNext(close: Double, range: Range, volume: Double, weightOfMoney: Double, prevData: List[TickData], book: Option[ExchangePrices] = None): Tick =
    Tick(close, range, volume, getCloseDelta(close, prevData), weightOfMoney, book)

  private def getCloseDelta(close: Double, prevData: List[TickData]): Option[Double] = prevData.headOption match {
    case Some(x) => Some((BigDecimal(close) - BigDecimal(x.tick.close)).toDouble)
    case _ => None
  }
}