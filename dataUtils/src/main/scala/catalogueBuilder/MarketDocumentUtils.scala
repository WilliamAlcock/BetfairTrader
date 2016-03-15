package catalogueBuilder

import csvReader.CSVData
import domain._
import org.joda.time.DateTime

trait MarketDocumentUtils {

  private def sortAndGroupData(csvData: List[CSVData]): List[(DateTime, List[CSVData])] = csvData.groupBy(_.timestamp).toList.sortBy{case (x,y) => x.getMillis}

  private def getDefaultMarketBook(marketId: String, size: Int): MarketBook = MarketBook(
    marketId,                     // marketId
    isMarketDataDelayed = false,
    "Active",                     // status
    0,                            // betDelay
    bspReconciled = false,
    complete = false,
    inplay = false,
    0,                            // numberOfWinners
    size,                         // numberOfRunners
    size,                         // numberOfActiveRunners
    None,                         // lastMatchTime
    0.0,                          // totalMatched
    0.0,                          // totalAvailable
    crossMatching = false,
    runnersVoidable = false,
    1,                            // version
    Set.empty[Runner])            // runners

  private def updateMarketBook(marketBooks: List[MarketBookUpdate], timestamp: DateTime, csvData: List[CSVData]): List[MarketBookUpdate] = {
    val runners: Set[Runner] = csvData.map(_.toRunner).toSet
    // TODO check status
    val lastMarketBook = marketBooks.head.data
    val lastRunners: Map[Long, Runner] = lastMarketBook.runners.groupBy(_.selectionId).mapValues(_.head)

    // updates
    val newRunners: Set[Runner] = runners.foldLeft(lastRunners)((acc, x) => acc + (x.selectionId -> x)).values.toSet
    val totalMatched: Double = newRunners.foldLeft(0.0)((acc, x) => acc + x.totalMatched.getOrElse(0.0))
    val inplay: Boolean = csvData.head.inplay

    MarketBookUpdate(timestamp, lastMarketBook.copy(inplay = inplay, totalMatched = totalMatched, runners = newRunners)) :: marketBooks
  }

  private def getRunners(csvData: List[CSVData]): Set[RunnerCatalog] = csvData.map(x => RunnerCatalog(x.selectionId, x.runnerName, 0, None, None)).toSet

  private def getMarketCatalogue(csvData: List[CSVData]): MarketCatalogue = MarketCatalogue(
    csvData(0).marketId,
    csvData(0).getMarketName,
    Some(csvData(0).startTime),
    None,
    0.0,
    Some(getRunners(csvData).toList),
    None,
    None,
    Event(csvData(0).eventId, csvData(0).getEventName, Some(csvData(0).getCountryCode), "", None, csvData(0).startTime)
  )

  private def getData(csvData: List[CSVData], defaultMarketBook: MarketBook, marketStartTime: DateTime): List[MarketBookUpdate] = {
    sortAndGroupData(csvData).foldLeft(List(MarketBookUpdate(DateTime.now(), defaultMarketBook))){
      case (x, (y, z)) => updateMarketBook(x, y, z)
    }.dropRight(1)
  }

  def fromCSVData(csvData: List[CSVData]): MarketDocument = {
    require(!csvData.isEmpty, "csvData cannot be empty")
    require(csvData.forall(x => x.marketId == csvData(0).marketId), "all records must have the same marketId")
    val marketId = csvData(0).marketId
    val catalog = getMarketCatalogue(csvData)
    MarketDocument(
      marketId = marketId,
      marketCatalogue = Some(catalog),
      data = getData(csvData, getDefaultMarketBook(marketId, catalog.runners.get.size), catalog.marketStartTime.get)
    )
  }
}


