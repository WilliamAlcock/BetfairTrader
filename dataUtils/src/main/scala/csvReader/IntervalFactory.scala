package csvReader

import indicators.Indicators
import randomForest.Instance

import scala.annotation.tailrec

trait IntervalFactory {

  /*
    Examples:
    price,        output
    1.1           100 - 90.91 = 9.09
    1.5           100 - 33 = 66
    3             100 - 66 = 33
    10            100 - 10 = 90


    normalised price is the 100 - percentage probability
    normalised price will range 0 -> 100
   */
  def getInvertedOdds(price: Double): Double = if (price == 0) price else 100 - ((1 / price) * 100)


  /*
  Converts a price to the number of price increments (starting @ 1.01)
    Range         Tick Increment      Number of ticks / 1
    1.01 → 2	    0.01                99
    2 → 3	        0.02                50
    3 → 4	        0.05                20
    4 → 6	        0.1                 10
    6 → 10	      0.2                 5
    10 → 20	      0.5                 2
    20 → 30	      1                   1
    30 → 50	      2                   0.5
    50 → 100	    5                   0.2
    100 → 1000	  10                  0.1
  */

  def getTicks(price: Double): Double = price match {
    case x if x > 100 => getTicks(100) + ((x - 100) / 10)
    case x if x > 50 => getTicks(50) + ((x - 50) / 5)
    case x if x > 30 => getTicks(30) + ((x - 30) / 2)
    case x if x > 20 => getTicks(20) + (x - 20)
    case x if x > 10 => getTicks(10) + ((x - 10) / 0.5)
    case x if x > 6 => getTicks(6) + ((x - 6) / 0.2)
    case x if x > 4 => getTicks(4) + ((x - 4) / 0.1)
    case x if x > 3 => getTicks(3) + ((x - 3) / 0.05)
    case x if x > 2 => getTicks(2) + ((x - 2) / 0.02)
    case x if x > 1 => (x - 1.01) / 0.01
    case _ => 0.0
  }

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
        priceFunction(0),
        priceFunction(0)
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