package backTester

import dbIO.CSVData
import domain._
import indicators.Indicators
import org.joda.time.DateTime

trait BackTester {
  def getIntervalStartTime(timestamp: Long, raceStartTime: Long, interval: Long) = {
    timestamp - raceStartTime match {
      case x if x > 0 => timestamp - (x % interval)                   // after (inplay)
      case x if x < 0 => timestamp - (x % interval) - interval        // before (pre-race)
      case _ => timestamp
    }
  }

  def getRunnerFromCatalogue(m: RunnerCatalog): Runner = Runner(
    selectionId   = m.selectionId,
    handicap      = m.handicap,
    status        = "Active",
    totalMatched  = m.totalMatched
  )

  def getMarketBookFromCatalogue(m: MarketCatalogue): MarketBook = MarketBook(
    marketId              = m.marketId,
    isMarketDataDelayed   = false,
    status                = "Active",
    betDelay              = 0,
    bspReconciled         = false,
    complete              = false,
    inplay                = false,
    numberOfWinners       = 1,
    numberOfRunners       = m.runners.size,
    numberOfActiveRunners = m.runners.size,
    lastMatchTime         = None,
    totalMatched          = 0.0,
    totalAvailable        = 0.0,
    crossMatching         = false,
    runnersVoidable       = false,
    version               = 1,
    runners               = if (m.runners.isDefined) m.runners.get.map(x => getRunnerFromCatalogue(x)).toSet else Set.empty[Runner]
  )

  def updateRunner(prev: Runner, csvData: CSVData): Runner = prev.copy(
    status = csvData.status,
    totalMatched = csvData.totalMatched,
    lastPriceTraded = csvData.lastPriceMatched,
    ex = csvData.ex
  )

  def updateRunners(prev: Set[Runner], csvData: List[CSVData]): Set[Runner] = {
    prev.map(x => csvData.find(csv => csv.selectionId == x.selectionId) match {
      case Some(csvUpdate) => updateRunner(x, csvUpdate)
      case _ => x
    })
  }

  def getNextMarketBook(prev: MarketBook, csvData: List[CSVData]): MarketBook = {
    val runners = updateRunners(prev.runners, csvData)
    prev.copy(
      status = csvData.head.status,
      inplay = csvData.head.inplay,
      totalMatched = runners.map(_.totalMatched.getOrElse(0.0)).sum,
      runners = runners
    )
  }

  def getMarketBookUpdates(csvData: List[CSVData]): List[MarketBookUpdate] = {
    val catalogue = CSVData.getMarketCatalogue(csvData)
    val marketBookUpdate = MarketBookUpdate(new DateTime(0), getMarketBookFromCatalogue(catalogue))

    // sort ascending by timestamp and group by timestamp
    val data = csvData.groupBy(_.timestamp)
    val output = data.keys.toList.sortBy(x => x.getMillis).foldLeft[List[MarketBookUpdate]](List(marketBookUpdate))(
      (updates, nextTimestamp) => MarketBookUpdate(nextTimestamp, getNextMarketBook(updates.head.data, data(nextTimestamp))) :: updates
    )
    output.take(output.length - 1)
  }

  /*
  Examples:
  price,        output
  1.1           1 - 0.9091  = 0.0909
  1.5           1 - 0.66    = 0.33
  3             1 - 0.33    = 0.66
  10            1 - 0.10    = 0.90


  normalised price is the 100 - percentage probability
  normalised price will range 0 -> 100
  */
  def priceToProbabilityOfLose(price: Double): Double = if (price == 0) price else 1 - (1/price)

  def getIndicatorFromMarketBook(m: List[MarketBookUpdate], selectionId: Long, prev: List[Indicators], timestamp: Long, startTime: Long, priceFunction: (Double) => Double): Indicators =  {
    val prices = m.map(x => x.data.getRunner(selectionId).lastPriceTraded).filter(_.isDefined).map(_.get)
    val high = prices.max
    val low = prices.min
    val close = prices.last
    val volume = m.last.data.totalMatched - prev.head.tick.volume
    val book = m.last.data.getRunner(selectionId).ex

    if (m.isEmpty) {
      Indicators.getNext(
        selectionId,
        timestamp,
        startTime,
        indicators.Range(
          priceFunction(prev.head.tick.close),
          priceFunction(prev.head.tick.close)
        ),
        prev.head.tick.close,
        0.0,
        prev.head.tick.weightOfMoney,
        prev,
        None
      )
    } else {
      Indicators.getNext(
        selectionId,
        timestamp,
        startTime,
        indicators.Range(high, low),
        close,
        volume,
        CSVData.getWeightOfMoney(book),
        prev,
        book
      )
    }
  }
}
