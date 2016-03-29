package service.simService

import domain.Side.Side
import domain._

trait Utils {
  private def mergePrices(x: List[PriceSize], y: List[PriceSize]): List[PriceSize] =
    (x ++ y).groupBy(_.price).mapValues(_.foldLeft(0.0)((acc, x) => acc + x.size)).map{case(price, size) => PriceSize(price, size)}.toList

  // TODO test !!! this function should add
  // lay order sizeRemaining -> availableToBack
  // back order sizeRemaining -> availableToLay
  // all order sizeTraded -> tradedVolume
  def updateExchangePrices(exchangePrices: Option[ExchangePrices], orders: Set[Order]): Option[ExchangePrices] = exchangePrices match {
    case Some(x) =>
      val backOrders = orders.filter(x => x.side == Side.BACK && x.sizeRemaining > 0).toList.map(x => PriceSize(x.price, x.sizeRemaining))
      val layOrders = orders.filter(x => x.side == Side.LAY && x.sizeRemaining > 0).toList.map(x => PriceSize(x.price, x.sizeRemaining))
      val tradedVolume = orders.filter(_.sizeMatched > 0).toList.map(x => PriceSize(x.avgPriceMatched, x.sizeMatched)).toList

      Some(x.copy(
        availableToBack = mergePrices(layOrders, x.availableToBack).sortBy(- _.price),
        availableToLay = mergePrices(backOrders, x.availableToLay).sortBy(_.price),
        tradedVolume = mergePrices(tradedVolume, x.tradedVolume)
      ))
    case None => exchangePrices
  }

  // TODO add test for divide by 0
  def getMatchFromOrders(orders: List[Order], side: Side): Match = orders.foldLeft[Match](Match(None, None, side, 0, 0, None))((acc, x) => {
    if (x.side == side && x.sizeMatched > 0) {
      acc.copy(price = ((acc.price * acc.size) + (x.avgPriceMatched * x.sizeMatched)) / (acc.size + x.sizeMatched), size = acc.size + x.sizeMatched)
    } else acc
  })
}

object Utils extends Utils {}