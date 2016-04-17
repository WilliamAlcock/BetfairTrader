package dbIO

import domain._
import indicators.Indicators
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.Json
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

import scala.annotation.tailrec

case class CSVData(eventId: String,
                   marketDescription: String,
                   marketId: String,
                   startTime: DateTime,
                   raceCourse: String,
                   timestamp: DateTime,
                   inplay: Boolean,
                   status: String,
                   selectionId: Long,
                   runnerName: String,
                   totalMatched: Option[Double],
                   lastPriceMatched: Option[Double],
                   ex: Option[ExchangePrices]) {

  private val splitMarketDescription = marketDescription.split(":")

  def getEventName: String = if (splitMarketDescription.size > 2) splitMarketDescription(2) else ""
  def getEventType: String = if (splitMarketDescription.size > 0) splitMarketDescription(0) else ""
  def getCountryCode: String = if (splitMarketDescription.size > 1) splitMarketDescription(1) else ""
  def getMarketName: String = if (splitMarketDescription.size > 3) getEventName + " " + splitMarketDescription(3) else ""


  def toRunner: Runner = Runner(selectionId, 0.0, status, lastPriceTraded = lastPriceMatched, totalMatched = totalMatched, ex = ex)
}

object CSVData {
  implicit val formatCSVData = Json.format[CSVData]

  implicit object PriceSizeReader extends BSONDocumentReader[PriceSize] {
    def read(doc: BSONDocument): PriceSize = {
      val price = doc.getAs[Double]("price").get
      val size = doc.getAs[Double]("size").get

      PriceSize(price, size)
    }
  }

  implicit object ExchangePricesReader extends BSONDocumentReader[ExchangePrices] {
    def read(doc: BSONDocument): ExchangePrices = {
      val availableToBack = doc.getAs[List[PriceSize]]("availableToBack").get
      val availableToLay = doc.getAs[List[PriceSize]]("availableToLay").get
      val tradedVolume = doc.getAs[List[PriceSize]]("tradedVolume").get

      ExchangePrices(availableToBack, availableToLay, tradedVolume)
    }
  }

  implicit object CSVReader extends BSONDocumentReader[CSVData] {
    def read(doc: BSONDocument): CSVData = {
      val eventId = doc.getAs[String]("eventId").get
      val marketDescription = doc.getAs[String]("marketDescription").get
      val marketId = doc.getAs[String]("marketId").get
      val startTime = new DateTime(doc.getAs[Long]("startTime").get)
      val raceCourse = doc.getAs[String]("raceCourse").get
      val timestamp = new DateTime(doc.getAs[Long]("timestamp").get)
      val inplay = doc.getAs[Boolean]("inplay").get
      val status = doc.getAs[String]("status").get
      val selectionId = doc.getAs[Int]("selectionId").get.toLong
      val runnerName = doc.getAs[String]("runnerName").get
      val totalMatched = Some(doc.getAs[Double]("totalMatched").get)
      val lastPriceMatched = Some(doc.getAs[Double]("lastPriceMatched").get)
      val ex = Some(doc.getAs[ExchangePrices]("ex").get)

      CSVData(eventId, marketDescription, marketId, startTime, raceCourse, timestamp, inplay, status, selectionId, runnerName, totalMatched, lastPriceMatched, ex)
    }
  }

  val startTimeFormat = DateTimeFormat.forPattern("yyyy-MM-ddHH:mm").withZone(DateTimeZone.UTC)
  val timeStampFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(DateTimeZone.UTC)

  private def stringToOptionDouble(input: String): Option[Double] = input match {
    case x if x == "null" => None
    case x => Some(x.toDouble)
  }

  private def getPriceSize(price: Option[Double], volume: Option[Double]): Option[PriceSize] = (price, volume) match {
    case (Some(x), Some(y)) => Some(PriceSize(x, y))
    case _ => None
  }

  def getWeightOfMoney(ex: Option[ExchangePrices]): Double = ex match {
    case Some(x) =>
      val backSize = x.availableToBack.map(_.size).sum
      val totalSize = backSize + x.availableToLay.map(_.size).sum
      if (totalSize == 0.0) totalSize else backSize / totalSize
    case None => 0.0
  }

  def fromCSVString(s: String): CSVData = {
    val cols = s.split(",").map(_.trim)

    val ex = ExchangePrices(
      List(
        getPriceSize(stringToOptionDouble(cols(13)), stringToOptionDouble(cols(16))),
        getPriceSize(stringToOptionDouble(cols(14)), stringToOptionDouble(cols(17))),
        getPriceSize(stringToOptionDouble(cols(15)), stringToOptionDouble(cols(18)))
      ).filter(_.isDefined).map(_.get),
      List(
        getPriceSize(stringToOptionDouble(cols(19)), stringToOptionDouble(cols(22))),
        getPriceSize(stringToOptionDouble(cols(20)), stringToOptionDouble(cols(23))),
        getPriceSize(stringToOptionDouble(cols(21)), stringToOptionDouble(cols(24)))
      ).filter(_.isDefined).map(_.get),
      List.empty
    )

    CSVData(
      cols(0),                                                    // eventId
      cols(1),                                                    // marketDescription
      cols(2),                                                    // marketId
      DateTime.parse(cols(3) + cols(4), startTimeFormat),         // startTime
      cols(5),                                                    // raceCourse
      DateTime.parse(cols(6), timeStampFormat),                   // timestamp
      cols(7).toInt == 1,                                         // inplay
      cols(8),                                                    // status
      cols(9).toLong,                                             // selectionId
      cols(10),                                                   // runnerName
      stringToOptionDouble(cols(11)),                             // totalMatched
      stringToOptionDouble(cols(12)),                             // lastPriceMatched
      Some(ex))                                                   // price data
  }

  def toIndicator(csvData: List[CSVData],
                  selectionId: Long,
                  intervalStartTime: Long,
                  raceStartTime: Long,
                  priceFunction: (Double) => Double,
                  prevData: List[Indicators]): Indicators = csvData.headOption match {
    case Some(x) => Indicators.getNext(
      selectionId,
      intervalStartTime,
      raceStartTime,
      indicators.Range(
        priceFunction(csvData.maxBy(_.lastPriceMatched.getOrElse(0.0)).lastPriceMatched.getOrElse(0.0)),
        priceFunction(csvData.minBy(_.lastPriceMatched.getOrElse(0.0)).lastPriceMatched.getOrElse(0.0))
      ),
      priceFunction(csvData.last.lastPriceMatched.getOrElse(0.0)),
      csvData.last.totalMatched.getOrElse(0.0) - (if (prevData.headOption.isDefined) prevData.head.tick.volume else 0.0),
      CSVData.getWeightOfMoney(csvData.last.ex),
      prevData,
      csvData.last.ex
    )
    case None => Indicators.getNext(
      selectionId,
      intervalStartTime,
      raceStartTime,
      indicators.Range(
        priceFunction(prevData.head.tick.close),
        priceFunction(prevData.head.tick.close)
      ),
      prevData.head.tick.close,
      0.0,
      prevData.head.tick.weightOfMoney,
      prevData,
      None
    )
  }

  @tailrec
  final def toIndicators(csvData: List[CSVData],                   // Assume this is sorted ascending by timestamp
                         intervalStartTime: Long,
                         interval: Long,                           // Milliseconds
                         priceFunction: (Double) => Double,
                         prevData: List[Indicators] = List.empty): List[Indicators] = csvData.headOption match {
    case Some(x) =>
      csvData.span(_.timestamp.getMillis < (intervalStartTime + interval)) match {
        case (intervalData, rest) => toIndicators(
          rest,
          intervalStartTime + interval,
          interval,
          priceFunction,
          toIndicator(intervalData, x.selectionId, intervalStartTime + interval, x.startTime.getMillis, priceFunction, prevData) :: prevData
        )
      }
    case None => prevData
  }

  def getRunners(csvData: List[CSVData]): List[RunnerCatalog] = {
    csvData.groupBy(x => (x.selectionId, x.runnerName)).mapValues(x => x.maxBy(_.totalMatched).totalMatched).map(x =>
      RunnerCatalog(
        selectionId = x._1._1,
        runnerName = x._1._2,
        handicap = 0.0,
        totalMatched = x._2
      )
    ).toList
  }

  def getMarketCatalogue(csvData: List[CSVData]): MarketCatalogue = {
    val runners = getRunners(csvData)
    MarketCatalogue(
      marketId = csvData.head.marketId,
      marketName = csvData.head.getMarketName,
      marketStartTime = Some(csvData.head.startTime),
      description = None,
      totalMatched = runners.map(_.totalMatched.get).sum,
      runners = Some(runners),
      eventType = Some(EventType("7", csvData.head.getEventType)), // TODO get eventType Id from the name
      competition = None,
      event = Event(csvData.head.eventId, csvData.head.getEventName, Some(csvData.head.getCountryCode), "", None, csvData.head.startTime)
    )
  }
}