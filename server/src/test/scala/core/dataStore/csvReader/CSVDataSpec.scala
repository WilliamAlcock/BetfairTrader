package core.dataStore.csvReader

import domain.{ExchangePrices, PriceSize, Runner}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

class CSVDataSpec extends FlatSpec with Matchers with MockFactory {

  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-ddHH:mm:ss.SSS").withZone(DateTimeZone.UTC)

  "CSVData.fromCSVString" should "convert a csv string to a CSVData case class" in {
    val testData = Table(
      ("string", "expectedResult", "throws"),
      ( // Null values
        "27267255,Horse Racing : GB : Yarm 16th Sep : To Be Placed 18:00,115481001,2014-09-16,18:00,To Be Placed,18:03:25.642,1,Active,4582747,Arctic Lynx,5682.88,3.7,1.9,1.77,1.66,3.07,99,3.3,null,null,null,null,null,null",
        Some(CSVData(
          "27267255",
          "Horse Racing : GB : Yarm 16th Sep : To Be Placed 18:00",
          "115481001",
          DateTime.parse("2014-09-1618:00:00.000", dateFormat),
          "To Be Placed",
          DateTime.parse("2014-09-1618:03:25.642", dateFormat),
          inplay = true,
          "Active",
          4582747L,
          "Arctic Lynx",
          Some(5682.88),
          Some(3.7),
          Some(ExchangePrices(
            List(
              PriceSize(1.9, 3.07),
              PriceSize(1.77, 99),
              PriceSize(1.66, 3.3)
            ),
            List(),
            List()
          ))
        )),
        false
        ),
      ( // All values correct
        "27267255,Horse Racing : GB : Yarm 16th Sep : To Be Placed 18:00,115481001,2014-09-16,18:00,To Be Placed,18:04:05.912,0,Active,3826852,Secret Millionaire,12318.68,2.1,2.1,2,1.98,83,4,2.22,2.82,2.9,3,38,100,121",
        Some(CSVData(
          "27267255",
          "Horse Racing : GB : Yarm 16th Sep : To Be Placed 18:00",
          "115481001",
          DateTime.parse("2014-09-1618:00:00.000", dateFormat),
          "To Be Placed",
          DateTime.parse("2014-09-1618:04:05.912", dateFormat),
          inplay = false,
          "Active",
          3826852L,
          "Secret Millionaire",
          Some(12318.68),
          Some(2.1),
          Some(ExchangePrices(
            List(
              PriceSize(2.1, 83),
              PriceSize(2, 4),
              PriceSize(1.98, 2.22)
            ),
            List(
              PriceSize(2.82, 38),
              PriceSize(2.9, 100),
              PriceSize(3, 121)
            ),
            List()
          ))
        )),
        false

        ),
      ( // Missing a value
        "27267255,Horse Racing : GB : Yarm 16th Sep : To Be Placed 18:00,115481001,2014-09-16,18:00,To Be Placed,18:04:05.912,1,Active,3826852,Secret Millionaire,12318.68,2.1,2.1,2,1.98,83,4,2.22,2.82,null,null,38,null,null",
        None,
        true
        ),
      ( // Incorrect Date format
        "27267255,Horse Racing : GB : Yarm 16th Sep : To Be Placed 18:00,115481001,2014-20-16,18:00,To Be Placed,18:04:05.912,1,Active,3826852,Secret Millionaire,12318.68,2.1,2.1,2,1.98,83,4,2.22,2.82,null,null,38,null,null",
        None,
        true
        ),
      ( // Incorrect time format
        "27267255,Horse Racing : GB : Yarm 16th Sep : To Be Placed 18:00,115481001,2014-09-16,18:00,To Be Placed,99:04:05.912,1,Active,3826852,Secret Millionaire,12318.68,2.1,2.1,2,1.98,83,4,2.22,2.82,null,null,38,null,null",
        None,
        true
        ),
      ( // Bad String
        "RUBBISH",
        None,
        true
        )
    )

    forAll(testData) { (input: String, expectedResult: Option[CSVData], throws: Boolean) =>
      try {
        CSVData.fromCSVString(input) should be(expectedResult.get)
        if (throws) fail()
      } catch {
        case _: Throwable => if (!throws) fail()
      }
    }
  }

  class MockExchangePrices extends ExchangePrices(List.empty, List.empty, List.empty)

  // TODO this would be a good case for quickCheck test
  "CSVData.toRunner" should "convert a CSVData case class to a Runner case class" in {
    val marketId = "TEST_ID"
    val marketStartTime = DateTime.parse("2014-09-14T18:00:00.000", dateFormat)
    val timestamp = DateTime.parse("2014-09-14T17:10:00.000", dateFormat)
    val selectionId = 100
    val name = "TEST_RUNNER"
    val status = "TEST_STATUS"
    val totalMatched = Some(100.00)
    val lastPriceTraded = Some(1.9)
    val ex1 = Some(mock[MockExchangePrices])

    val csvData = CSVData("", "", marketId, marketStartTime, "", timestamp, false, status, selectionId, name, totalMatched, lastPriceTraded, ex1)
    val runner = Runner(selectionId, 0.0, status, lastPriceTraded = lastPriceTraded, totalMatched = totalMatched, ex = ex1)

    csvData.toRunner() should be(runner)
  }
}

