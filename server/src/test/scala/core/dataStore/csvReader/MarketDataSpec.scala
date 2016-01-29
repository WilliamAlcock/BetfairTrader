package core.dataStore.csvReader

import core.api.output.MarketBookUpdate
import domain._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class MarketDataSpec extends FlatSpec with Matchers with MockFactory {
  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-ddHH:mm:ss.SSS").withZone(DateTimeZone.UTC)


  "MarketData.convertToMarketBookUpdates" should "convert a MarketData case class to a chronological list of MarketBookUpdate" in {
    val marketId = "TEST_ID"
    val marketStartTime = DateTime.parse("2014-09-1418:00:00.000", dateFormat)
    val timestamps = Map(
      1 -> DateTime.parse("2014-09-1417:10:00.000", dateFormat),
      2 -> DateTime.parse("2014-09-1417:15:00.000", dateFormat),
      3 -> DateTime.parse("2014-09-1417:20:00.000", dateFormat),
      4 -> DateTime.parse("2014-09-1417:30:00.000", dateFormat),
      5 -> DateTime.parse("2014-09-1417:35:00.000", dateFormat)
    )
    val firstRunner = RunnerCatalog(100, "1st Runner", 0)
    val secondRunner = RunnerCatalog(200, "2nd Runner", 0)

    class MockCSVData(timestamp: DateTime, selectionId: Long, runnerName: String)
      extends CSVData("", "", marketId, marketStartTime, "", timestamp, false, "Active", selectionId, runnerName, None, None, None)

    class MockCSVRec1 extends MockCSVData(timestamps(1), firstRunner.selectionId, firstRunner.runnerName)
    class MockCSVRec2 extends MockCSVData(timestamps(3), firstRunner.selectionId, firstRunner.runnerName)
    class MockCSVRec3 extends MockCSVData(timestamps(4), firstRunner.selectionId, firstRunner.runnerName)
    class MockCSVRec4 extends MockCSVData(timestamps(2), secondRunner.selectionId, secondRunner.runnerName)
    class MockCSVRec5 extends MockCSVData(timestamps(3), secondRunner.selectionId, secondRunner.runnerName)
    class MockCSVRec6 extends MockCSVData(timestamps(5), secondRunner.selectionId, secondRunner.runnerName)

    val csvData = List(mock[MockCSVRec1], mock[MockCSVRec2], mock[MockCSVRec3], mock[MockCSVRec4], mock[MockCSVRec5], mock[MockCSVRec6])

    val mockRunners = List(
      Runner(firstRunner.selectionId, 0, "TEST_STATUS1", totalMatched = Some(100)),
      Runner(firstRunner.selectionId, 0, "TEST_STATUS2", totalMatched = Some(200)),
      Runner(firstRunner.selectionId, 0, "TEST_STATUS3", totalMatched = Some(300)),
      Runner(secondRunner.selectionId, 0, "TEST_STATUS4", totalMatched = Some(100)),
      Runner(secondRunner.selectionId, 0, "TEST_STATUS5", totalMatched = Some(200)),
      Runner(secondRunner.selectionId, 0, "TEST_STATUS6", totalMatched = Some(300))
    )

    csvData.zipWithIndex.foreach{case (x, i) => (x.toRunner _).expects().returns(mockRunners(i))}

    val marketData = MarketData(marketId, runners = Set(firstRunner, secondRunner), data = csvData)

    val marketBookUpdates = List(
      MarketBookUpdate(timestamps(5), MarketBook(
        marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
        600.00,
        0.0, crossMatching = false, runnersVoidable = false, 1,
        Set(mockRunners(2), mockRunners(5))
      )),
      MarketBookUpdate(timestamps(4), MarketBook(
        marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
        500.00,
        0.0, crossMatching = false, runnersVoidable = false, 1,
        Set(mockRunners(2), mockRunners(4))
      )),
      MarketBookUpdate(timestamps(3), MarketBook(
        marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
        400.00,
        0.0, crossMatching = false, runnersVoidable = false, 1,
        Set(mockRunners(1), mockRunners(4))
      )),
      MarketBookUpdate(timestamps(2), MarketBook(
        marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
        200.00,
        0.0, crossMatching = false, runnersVoidable = false, 1,
        Set(mockRunners(0), mockRunners(3))
      )),
      MarketBookUpdate(timestamps(1), MarketBook(
        marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
        100.00,
        0.0, crossMatching = false, runnersVoidable = false, 1,
        Set(mockRunners(0))
      ))
    )

    val output = marketData.convertToMarketBookUpdates()
    output(0) should be(marketBookUpdates(0))
    output(1) should be(marketBookUpdates(1))
    output(2) should be(marketBookUpdates(2))
    output(3) should be(marketBookUpdates(3))
    output(4) should be(marketBookUpdates(4))

  }
}
