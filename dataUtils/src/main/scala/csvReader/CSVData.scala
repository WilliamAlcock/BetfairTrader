package csvReader

import domain.{PriceSize, ExchangePrices, Runner}
import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

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

  def getWeightOfMoney: Double = ex match {
    case Some(x) =>
      val backSize = x.availableToBack.map(_.size).sum
      val totalSize = backSize + x.availableToLay.map(_.size).sum
      if (totalSize == 0.0) totalSize else backSize / totalSize
    case None => 0.0
  }

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
}