package randomForest

import play.api.libs.json.Json

case class InnerNode(f: Int, t: Double, l: DecisionTree, r: DecisionTree) extends DecisionTree {
  def classify(data: List[Double]): String = data(f) match {
    case x if x <= t => l.classify(data)
    case x => r.classify(data)
  }
}

object InnerNode {
  implicit val readInnerNode = Json.reads[InnerNode]
}