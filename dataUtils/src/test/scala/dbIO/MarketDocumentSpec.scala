package dbIO

import catalogueBuilder.MarketDocumentUtils
import csvReader.CSVData
import domain._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class MarketDocumentSpec extends FlatSpec with Matchers with MockFactory with MarketDocumentUtils {
    val dateFormat = DateTimeFormat.forPattern("yyyy-MM-ddHH:mm:ss.SSS").withZone(DateTimeZone.UTC)

    // TODO break this test down into multiple stages

    "MarketDocument.fromCSVData" should "convert a list of CSVData to a chronological list of MarketBookUpdate" in {
      val marketId = "TEST_ID"
      val marketStartTime = DateTime.parse("2014-09-1418:00:00.000", dateFormat)
      val timestamps = List(
        DateTime.parse("2014-09-1417:40:02.000", dateFormat),
        DateTime.parse("2014-09-1417:40:05.000", dateFormat),
        DateTime.parse("2014-09-1417:45:00.000", dateFormat),
        DateTime.parse("2014-09-1417:50:00.000", dateFormat),
        DateTime.parse("2014-09-1418:00:00.000", dateFormat),
        DateTime.parse("2014-09-1418:05:00.000", dateFormat)
      )
      val firstRunner = RunnerCatalog(100, "1st Runner", 0)
      val secondRunner = RunnerCatalog(200, "2nd Runner", 0)

      class MockCSVData(timestamp: DateTime, selectionId: Long, runnerName: String)
        extends CSVData("TEST_EVENT_ID", "", marketId, marketStartTime, "", timestamp, false, "Active", selectionId, runnerName, None, None, None)

      class MockCSVRec1 extends MockCSVData(timestamps(0), firstRunner.selectionId, firstRunner.runnerName)
      class MockCSVRec1_2 extends MockCSVData(timestamps(1), firstRunner.selectionId, firstRunner.runnerName)
      class MockCSVRec2 extends MockCSVData(timestamps(3), firstRunner.selectionId, firstRunner.runnerName)
      class MockCSVRec3 extends MockCSVData(timestamps(4), firstRunner.selectionId, firstRunner.runnerName)
      class MockCSVRec4 extends MockCSVData(timestamps(2), secondRunner.selectionId, secondRunner.runnerName)
      class MockCSVRec5 extends MockCSVData(timestamps(3), secondRunner.selectionId, secondRunner.runnerName)
      class MockCSVRec6 extends MockCSVData(timestamps(5), secondRunner.selectionId, secondRunner.runnerName)

      val csvData = List(mock[MockCSVRec1], mock[MockCSVRec1_2], mock[MockCSVRec2], mock[MockCSVRec3], mock[MockCSVRec4], mock[MockCSVRec5], mock[MockCSVRec6])

      val mockRunners = List(
        Runner(firstRunner.selectionId, 0, "TEST_STATUS1", totalMatched = Some(100)),
        Runner(firstRunner.selectionId, 0, "TEST_STATUS1_2", totalMatched = Some(150)),
        Runner(firstRunner.selectionId, 0, "TEST_STATUS2", totalMatched = Some(200)),
        Runner(firstRunner.selectionId, 0, "TEST_STATUS3", totalMatched = Some(300)),
        Runner(secondRunner.selectionId, 0, "TEST_STATUS4", totalMatched = Some(100)),
        Runner(secondRunner.selectionId, 0, "TEST_STATUS5", totalMatched = Some(200)),
        Runner(secondRunner.selectionId, 0, "TEST_STATUS6", totalMatched = Some(300))
      )

      csvData.zipWithIndex.foreach{case (x, i) => (x.toRunner _).expects().returns(mockRunners(i))}

      // Used for creating marketCatalogue
      (csvData(0).getMarketName _).expects().returns("TEST_MARKET_NAME")
      // Used for creating event
      (csvData(0).getEventName _).expects().returns("TEST_EVENT_NAME")
      (csvData(0).getCountryCode _).expects().returns("TEST_COUNTRY_CODE")

      val marketCatalogue = MarketCatalogue(
        marketId,
        "TEST_MARKET_NAME",
        Some(marketStartTime),
        None,
        0.0,
        Some(List(firstRunner, secondRunner)),
        None,
        None,
        Event("TEST_EVENT_ID", "TEST_EVENT_NAME", Some("TEST_COUNTRY_CODE"), "", None, marketStartTime)
      )

      val data = List(
        MarketBookUpdate(timestamps(5), MarketBook(
          marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
          600.00,
          0.0, crossMatching = false, runnersVoidable = false, 1,
          Set(mockRunners(3), mockRunners(6))
        )),
        MarketBookUpdate(timestamps(4), MarketBook(
          marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
          500.00,
          0.0, crossMatching = false, runnersVoidable = false, 1,
          Set(mockRunners(3), mockRunners(5))
        )),
        MarketBookUpdate(timestamps(3), MarketBook(
          marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
          400.00,
          0.0, crossMatching = false, runnersVoidable = false, 1,
          Set(mockRunners(2), mockRunners(5))
        )),
        MarketBookUpdate(timestamps(2), MarketBook(
          marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
          250.00,
          0.0, crossMatching = false, runnersVoidable = false, 1,
          Set(mockRunners(1), mockRunners(4))
        )),
        MarketBookUpdate(timestamps(1), MarketBook(
          marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
          150.00,
          0.0, crossMatching = false, runnersVoidable = false, 1,
          Set(mockRunners(1))
        )),
        MarketBookUpdate(timestamps(0), MarketBook(
          marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
          100.00,
          0.0, crossMatching = false, runnersVoidable = false, 1,
          Set(mockRunners(0))
        ))
      )

      val document = fromCSVData(csvData)
      document.marketId should be(marketId)
      document.marketCatalogue should be(Some(marketCatalogue))
      document.data should be(data)
    }
}

