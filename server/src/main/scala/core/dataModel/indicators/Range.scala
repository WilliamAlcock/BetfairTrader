package core.dataModel.indicators

import domain.{ExchangePrices, PriceSize}

case class Range(high: Double, low: Double) {
  def +(that: Option[Range]): Range = that match {
    case Some(x) => this + x
    case None => this
  }
  def +(that: Range): Range = Range(
    Math.max(this.high, that.high),
    Math.min(this.low, that.low)
  )
  val range: Double = high - low
}

// TODO add tests
// TODO should I be using 0.0 when it hasn't traded ??

object Range {
  private def getTradedDelta(ex: Option[ExchangePrices], prevEx: Option[ExchangePrices]): List[PriceSize] = (ex, prevEx) match {
    case (Some(x), Some(y)) =>
      val oldTradedLookUp = y.tradedVolume.map(x => x.price -> x.size)(collection.breakOut): Map[Double, Double]
      x.tradedVolume.map(x => PriceSize(x.price, x.size - oldTradedLookUp.getOrElse(x.price, 0.0))).sortBy(-_.price)
    case (Some(x), None) => x.tradedVolume
    case _ => List.empty
  }

  private def _getRange(tradedDelta: List[PriceSize], close: Option[Double] = None): Range = (tradedDelta.headOption, close) match {
    case (Some(x), _) => Range(x.price, tradedDelta.last.price)
    case (_, Some(x)) => Range(x, x)
    case _ => Range(0.0, 0.0)
  }

  def getRange(ex: Option[ExchangePrices], prevEx: Option[ExchangePrices], close: Option[Double]) = _getRange(getTradedDelta(ex, prevEx), close)
}

