package randomForest

class ExtremelyRandomDecisionTree extends DecisionTreeBuilder with InstanceUtils {

  def stopSplit(leafSize: Int, data: List[Instance]): Boolean =
    data.size < leafSize || attributesAreConstant(data) || outputIsConstant(data)

  def getRandomThreshold(feature: Int, data: List[Instance]): Double = {
    val d = data.map(_.features(feature)).sorted
    (d.head, d.last) match {
      case (min, max) => min + (scala.util.Random.nextDouble() * (max - min))
    }
  }

  def getRandomNonConstantFeatures(numberOfFeatures: Int, data: List[Instance]): List[Int] = getNonConstantFeatures(data) match {
    case features if features.size <= numberOfFeatures => features
    case features =>
      var output = Set.empty[Int]
      while (output.size < numberOfFeatures) output = output + features(scala.util.Random.nextInt(features.size))
      output.toList
  }

  private def getClasses(distribution: Map[String, Int]): List[String] = {
    val maxValue = distribution.values.max
    distribution.filter(_._2 == maxValue).keys.toList
  }

  def getDecisionTree(leafSize: Int, numberOfFeatures: Int, data: List[Instance]): DecisionTree = {
    if (stopSplit(leafSize, data)) {
      LeafNode(getClasses(getDistribution(data)))
    } else {
      val bestSplit = getRandomNonConstantFeatures(numberOfFeatures, data)
        .map(x => (x, getRandomThreshold(x, data)))
        .map(x => (x._1, x._2, splitData(x._1, x._2, data)))
        .minBy(x => getEntropyOfSplit(x._3, data))

      if (bestSplit._3._1.isEmpty || bestSplit._3._2.isEmpty) {
        println("SPLIT IS BAD: ")
        println("data", data)
        println("thresholds", bestSplit._1, bestSplit._2)
      }

      InnerNode(
        bestSplit._1,
        bestSplit._2,
        getDecisionTree(leafSize, numberOfFeatures, bestSplit._3._1),
        getDecisionTree(leafSize, numberOfFeatures, bestSplit._3._2)
      )
    }
  }
}

