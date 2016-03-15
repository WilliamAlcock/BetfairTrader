package randomForest

trait InstanceUtils extends MathUtils {
  def getDistribution(data: List[Instance]): Map[String, Int] = data.groupBy(_.label).mapValues(_.size)

  def attributesAreConstant(data: List[Instance]): Boolean = data.map(_.features).toSet.size == 1

  def outputIsConstant(data: List[Instance]): Boolean = getDistribution(data).size == 1

  def isFeatureConstant(feature: Int, data: List[Instance]): Boolean = data.forall(_.features(feature) == data(0).features(feature))

  def getNonConstantFeatures(data: List[Instance]): List[Int] = List.range(0, data(0).features.size).filter(feature => !isFeatureConstant(feature, data))

  // Split the data into two halves using the given feature and threshold
  def splitData(feature: Int, threshold: Double, data: List[Instance]): (List[Instance], List[Instance]) = data.partition(x => x.features(feature) <= threshold)

  // Returns the probability for each label in the data
  def getProbabilities(data: List[Instance]): Map[String, Double] = getDistribution(data).mapValues(getWeight(_, data.size))

  // Returns the entropy of the data
  protected def getEntropyOfList(data: List[Instance]): Double = getProbabilities(data).values.foldLeft(0.0)((acc: Double, x: Double) => acc + getEntropy(x))

  // Returns the entropy of splitting the feature by the given threshold
  protected def getEntropyOfSplit(split: (List[Instance], List[Instance]), data: List[Instance]): Double = {
    (getWeight(split._1.size, split._1.size + split._2.size) * getEntropyOfList(split._1)) +
    (getWeight(split._2.size, split._1.size + split._2.size) * getEntropyOfList(split._2))
  }
}
