package service.simService

import domain.Side.Side
import domain._
import org.scalatest.{Matchers, FlatSpec}
import TestHelpers._
import org.scalatest.prop.TableDrivenPropertyChecks._

class UtilsSpec extends FlatSpec with Matchers {

  "updateExchangePrices" should "add orders sizeRemaining and sizeMatched to exchangePrices size" in {
    val exchangePrices = ExchangePrices(
      List(PriceSize(3, 1)),
      List(PriceSize(4, 2)),
      List(PriceSize(3, 10))
    )

    val orders = Set[Order](
      generateOrder("order1", 2, 10, Side.LAY, sizeRemaining = Some(10)),
      generateOrder("order2", 3, 10, Side.LAY, sizeRemaining = Some(5), sizeMatched = 5).copy(avgPriceMatched = 30),
      generateOrder("order3", 3, 10, Side.LAY, sizeRemaining = Some(15), sizeMatched = 5).copy(avgPriceMatched = 30),
      generateOrder("order4", 4, 10, Side.BACK, sizeRemaining = Some(10), sizeMatched = 5).copy(avgPriceMatched = 40),
      generateOrder("order5", 4, 10, Side.BACK, sizeRemaining = Some(15), sizeMatched = 5).copy(avgPriceMatched = 40),
      generateOrder("order6", 5, 10, Side.BACK, sizeRemaining = Some(5))
    )

    val expectedResult = ExchangePrices(
      List(PriceSize(3, 21), PriceSize(2, 10)),     // availableToBack in descending
      List(PriceSize(4, 27), PriceSize(5, 5)),      // availableToLay in ascending
      List(PriceSize(40, 10), PriceSize(3, 10), PriceSize(30, 10))      // tradedVolume in descending, price is avgPriceMatched
    )

    val result = Utils.updateExchangePrices(Some(exchangePrices), orders)

    result.get should equal (expectedResult)
  }




  "getMatchFromOrders" should "get the average price and size matched for the given side (BACK/LAY)" in {
    val orders = List(
      generateOrder("order1", 5, 100, Side.LAY, sizeMatched = 10).copy(avgPriceMatched = 2),
      generateOrder("order2", 5, 200, Side.LAY, sizeMatched = 10).copy(avgPriceMatched = 3),
      generateOrder("order3", 5, 300, Side.LAY, sizeMatched = 10).copy(avgPriceMatched = 4),
      generateOrder("order4", 5, 100, Side.BACK, sizeMatched = 10).copy(avgPriceMatched = 5),
      generateOrder("order5", 5, 200, Side.BACK, sizeMatched = 10).copy(avgPriceMatched = 6),
      generateOrder("order6", 5, 300, Side.BACK, sizeMatched = 10).copy(avgPriceMatched = 7)
    )

    val matchScenarios = Table(
      ("Side",      "Orders",   "Match"),
      (Side.BACK,   orders,     Match(None, None, Side.BACK, 6, 30, None)),
      (Side.LAY,    orders,     Match(None, None, Side.LAY, 3, 30, None)),
      (Side.BACK,   List.empty, Match(None, None, Side.BACK, 0, 0, None)),
      (Side.LAY,    List.empty, Match(None, None, Side.LAY, 0, 0, None))
    )

    forAll(matchScenarios)((side: Side, orders: List[Order], _match: Match) => {
      val result = Utils.getMatchFromOrders(orders, side)
      result should be (_match)
    })
  }
}

