package randomForest

import scala.util.Random

case class LeafNode(distribution: Map[String, Int]) extends DecisionTree {

  lazy val classes = getClasses

  private def getClasses: List[String] = {
    val maxValue = distribution.values.max
    distribution.filter{case (k,v) => v == maxValue}.map{case (k,v) => k}.toList
  }

//  lazy val classification = distribution.toList.maxBy(x => x._2)._1
  def classify(data: List[Double]): String = {
    classes(Random.nextInt(classes.length))
  }
}