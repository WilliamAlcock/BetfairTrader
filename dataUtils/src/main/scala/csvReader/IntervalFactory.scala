package csvReader

import indicators.Indicators
import randomForest.Instance

import scala.annotation.tailrec

trait IntervalFactory {

  /*
    Examples:
    price,        output
    1.1           1 - 0.9091  = 0.0909
    1.5           1 - 0.33    = 0.66
    3             1 - 0.66    = 0.33
    10            1 - 0.10    = 0.90


    normalised price is the 100 - percentage probability
    normalised price will range 0 -> 100
   */
  def probLose(price: Double): Double = if (price == 0) price else 1 - (1/price)

  def getIntervalStartTime(timestamp: Long, raceStartTime: Long, interval: Long) = {
    timestamp - raceStartTime match {
      case x if x > 0 => timestamp - (x % interval)                   // after (inplay)
      case x if x < 0 => timestamp - (x % interval) - interval        // before (pre-race)
      case _ => timestamp
    }
  }

  def getIndicators(csvData: List[CSVData],
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
      csvData.last.getWeightOfMoney,
      prevData
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
      prevData
    )
  }

  @tailrec
  final def indicatorsFromCSVData(csvData: List[CSVData],                   // Assume this is sorted ascending by timestamp
                                  intervalStartTime: Long,
                                  interval: Long,                           // Milliseconds
                                  priceFunction: (Double) => Double,
                                  prevData: List[Indicators] = List.empty): List[Indicators] = csvData.headOption match {
    case Some(x) =>
      csvData.span(_.timestamp.getMillis < (intervalStartTime + interval)) match {
        case (intervalData, rest) => indicatorsFromCSVData(
          rest,
          intervalStartTime + interval,
          interval,
          priceFunction,
          getIndicators(intervalData, x.selectionId, intervalStartTime + interval, x.startTime.getMillis, priceFunction, prevData) :: prevData
        )
      }
    case None => prevData
  }

  def getLabel(nextClose: Double, close: Double) = nextClose - close match {
    case x if x > 0 => "UP"
    case x if x < 0 => "DOWN"
    case _ => "NONE"
  }

  def indicatorsToInstances(indicators: List[Indicators], instances: List[Instance] = List.empty): List[Instance] = indicators.size match {
    case x if x > 1 =>
      val next = indicators.head
      val current = indicators.tail.head
      require(next.timestamp > current.timestamp, "indicators must be ascending order by timestamp")
      indicatorsToInstances(
        indicators.drop(1),
        Instance(Indicators.getFeatures(current).map(_.get), getLabel(next.tick.close, current.tick.close)) :: instances
      )
    case _ => instances
  }
}