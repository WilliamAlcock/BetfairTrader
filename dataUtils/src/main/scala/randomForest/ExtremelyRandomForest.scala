package randomForest

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

class ExtremelyRandomForest extends RandomForestBuilder {

  val treeBuilder: DecisionTreeBuilder = new ExtremelyRandomDecisionTree()

  @tailrec
  private def getSampleWithReplacement(data: Array[Instance], size: Int, sample: List[Instance] = List.empty): List[Instance] = sample.size match {
    case x if x == size => sample
    case _ => getSampleWithReplacement(data, size, data(Random.nextInt(data.length)) :: sample)
  }

  def sampleDataSet(data: List[Instance]): List[Instance] = {
    val byLabel = data.groupBy(x => x.label).mapValues(_.toArray)
    val size = data.size / byLabel.size
    byLabel.map{case (k,v) => getSampleWithReplacement(v, size)}.flatten.toList
  }

  private def getMaxClass(results: List[Classification]): String = results match {
    case answers if results.isEmpty => ""
    case answers =>
      val values = answers.groupBy(x => x.classification).mapValues(_.size)
      val maxValue = values.map{case (k,v) => v}.max
      val output = values.filter{case (k,v) => v == maxValue}.keys.toArray
      output(Random.nextInt(output.length))
  }

//  def classifiyInstance(data: Instance, trees: List[(DecisionTree, Set[Instance])]): String = {
//    getMaxClass(trees.filter{case (tree,subsample) => !subsample.contains(data)}.map(_._1.classify(data.features)).groupBy(x => x))
//  }

  private def isError(instance: Instance, index: Int, classifications: List[Classification]): Boolean = {
    getMaxClass(classifications) != instance.label
  }

  def getOOBError(data: List[Instance], trees: List[(DecisionTree, List[Classification])]): Double = {
    val classifications:Map[Int, List[Classification]] = trees.map{case(k,v) => v}.flatten.groupBy(x => x.i)
    val errors: Int = data.zipWithIndex.map{case(instance, index) => isError(instance, index, classifications.getOrElse(index, List.empty[Classification]))}.count(x => x)
//    val errors = data.count(instance => classifiyInstance(instance, trees) != instance.label)
    val errorRate = errors / data.size.toDouble
    println("OOB Error Rate, Number of Errors: ", errors, " ErrorRate: ", errorRate)
    errorRate
  }

  sealed case class Classification(i: Int, classification: String)

  private def getTreePredictions(data: List[Instance], sample: Set[Instance], tree: DecisionTree): List[Classification] = {
    data.zipWithIndex.map{case(instance, index) => if (!sample.contains(instance)) Some(Classification(index, tree.classify(instance.features))) else None}.filter(_.isDefined).map(_.get)
  }

  private def runBatch(size: Int, leafSize: Int, numberOfFeatures: Int, data: List[Instance]): List[(DecisionTree, List[Classification])] = {
    Await.result(Future.sequence(List.range(0, size).map(x => Future{
        val sample = sampleDataSet(data)
        val tree = treeBuilder.getDecisionTree(leafSize, numberOfFeatures, sample)
        (tree, getTreePredictions(data, sample.toSet, tree))
    })), Duration.Inf)
  }

  def trainForest(numberOfTrees: Int, leafSize: Int, numberOfFeatures: Int, data: List[Instance]): RandomForest = {
    var completed = 0

    // Throttle the futures into groups of 8, same size as thread pool

    val trees:List[(DecisionTree, List[Classification])] = List.range(0, numberOfTrees).grouped(8).map(x => {
      val output = runBatch(x.size, leafSize, numberOfFeatures, data)
      completed += x.size
      print("Trees Trained: " + completed + "\r")
      output
    }).toList.flatten


//      x => Future{
//      val subsample = sampleDataSet(data)
//      val tree = treeBuilder.getDecisionTree(leafSize, numberOfFeatures, subsample)
//      completed += 1
//      print("Trees Trained: " + completed + "\r")
//      (tree, subsample.toSet)
//    })
//    val trees:List[(DecisionTree, Set[Instance])] = Await.result(Future.sequence(futures), Duration.Inf)


    RandomForest(trees.map(_._1), getOOBError(data, trees))

//      .foldLeft(List.empty[List[DecisionTree]])((trees, batch) => {
//      Await.result(Future.sequence(x => Future {
//        val subsample = sampleDataSet(data)
//        val oobSample = subsample.toSet.diff(data.toSet)
//        val tree = treeBuilder.getDecisionTree(leafSize, numberOfFeatures, subsample)
//        completed += 1
//        print("Trees Trained: " + completed + "\r")
//        (tree, oobSample)
//      }), Duration.Inf) :: trees
//    }).flatten


  }
}