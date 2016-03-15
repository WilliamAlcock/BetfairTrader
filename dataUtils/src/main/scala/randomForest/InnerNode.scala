package randomForest

case class InnerNode(feature: Int, threshold: Double, leftNode: DecisionTree, rightNode: DecisionTree) extends DecisionTree {
  def classify(data: List[Double]): String = data(feature) match {
    case x if x <= threshold => leftNode.classify(data)
    case x => rightNode.classify(data)
  }
}
