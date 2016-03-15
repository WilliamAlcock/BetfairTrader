package randomForest

case class LeafNode(distribution: Map[String, Int]) extends DecisionTree {
  lazy val classification = distribution.toList.maxBy(x => x._2)._1
  def classify(data: List[Double]): String = classification
}