package randomForest

import indicators.Indicators
import play.api.libs.json.Json

case class Instance(features: List[Double], label: String)

object Instance {
  def getLabel(nextClose: Double, close: Double) = nextClose - close match {
    case x if x > 0 => "UP"
    case x if x < 0 => "DOWN"
    case _ => "NONE"
  }

  def fromIndicators(indicators: List[Indicators], instances: List[Instance] = List.empty): List[Instance] = indicators.size match {
    case x if x > 1 =>
      val next = indicators.head
      val current = indicators.tail.head
      require(next.timestamp > current.timestamp, "indicators must be ascending order by timestamp")
      fromIndicators(
        indicators.drop(1),
        Instance(Indicators.getFeatures(current).map(_.get), getLabel(next.tick.close, current.tick.close)) :: instances
      )
    case _ => instances
  }

  implicit val formatInstance = Json.format[Instance]
}