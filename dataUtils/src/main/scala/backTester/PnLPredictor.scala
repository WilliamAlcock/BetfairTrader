package backTester

import domain.{ExchangePrices, Side}
import domain.Side.Side
import indicators.Indicators
import randomForest.RandomForest

case class Trade(side: Side, price: Double, size: Int)

trait PnLPredictor {

  def getPriceAbove(price: Double): Double = price match {
    case x if x < 2 => x + 0.01
    case x if x < 3 => x + 0.02
    case x if x < 4 => x + 0.05
    case x if x < 6 => x + 0.1
    case x if x < 10 => x + 0.2
    case x if x < 20 => x + 0.5
    case x if x < 30 => x + 1
    case x if x < 50 => x + 2
    case x if x < 100 => x + 5
    case x => x + 10
  }

  sealed case class State(trades: List[Trade] = List.empty, classifications: List[String] = List.empty)

  /*
  Examples:
  price,        output
  0.9091             1 / (1 - 0.0909) = 1.1
  0.66               1 / (1 - 0.33)   = 1.5
  0.33               1 / (1 - 0.66)   = 3
  0.10               1 / (1 - 0.90)   = 10


  normalised price is the 100 - percentage probability
  normalised price will range 0 -> 100
  */
  def probabilityOfLoseToPrice(prob: Double): Double = if (prob == 0) prob else 1 / (1 - prob)

  def getPnL(trades: List[Trade]): Double = trades.map {
    //                               Profit                     Liability
    case x if x.side == Side.BACK => (x.size * (x.price - 1)) - x.size
    case x if x.side == Side.LAY  => x.size                   - (x.size * (x.price - 1))
  }.sum

  def addClosingTrade(ex: ExchangePrices, trades: List[Trade]): List[Trade] = {

    (trades.filter(_.side == Side.BACK).map(_.size).sum, trades.filter(_.side == Side.LAY).map(_.size).sum) match {
      case (sizeToBack, sizeToLay) if sizeToBack == sizeToLay => trades
      case (sizeToBack, sizeToLay) if sizeToBack > sizeToLay  =>
//        val tradingPrice = if (ex.availableToBack.headOption.isDefined && ex.availableToLay.headOption.isDefined) {
//          getTradingPrice(ex.availableToBack.head.price, ex.availableToLay.head.price)
//        } else {
//          ex.availableToLay.head.price
//        }
        Trade(Side.LAY, ex.availableToLay.head.price, sizeToBack - sizeToLay) :: trades
      case (sizeToBack, sizeToLay) if sizeToBack < sizeToLay  =>
//        val tradingPrice = if (ex.availableToBack.headOption.isDefined && ex.availableToLay.headOption.isDefined) {
//          getTradingPrice(ex.availableToBack.head.price, ex.availableToLay.head.price)
//        } else {
//          ex.availableToBack.head.price
//        }
        Trade(Side.BACK, ex.availableToBack.head.price, sizeToLay - sizeToBack) :: trades
    }
  }


  def getTradingPrice(back: Double, lay: Double): Double = ((lay - back) / 2) + back

  def updateTrades(trades: List[Trade], classifications: List[String], ex: ExchangePrices, numIndicators: Int): List[Trade] = {
    // only trade if market is NOT choice
    if (ex.availableToBack.nonEmpty && ex.availableToLay.nonEmpty && ex.availableToLay.head.price == getPriceAbove(ex.availableToBack.head.price)) {
//      val tradingPrice = getTradingPrice(ex.availableToBack.head.price, ex.availableToLay.head.price)
      if (trades.isEmpty) {
        classifications.head match {
          case "UP"   if classifications.take(numIndicators).count(x => x == classifications.head) == numIndicators =>
            Trade(Side.LAY, ex.availableToBack.head.price/*tradingPrice ex.availableToLay.head.price*/, 1) :: trades
          case "DOWN" if classifications.take(numIndicators).count(x => x == classifications.head) == numIndicators =>
            Trade(Side.BACK, ex.availableToLay.head.price/*tradingPrice ex.availableToBack.head.price*/, 1) :: trades
          case _ => trades
        }
      } else if (trades.head.side == Side.BACK) {
        classifications.head match {
          case "UP"   if classifications.take(numIndicators).count(x => x == classifications.head) == numIndicators =>
            Trade(Side.LAY, ex.availableToBack.head.price, 1) :: Trade(Side.LAY, ex.availableToLay.head.price, 1) :: trades
          case _ => trades
        }
      } else {        // Side must == Side.LAY
        classifications.head match {
          case "DOWN" if classifications.take(numIndicators).count(x => x == classifications.head) == numIndicators =>
            Trade(Side.BACK, ex.availableToLay.head.price, 1) :: Trade(Side.BACK, ex.availableToBack.head.price, 1) :: trades
          case _ => trades
        }
      }
    } else trades
  }

  def updateState(state: State, ex: ExchangePrices, classification: String, numIndicators: Int): State = {
    val newClasses = classification :: state.classifications

    state.copy(
      trades = updateTrades(state.trades, newClasses, ex, numIndicators),
      classifications = newClasses
    )
  }

  def testSelection(indicators: List[Indicators], model: RandomForest, numIndicators: Int): List[Trade] = {
    val orderedIndicators = indicators.sortBy(_.timestamp)
    val state = orderedIndicators.foldLeft(State())((state, indicator) =>
      updateState(state, indicator.tick.book.get, model.getClassification(Indicators.getFeatures(indicator).map(_.get)), numIndicators)
    )
    addClosingTrade(orderedIndicators.last.tick.book.get, state.trades)
  }

  def testMarket(indicators: List[Indicators], model: RandomForest, numIndicators: Int): Double = {
    indicators.groupBy(_.selectionId).mapValues(x => testSelection(x, model, numIndicators)).foldLeft(0.0)((acc, selection) => selection match {
      case (id, trades) =>
        val pnl = getPnL(trades)
        println("selectionId", id, "trades", trades, "pnl", pnl)
        acc + pnl
      case _ => 0.0
    })
  }
}