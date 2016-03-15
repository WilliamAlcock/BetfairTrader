package randomForest

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class ExtremelyRandomForest extends RandomForestBuilder {

  val treeBuilder: DecisionTreeBuilder = new ExtremelyRandomDecisionTree()

  def trainForest(numberOfTrees: Int, leafSize: Int, numberOfFeatures: Int, data: List[Instance]): RandomForest = {
    var completed = 0

    // Throttle the futures into groups of 8, same size as thread pool
    val trees = List.range(0, numberOfTrees).grouped(40).foldLeft(List.empty[List[DecisionTree]])((trees, batch) => {
      Await.result(Future.sequence(batch.map(x => Future {
        val tree = treeBuilder.getDecisionTree(leafSize: Int, numberOfFeatures: Int, data: List[Instance])
        completed += 1
        print("Trees Trained: " + completed + "\r")
        tree
      })), Duration.Inf) :: trees
    })

    RandomForest(trees.flatten.toList)
  }
}