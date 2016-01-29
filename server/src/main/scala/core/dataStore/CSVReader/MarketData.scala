package core.dataStore.csvReader

import core.api.output.MarketBookUpdate
import domain.{Runner, MarketBook, RunnerCatalog}
import org.joda.time.DateTime

case class MarketData(marketId: String, runners: Set[RunnerCatalog] = Set.empty, data: List[CSVData] = List.empty) extends Data {
  def addCSV(csv: CSVData): MarketData = this.copy(runners = runners + RunnerCatalog(csv.selectionId, csv.runnerName, 0), data = csv :: data)

  private def sortData(): List[CSVData] = this.data.sortBy(x => x.timestamp.getMillis)

  private def sortAndGroupData(): List[(DateTime, List[CSVData])] = this.data.groupBy(_.timestamp).toList.sortBy{case (x,y) => x.getMillis}

  private def getDefaultMarketBook(): MarketBook = MarketBook(
    this.marketId,                // marketId
    isMarketDataDelayed = false,
    "Active",                     // status
    0,                            // betDelay
    bspReconciled = false,
    complete = false,
    inplay = false,
    0,                            // numberOfWinners
    this.runners.size,            // numberOfRunners
    this.runners.size,            // numberOfActiveRunners
    None,                         // lastMatchTime
    0.0,                          // totalMatched
    0.0,                          // totalAvailable
    crossMatching = false,
    runnersVoidable = false,
    1,                            // version
    Set.empty[Runner])            // runners

  private def updateMarketBook(marketBooks: List[MarketBookUpdate], timestamp: DateTime, csvData: List[CSVData]): List[MarketBookUpdate] = {
    require(csvData.forall(x => x.inplay) || !csvData.forall(x => x.inplay))          // check all the inplay flags are the same
    val runners: Set[Runner] = csvData.map(_.toRunner()).toSet
    // TODO check status
    val lastMarketBook = marketBooks.head.data
    val lastRunners: Map[Long, Runner] = lastMarketBook.runners.groupBy(_.selectionId).mapValues(_.head)

    // updates
    val newRunners: Set[Runner] = runners.foldLeft(lastRunners)((acc, x) => acc + (x.selectionId -> x)).values.toSet
    val totalMatched: Double = newRunners.foldLeft(0.0)((acc, x) => acc + x.totalMatched.getOrElse(0.0))
    val inplay: Boolean = csvData.head.inplay

    MarketBookUpdate(timestamp, lastMarketBook.copy(inplay = inplay, totalMatched = totalMatched, runners = newRunners)) :: marketBooks
  }

  def convertToMarketBookUpdates(): List[MarketBookUpdate] =
    sortAndGroupData().foldLeft(List(MarketBookUpdate(new DateTime(0), getDefaultMarketBook()))){case (x, (y,z)) => updateMarketBook(x, y, z)}

  def printSortedData() = sortAndGroupData().foreach{case(x, y) => println(x); y.foreach(println(_))}
}