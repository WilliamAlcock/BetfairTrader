package core.dataModel

import domain.{MarketBook, MarketCatalogue}

// TODO commented out tickData, possibly move to machine learning algo

sealed case class MarketData(book: Option[MarketBook] = None,
                             catalogue: Option[MarketCatalogue] = None)
//                             tickData: HashMap[String, List[TickData]] = HashMap.empty)

object MarketData {
  def updateMarketBook(marketData: MarketData, marketBook: MarketBook): MarketData = marketData.copy(
    book = Some(marketBook)
//    tickData = updateTickData(
//      marketBook.runners,
//      getRunnersLookup(if (marketData.book.isDefined) marketData.book.get.runners else Set.empty[Runner]),
//      marketData.tickData
//    )
  )

  def updateMarketCatalogue(marketData: MarketData, marketCatalogue: MarketCatalogue): MarketData = marketData.copy(
    catalogue = Some(marketCatalogue)
  )

//  private def getRunnersLookup(runners: Set[Runner]): HashMap[String, Runner] =
//    runners.map(x => x.uniqueId -> x)(collection.breakOut): HashMap[String, Runner]
//
//  private def updateTickData(runners: Set[Runner],
//                             prevRunners: HashMap[String, Runner],
//                             prevData: HashMap[String, List[TickData]]): HashMap[String, List[TickData]] = runners.map(x => x.uniqueId ->
//    (TickData.getNextTick(
//      getRange(x, prevRunners.get(x.uniqueId)),
//      getClose(x),
//      getVolume(x, prevRunners.get(x.uniqueId)),
//      prevData.getOrElse(x.uniqueId, List.empty[TickData])
//    ) :: prevData.getOrElse(x.uniqueId, List.empty[TickData]))
//  )(collection.breakOut): HashMap[String, List[TickData]]

//  private def getRange(runner: Runner, prevRunner: Option[Runner]): Range = Range.getRange(
//    runner.ex,
//    if (prevRunner.isDefined) prevRunner.get.ex else None,
//    runner.lastPriceTraded
//  )
//
//  private def getClose(runner: Runner): Double = runner.lastPriceTraded.getOrElse(0.0)
//
//  private def getVolume(runner: Runner, prevRunner: Option[Runner]): Double = (runner.totalMatched, prevRunner) match {                                                                     // Volume
//    case (Some(x), Some(y)) => x - y.totalMatched.getOrElse(0.0)
//    case (Some(x), _) => x
//    case _ => 0.0
//  }
}
