package randomForest

import play.api.libs.json._

@SerialVersionUID(101L)
case class RandomForest(trees: List[DecisionTree], oobError: Double) extends ClassificationUtils with Serializable {
  def getClassification(data: List[Double]): String = getMaxClass(trees.map(x => x.classify(data)).groupBy(y => y))
}

trait RandomForestBuilder {
  def trainForest(numberOfTrees: Int, leafSize: Int, numberOfFeatures: Int, data: List[Instance]): RandomForest
}

object RandomForest {
  implicit val formatRandomForest = Json.format[RandomForest]
}