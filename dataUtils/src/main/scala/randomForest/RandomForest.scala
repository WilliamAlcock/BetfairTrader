package randomForest

case class RandomForest(trees: List[DecisionTree]) {
  def getClassification(data: List[Double]): String = trees.map(x => x.classify(data)).groupBy(y => y) match {
    case answers if answers.isEmpty => ""
    case answers => answers.maxBy{case(k,v) => v.size}._1
  }
}

trait RandomForestBuilder {
  def trainForest(numberOfTrees: Int, leafSize: Int, numberOfFeatures: Int, data: List[Instance]): RandomForest
}



//
//import scala.concurrent.duration.Duration
//import scala.concurrent.{Await, Future}
//import scala.concurrent.ExecutionContext.Implicits.global
//
//case class RandomForest(trees: List[DecisionTree], oobErrorRate: Double) {
//  // Take the highest label by votes after classification by every tree
//  def getLabel(data: List[Double]): String = {
//    trees.map(_.getLabel(data)).groupBy(x => x).mapValues(_.size).maxBy(_._2)._1
//  }
//}
//
//trait RandomForestUtils {
//
//  def getDecisionTree(leafSize: Int, numberOfFeatures: Int, data: List[Instance], level: List[Int] = List.empty): DecisionTree
//
//  private def getRandomIndex(data: List[Instance]): Int = scala.util.Random.nextInt(data.size)
//
//  private def sampleWithReplacement(data: List[Instance]): (List[Instance], Set[Int]) = {
//    var indexes = Set.empty[Int]
//
//    val sample = data.map(x => {
//      val index = getRandomIndex(data)
//      indexes += index
//      data(index)
//    })
//
//    (sample, indexes)
//  }
//
//  /*
//    returns a decision tree, returns a future so multiple trees can be built in parallel
//   */
//  private def buildTree(treeIndex: Int, leafSize: Int, numberOfFeatures: Int, data: List[Instance]): Future[(DecisionTree, Set[Int])] = Future {
//    println("Building tree # " + treeIndex)
//    val (sample, indexes) = sampleWithReplacement(data)
//    println("Got sample " + treeIndex)
//    val tree = getDecisionTree(leafSize, numberOfFeatures, sample)
//    println("Finished tree # " + treeIndex)
//    (tree, indexes)
//  }
//
//  /*
//    Only use the trees who were NOT trained on the given index to predict the label
//   */
//  private def getOOBLabel(trees: List[(DecisionTree, Set[Int])], f: List[Double], index: Int): String = {
//    var labels = Map.empty[String, Int]
//    trees.foreach(x => {
//      if (!x._2.contains(index)) {
//
//        val prediction = x._1.getLabel(f)
//        labels = labels + (prediction -> (labels.getOrElse(prediction, 0) + 1))
//      }
//    })
//    if (labels.isEmpty) "" else labels.maxBy(x => x._2)._1
//  }
//
//  /*
//    The out-of-bag error rate is computed by passing each instance down the trees that WERE NOT trained using that instance
//    and picking the label with the MOST votes.
//
//    This means each instance will NOT be classified by all the trees BUT the forest will classify every instance.
//    In this respect it is comparable to Cross Validation
//   */
//  private def getOOBErrorRate(trees: List[(DecisionTree, Set[Int])], data: List[Instance]): Double = {
//    var errors = 0
//    var totalClassified = 0
//    for((instance, index) <- data.view.zipWithIndex) {
//      val classification = getOOBLabel(trees, instance.features, index)
//
//      if (classification != "") {
//        totalClassified += 1
//        if (classification != instance.label) errors += 1
//      }
//    }
//    println("errors", errors, "total", totalClassified)
//    errors.toDouble / totalClassified.toDouble
//  }
//
//  def trainForest(numberOfTrees: Int, leafSize: Int, data: List[Instance]): RandomForest = {
//    // Build trees in parallel using futures
//    val numberOfFeatures = Math.sqrt(data.head.features.size.toDouble).toInt
//    val trees: List[(DecisionTree, Set[Int])] = Await.result(Future.sequence(List.range(0, numberOfTrees).map(x => {
//      buildTree(x, leafSize, numberOfFeatures, data)
//    })), Duration.Inf)
//
//    RandomForest(trees.map(_._1), getOOBErrorRate(trees, data))
//  }
//}
//
//object RandomForestUtils extends RandomForestUtils with DecisionTreeUtils