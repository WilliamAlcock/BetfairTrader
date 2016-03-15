package randomForest

trait DecisionTree {
  def classify(data: List[Double]): String
}

trait DecisionTreeBuilder {
  def getDecisionTree(leafSize: Int, numberOfFeatures: Int, data: List[Instance]): DecisionTree
}

//
//import scala.concurrent.duration.Duration
//import scala.concurrent.{Await, Future}
//import scala.concurrent.ExecutionContext.Implicits.global
//
//trait DecisionTree {
//  def getLabel(data: List[Double]): String
//}
//
//case class LeafNode(labels: Map[String, Int]) extends DecisionTree {
//  lazy val label = labels.maxBy(_._2)._1
//  def getLabel(data: List[Double]): String = label
//}
//
//case class InnerNode(feature: Int, threshold: Double, leftNode: DecisionTree, rightNode: DecisionTree) extends DecisionTree {
//  def getLabel(data: List[Double]): String = data(feature) match {
//    case x if x <= threshold => leftNode.getLabel(data)
//    case x => rightNode.getLabel(data)
//  }
//}
//
//trait DecisionTreeUtils {
//
//  def getRandomFeatures(numberOfFeatures: Int, maxIndex: Int): List[Int] = List.range(0, numberOfFeatures).map(x => scala.util.Random.nextInt(maxIndex))
//
//  def log2(x: Double): Double = scala.math.log(x) / scala.math.log(2)
//
//  /*
//    Returns a weight given two integers
//   */
//  def getWeight(n: Int, d: Int): Double = n.toDouble / d
//
//  /*
//    Returns entropy given a probability
//   */
//  def getEntropy(p: Double): Double = -p * log2(p)
//
//  /*
//    Returns the probability for each label in the data
//   */
//  def getProbabilities(data: List[Instance]): Map[String, Double] = getLabels(data).mapValues(getWeight(_, data.size))
//
//  /*
//    Returns the entropy of the data
//   */
//  def getEntropyOfData(data: List[Instance]): Double = getProbabilities(data).values.foldLeft(0.0)((acc: Double, x: Double) => acc + getEntropy(x))
//
//  /*
//    Returns the entropy of splitting the feature by the given threshold
//   */
//  def getEntropyOfThreshold(feature: Int, threshold: Double, data: List[Instance]): Double = {
//    val (left, right) = splitData(feature, threshold, data)
//    (getWeight(left.size, data.size) * getEntropyOfData(left)) + (getWeight(right.size, data.size) * getEntropyOfData(right))
//  }
//
//  /*
//    Return all the possible thresholds (cut-points) for the given feature
//   */
//  def getPossibleThresholds(feature: Int, data: List[Instance]): Set[Double] = {
//    data.dropRight(1).map(x => {
//      if (feature > (x.features.size - 1)) println(feature, x.features)
//      x.features(feature)
//    }).toSet
//  }
//
//  /*
//    Returns the threshold for the feature that splits the data resulting with the LOWEST entropy
//   */
//  def getThresholdAndEntropyForFeature(feature: Int, data: List[Instance]): (Double, Double) = getPossibleThresholds(feature, data) match {
//    case thresholds if thresholds.size > 1 =>
//      thresholds.tail.foldLeft(thresholds.head, getEntropyOfThreshold(feature, thresholds.head, data))(
//        (acc: (Double, Double), threshold: Double) => getEntropyOfThreshold(feature, threshold, data) match {
//          case entropy if entropy < acc._2 => (threshold, entropy)
//          case _ => acc
//      })
//    case x if x.size > 0 => (x.head, getEntropyOfThreshold(feature, x.head, data))
//    case _ => throw new IllegalArgumentException("No possible thresholds in the data")
//  }
//
//  /*
//    Split the data into two halves using the given feature and threshold
//   */
//  def splitData(feature: Int, threshold: Double, data: List[Instance]): (List[Instance], List[Instance]) = data.partition(x => x.features(feature) <= threshold)
//
//  /*
//    Return the number of instances for each label in the data
//   */
//  def getLabels(data: List[Instance]): Map[String, Int] = {
//    data.groupBy(_.label).mapValues(_.size)
//  }
//
//  /*
//    Return an inner node that splits the data on a feature & threshold
//   */
//  def getInnerNode(leafSize: Int, numberOfFeatures: Int, data: List[Instance], level: List[Int]): InnerNode = {
//    println("GETTING INNER NODE " + level)
//    // get the random features
//    // For each feature: Calculate the threshold with the min entropy
//    // Select the feature that produces the min entropy
//    val totalFeatures = data.head.features.size
//    val randomFeatures = getRandomFeatures(numberOfFeatures, totalFeatures)
//    println("Got randomFeatures", randomFeatures)
//
//    val thresholds = Await.result(Future.sequence(randomFeatures.map(x => Future{getThresholdAndEntropyForFeature(x, data)})), Duration.Inf)
//    val feature = (randomFeatures zip thresholds).minBy(_._2._2)
//
//    // Split the data on that feature + threshold
//    val (left, right) = splitData(feature._1, feature._2._1, data)
//    println("split data")
//    InnerNode(
//      feature._1,
//      feature._2._1,
//      getDecisionTree(leafSize, numberOfFeatures, left, level.head + 1 ::level),
//      getDecisionTree(leafSize, numberOfFeatures, right, level.head + 1 :: level)
//    )
//  }
//
//  def getDecisionTree(leafSize: Int, numberOfFeatures: Int, data: List[Instance], level: List[Int] = List(1)): DecisionTree = {
//    println("GETTING DECISION TREE " + level)
//    data match {
//      case x if x.size <= leafSize =>
//        println("leaf", x.size, getLabels(x))
//        LeafNode(getLabels(x))
//      case x =>
//        println(x.size, getLabels(x))
//        getInnerNode(leafSize, numberOfFeatures, x, level)
//    }
//  }
//}