//package dbIO
//
//import domain.{Event, MarketBook, MarketBookUpdate, MarketCatalogue}
//import org.joda.time.DateTime
//import org.joda.time.format.DateTimeFormat
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
//import reactivemongo.api.commands.WriteResult
//import org.scalatest.prop.TableDrivenPropertyChecks._
//
//import scala.concurrent.Await
//import scala.concurrent.duration._
//import scala.language.postfixOps
//
//class DBIOSpec extends FlatSpec with Matchers with BeforeAndAfterAll with ScalaFutures with BeforeAndAfterEach {
//
//  class TestDBWriter extends DBIO {
//    import scala.concurrent.ExecutionContext.Implicits.global
//    override def getDBName(timestamp: DateTime): String = "test"
//
//    println("cleaning DB")
//    Await.result(dbConnector.getDB("test").drop(), 10 seconds)
//  }
//
//  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd") //.withZone(DateTimeZone.UTC)
//
//  "DBWriter.writeMarketCatalogue" should "write the catalogue to the database" in {
//    val marketId = "TEST_ID"
//    val marketStartTime = DateTime.parse("2014-01-30", dateFormat)
//    val event = Event("TEST_EVENT_ID", "TEST_EVENT_NAME", timezone = "TEST_TIMEZONE", openDate = DateTime.now())
//    val marketCatalogue = MarketCatalogue(marketId, "TEST_NAME", marketStartTime = Some(marketStartTime), runners = None, eventType = None, totalMatched = 100, event = event)
//
//    val dbWriter = new TestDBWriter()
//
//    Await.result(dbWriter.writeMarketCatalogue(marketCatalogue), 10 seconds) match {
//      case x: WriteResult => x.ok should be(true)
//      case _ => fail()
//    }
//
//    Await.result(dbWriter.findMarketCatalogue(marketId, marketStartTime), 10 seconds) match {
//      case Some(x) => x should equal(marketCatalogue)
//      case _ => fail()
//    }
//  }
//
//  "DBWriter.getTickStartTime" should "return the start time of the tick the timestamp is in" in {
//    val testData = Table(
//
//      ("timestamp", "startTime", "interval", "expectedOutput"),
//      (100L,        50L,         20L,        90L),
//      (50L,         100L,        20L,        40L)
//    )
//
//    val dbWriter = new TestDBWriter()
//
//    forAll(testData){ (timestamp: Long, startTime: Long, interval: Long, expectedOutput: Long) =>
//      val output = dbWriter.getTickStartTime(timestamp, startTime, interval)
//      output should be (expectedOutput)
//    }
//  }
//
////  "DBWriter.writeMarketCatalogue" should "update the marketCatalogue if a Market Document already exists" in {
////    val marketId = "TEST_ID"
////    val marketStartTime = DateTime.parse("2014-01-30", dateFormat)
////    val event = Event("TEST_EVENT_ID", "TEST_EVENT_NAME", timezone = "TEST_TIMEZONE", openDate = DateTime.now())
////    val firstMarketCatalogue = MarketCatalogue(marketId, "TEST_NAME", marketStartTime = Some(marketStartTime), runners = None, eventType = None, totalMatched = 100, event = event)
////    val secondMarketCatalogue = MarketCatalogue(marketId, "TEST_NAME", marketStartTime = Some(marketStartTime), runners = None, eventType = None, totalMatched = 200, event = event)
////
////    val dbWriter = new TestDBWriter()
////
////    Await.result(dbWriter.writeMarketCatalogue(firstMarketCatalogue), 10 seconds) match {
////      case x: UpdateWriteResult => x.ok should be(true)
////      case _ => fail()
////    }
////
////    val firstResult = Await.result(dbWriter.findMarketDocument(marketId, marketStartTime), 10 seconds)
////
////    Await.result(dbWriter.writeMarketCatalogue(secondMarketCatalogue), 10 seconds) match {
////      case x: UpdateWriteResult => x.ok should be(true)
////      case _ => fail()
////    }
////
////    val secondResult = Await.result(dbWriter.findMarketDocument(marketId, marketStartTime), 10 seconds)
////
////    firstResult.isDefined should be(true)
////    secondResult.isDefined should be(true)
////
////    firstResult.get.data should be(secondResult.get.data)
////    firstResult.get._id should be(secondResult.get._id)
////    firstResult.get.marketId should be(secondResult.get.marketId)
////    firstResult.get.marketCatalogue.get should be(firstMarketCatalogue)
////    secondResult.get.marketCatalogue.get should be(secondMarketCatalogue)
////  }
//
//  "DBWriter.writeMarketBookUpdate" should "write the update to the database" in {
//    val marketId = "TEST_ID"
//    val marketStartTime = DateTime.parse("2014-01-30", dateFormat)
//    val timestamp = marketStartTime.minusMinutes(19).minusSeconds(30)
//
//    val marketBook = MarketBook(marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
//      600.00, 0.0, crossMatching = false, runnersVoidable = false, 1, Set())
//    val marketBookUpdate = MarketBookUpdate(timestamp, marketBook)
//
//    //    val event = Event("TEST_EVENT_ID", "TEST_EVENT_NAME", timezone = "TEST_TIMEZONE", openDate = DateTime.now())
////    val marketCatalogue = MarketCatalogue(marketId, "TEST_NAME", marketStartTime = Some(marketStartTime), runners = None, eventType = None, totalMatched = 100, event = event)
//
//    val dbWriter = new TestDBWriter()
//
//    Await.result(dbWriter.writeMarketBookUpdate(marketBookUpdate), 10 seconds) match {
//      case x: WriteResult => x.ok should be(true)
//      case _ => fail()
//    }
//
//    Await.result(dbWriter.findMarketBookUpdate(marketId, timestamp), 10 seconds) match {
//      case Some(x) => x should equal (marketBookUpdate)
//      case _ => fail()
//    }
//  }
//
////  "DBWriter.writeMarketBookUpdate" should "add the marketBookUpdate to the list for the correct minute if a Market Document already exists" in {
////    val marketId = "TEST_ID"
////    val marketStartTime = DateTime.parse("2014-01-30", dateFormat)
////    val timestamps = Map(
////      1 -> marketStartTime.minusMinutes(19).minusSeconds(30),
////      2 -> marketStartTime.minusMinutes(19).minusSeconds(31)
////    )
////
////    val marketBooks = Map(
////      1 -> MarketBook(marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
////        600.00, 0.0, crossMatching = false, runnersVoidable = false, 1, Set()),
////      2 -> MarketBook(marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
////        1200.00, 0.0, crossMatching = false, runnersVoidable = false, 1, Set())
////    )
////
////    val marketBookUpdates = Map(
////      1 -> MarketBookUpdate(timestamps(1), marketBooks(1)),
////      2 -> MarketBookUpdate(timestamps(2), marketBooks(2))
////    )
////
////    val dbWriter = new TestDBWriter()
////
////    Await.result(dbWriter.writeMarketBookUpdate(marketBookUpdates(1), marketStartTime), 10 seconds) match {
////      case x: UpdateWriteResult => x.ok should be(true)
////      case _ => fail()
////    }
////
////    Await.result(dbWriter.writeMarketBookUpdate(marketBookUpdates(2), marketStartTime), 10 seconds) match {
////      case x: UpdateWriteResult => x.ok should be(true)
////      case _ => fail()
////    }
////
////    Await.result(dbWriter.findMarketDocument(marketId, marketStartTime), 10 seconds) match {
////      case Some(x) =>
////        x.marketId should be(marketId)
////        x.marketCatalogue.isDefined should be(false)
////        x.data should equal (Map("19" -> List(marketBookUpdates(1), marketBookUpdates(2))))
////      case _ => fail()
////    }
////  }
//
////  "DBWriter.writeMarketBookUpdate" should "add marketBookUpdate should store by minutes before/after marketStartTime" in {
////    val marketId = "TEST_ID"
////    val marketStartTime = DateTime.parse("2014-01-30", dateFormat)
////    val timestamps = List(
////      marketStartTime.minusMinutes(19).minusSeconds(21),                      // before marketStartTime
////      marketStartTime.minusMinutes(19).minusSeconds(8),                       // before marketStartTime
////      marketStartTime.minusMinutes(15).minusSeconds(5),                       // before marketStartTime
////      marketStartTime.minusMinutes(15).minusSeconds(7),                       // before marketStartTime
////      marketStartTime.minusMinutes(15).minusSeconds(45),                      // before marketStartTime
////      marketStartTime.minusMinutes(9).minusSeconds(24),                       // before marketStartTime
////      marketStartTime.minusMinutes(2).minusSeconds(32),                       // before marketStartTime
////      marketStartTime.minusMinutes(2).minusSeconds(19),                       // before marketStartTime
////      marketStartTime.minusSeconds(20),                                       // before marketStartTime
////      marketStartTime,
////      marketStartTime.plusSeconds(21),                                        // after marketStartTime
////      marketStartTime.plusMinutes(2).plusSeconds(8),                          // after marketStartTime
////      marketStartTime.plusMinutes(2).plusSeconds(51),                         // after marketStartTime
////      marketStartTime.plusMinutes(3).plusSeconds(7),                          // after marketStartTime
////      marketStartTime.plusMinutes(3).plusSeconds(45)
////    )
////
////    val marketBook = MarketBook(marketId, isMarketDataDelayed = false, "Active", 0, bspReconciled = false, complete = false, inplay = false, 0, 2, 2, None,
////        600.00, 0.0, crossMatching = false, runnersVoidable = false, 1, Set())
////
////    val marketBookUpdates = timestamps.map(x => MarketBookUpdate(x, marketBook))
////
////    val dbWriter = new TestDBWriter()
////
////    marketBookUpdates.foreach(x => Await.result(dbWriter.writeMarketBookUpdate(x, marketStartTime), 10 seconds) match {
////      case x: UpdateWriteResult => x.ok should be(true)
////      case _ => fail()
////    })
////
////    Await.result(dbWriter.findMarketDocument(marketId, marketStartTime), 10 seconds) match {
////      case Some(x) =>
////        x.marketId should be(marketId)
////        x.data should equal (Map(
////          "19" -> List(marketBookUpdates(0), marketBookUpdates(1)),
////          "15" -> List(marketBookUpdates(2), marketBookUpdates(3), marketBookUpdates(4)),
////          "9" -> List(marketBookUpdates(5)),
////          "2" -> List(marketBookUpdates(6), marketBookUpdates(7)),
////          "0" -> List(marketBookUpdates(8), marketBookUpdates(9)),
////          "0a" -> List(marketBookUpdates(10)),
////          "2a" -> List(marketBookUpdates(11), marketBookUpdates(12)),
////          "3a" -> List(marketBookUpdates(13), marketBookUpdates(14))
////        ))
////      case _ => fail()
////    }
////  }
//
////  "DBWriter.writeMarketDocument" should "write marketDocument for the given day if it doesn't already exist" in {
////    val marketId = "TEST_ID"
////    val marketStartTime = DateTime.parse("2014-01-30", dateFormat)
////    val marketDocument = MarketDocument(marketId  = marketId)
////
////    val dbWriter = new TestDBWriter()
////
////    Await.result(dbWriter.writeMarketDocument(marketDocument, marketStartTime), 10 seconds) match {
////      case Some(x: WriteResult) => x.ok should be(true)
////      case x => fail("Failed to write marketDocument to DB")
////    }
////
////    Await.result(dbWriter.findMarketDocument(marketId, marketStartTime), 10 seconds) match {
////      case Some(x) => x.copy(_id = None) should equal (marketDocument)
////      case x => fail("Failed to find marketDocument in DB")
////    }
////  }
//
////  "DBWriter.writeMarketDocument" should "NOT write marketDocument for the given day if it does exist" in {
////    val marketId = "TEST_ID"
////    val marketStartTime = DateTime.parse("2014-01-30", dateFormat)
////    val marketDocument = MarketDocument(marketId  = marketId)
////
////    val dbWriter = new TestDBWriter()
////
////    Await.result(dbWriter.writeMarketDocument(marketDocument, marketStartTime), 10 seconds) match {
////      case Some(x: WriteResult) => x.ok should be(true)
////      case _ => fail("Failed to write marketDocument to DB")
////    }
////
////    Await.result(dbWriter.writeMarketDocument(marketDocument, marketStartTime), 10 seconds) match {
////      case None => println("write failed !!!")
////      case _ => fail()
////    }
////
////    Await.result(dbWriter.findMarketDocument(marketId, marketStartTime), 10 seconds) match {
////      case Some(x) => x.copy(_id = None) should equal (marketDocument)
////      case _ => fail("Failed to find marketDocument in DB")
////    }
////  }
//
////  "DBWriter.findMarketDocument" should "retrieve a market from the database for a given day" in {
////    val marketStartTimes = List(DateTime.parse("2014-01-30", dateFormat), DateTime.parse("2015-01-30", dateFormat))
////    val marketDocuments = List(MarketDocument(marketId = "TEST_ID1"), MarketDocument(marketId = "TEST_ID2"))
////
////    val dbWriter = new TestDBWriter()
////
////    Await.result(dbWriter.writeMarketDocument(marketDocuments(0), marketStartTimes(0)), 10 seconds) match {
////      case Some(x: WriteResult) => x.ok should be(true)
////      case _ => fail()
////    }
////
////    Await.result(dbWriter.writeMarketDocument(marketDocuments(1), marketStartTimes(1)), 10 seconds) match {
////      case Some(x: WriteResult) => x.ok should be(true)
////      case _ => fail()
////    }
////
////    val searchTests = Table(
////      ("marketId",                                "startTime",            "shouldFind"),
////      (marketDocuments(0),                        marketStartTimes(0),    true),
////      (marketDocuments(1),                        marketStartTimes(1),    true),
////      (marketDocuments(0),                        marketStartTimes(1),    false),           // searching for the marketId on the wrong date
////      (marketDocuments(1),                        marketStartTimes(0),    false),           // searching for the marketId on the wrong date
////      (MarketDocument(marketId = "INVALID_ID"),   marketStartTimes(1),    false)            // id doesn't exist
////    )
////
////    forAll(searchTests) {(marketDocument: MarketDocument, startTime: DateTime, shouldFind: Boolean) =>
////      Await.result(dbWriter.findMarketDocument(marketDocument.marketId, startTime), 10 seconds) match {
////        case Some(x) if shouldFind => x.copy(_id = None) should be (marketDocument)
////        case _ => if (shouldFind) fail()
////      }
////    }
////  }
//}
