package randomForest

import scala.util.Random

trait ClassificationUtils {
  def getMaxClass(results: Map[String, List[String]]): String = results match {
    case answers if results.isEmpty => ""
    case answers =>
      val maxValue = answers.map{case (k,v) => v.size}.max
      val output = answers.filter{case (k,v) => v.size == maxValue}.keys.toArray
      output(Random.nextInt(output.length))
  }
}