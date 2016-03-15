package csvReader

import domain.{ExchangePrices, PriceSize}
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat

trait CSVDataUtils {
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
}


