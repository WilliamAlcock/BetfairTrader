package randomForest

import play.api.libs.json.Json

import scala.util.Random

case class LeafNode(c: List[String]) extends DecisionTree {

  def classify(data: List[Double]): String = {
    c(Random.nextInt(c.length))
  }
}

object LeafNode {
  implicit val readLeafNode = Json.reads[LeafNode]
}