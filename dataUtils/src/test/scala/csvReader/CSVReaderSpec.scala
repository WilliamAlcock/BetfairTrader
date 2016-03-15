package csvReader

//import dbIO.DBIO
//import domain.{Event, MarketCatalogue}
//import org.joda.time.DateTime
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
//
//import scala.language.postfixOps
//
//class CSVReaderSpec extends FlatSpec with Matchers with BeforeAndAfterEach with MockFactory {
//
//  var mockDBIO: DBIO = _
//
//  val marketIds = List("TEST_MARKET_1", "TEST_MARKET_2")
//  val startTimes = List(DateTime.now(), DateTime.now().plusMinutes(1))
//  val marketCatalogues = List(
//    MarketCatalogue(marketIds(0), "", marketStartTime = Some(startTimes(0)), totalMatched = 0, event = Event("", "", timezone = "", openDate = DateTime.now())),
//    MarketCatalogue(marketIds(1), "", marketStartTime = Some(startTimes(1)), totalMatched = 0, event = Event("", "", timezone = "", openDate = DateTime.now()))
//  )
//
//  class MockDocument1 extends MarketDocument(marketId = marketIds(0), marketCatalogue = Some(marketCatalogues(0)))
//  class MockDocument2 extends MarketDocument(marketId = marketIds(1), marketCatalogue = Some(marketCatalogues(1)))
//
//  override def beforeEach = {
//    mockDBIO = mock[DBIO]
//  }
//
//  "CSVReader.readFile" should "getLines, convertLinesToCSV, getMarketDocumentsByDate then call writeToDB with each document" in {
//    val mockGetLines = mockFunction[String, List[String]]
//    val mockConvertLinesToCSV = mockFunction[List[String], List[CSVData]]
//    val mockGetMarketDocuments = mockFunction[List[CSVData], Map[String, List[MarketDocument]]]
//    val mockWriteToDB = mockFunction[MarketDocument, Boolean]
//
//    val mockPath = "TEST_PATH"
//    val mockLines = List.empty[String]
//    val mockCSVData = List.empty[CSVData]
//    val mockDocument = MarketDocument(marketId = marketIds(0), marketCatalogue = Some(marketCatalogues(0)))
//    val mockMarketDocuments = Map("2015-01-30" -> List(mockDocument))
//
//    val csvReader = new CSVReader {
//      override def getLines(path: String) = mockGetLines(path)
//      override def convertLinesToCSV(lines: List[String]) = mockConvertLinesToCSV(lines)
//      override def getMarketDocumentsByDate(csvData: List[CSVData]) = mockGetMarketDocuments(csvData)
//      override def writeToDB(m: MarketDocument) = mockWriteToDB(m)
//    }
//
//    mockGetLines.expects(mockPath).returns(mockLines)
//    mockConvertLinesToCSV.expects(mockLines).returns(mockCSVData)
//    mockGetMarketDocuments.expects(mockCSVData).returns(mockMarketDocuments)
//    mockWriteToDB.expects(mockDocument).returns(true)
//
//    csvReader.readFile(mockPath)
//  }
//
//  "CSVReader.convertLinesToCSV" should "convert each line to CSVData" in {
//    val mockLines = List("TEST_LINE_1", "TEST_LINE_2")
//    val mockOutput = List(
//      CSVData("", "", "", DateTime.now(), "", DateTime.now(), inplay = false, "", 0, "", None, None, None),
//      CSVData("", "", "", DateTime.now().plusMinutes(10), "", DateTime.now().plusMinutes(10), inplay = false, "", 0, "", None, None, None)
//    )
//    val mockFromCSVString = mockFunction[String, CSVData]
//
//    val csvReader = new CSVReader {
//      override def fromCSVString(s: String) = mockFromCSVString(s)
//    }
//
//    mockFromCSVString.expects(mockLines(0)).returns(mockOutput(0))
//    mockFromCSVString.expects(mockLines(1)).returns(mockOutput(1))
//
//    csvReader.convertLinesToCSV(mockLines) should be(mockOutput)
//  }
//
////  "CSVReader.getMarketDocuments" should "" in {
////  }
//
////  "CSVReader.writeToDB" should "" in {
////    val marketDocuments = List(mock[MockDocument1], mock[MockDocument2])
////
////    val mockDBIO = mock[DBIO]
////
////    (mockDBIO.writeMarketDocument _).expects(marketDocuments(0), startTimes(0))
////    (mockDBIO.writeMarketDocument _).expects(marketDocuments(1), startTimes(1))
////
////    val csvReader = new CSVReader {
////      override val dbIO = mockDBIO
////    }
////
////    csvReader.underlyingActor.writeToDB(marketDocuments)
////  }
//}
//
