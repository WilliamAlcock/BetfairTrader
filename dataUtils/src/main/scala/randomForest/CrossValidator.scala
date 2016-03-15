package randomForest

trait CrossValidator {

  def testForest(forest: RandomForest, testingSet: List[Instance]): Map[String, Map[String, Int]] = {
    var output = Map.empty[String, Map[String, Int]]

    testingSet.map(instance => {
      val current = output.getOrElse(instance.label, Map.empty[String, Int])
      val classification = forest.getClassification(instance.features)

      output = output + (instance.label -> (current + (classification -> (current.getOrElse(classification, 0) + 1))))
    })
    output
  }

  def mergeMap(x: Map[String, Int], y: Map[String, Int]): Map[String, Int] = y.foldLeft(x)((acc, m) => acc + (m._1 -> (acc.getOrElse(m._1, 0) + m._2)))

  def mergeErrors(x: Map[String, Map[String, Int]], y: Map[String, Map[String, Int]]): Map[String, Map[String, Int]] =
    y.foldLeft(x)((acc, m) => acc + (m._1 -> mergeMap(acc.getOrElse(m._1, Map.empty[String, Int]), m._2)))

  def crossValidateForest(numberOfTrees: Int,
                          leafSize: Int,
                          numberOfFeatures: Int,
                          forestBuilder: RandomForestBuilder,
                          data: List[Instance],
                          numberOfFolds: Int = 10): Map[String, Map[String, Int]] = {

    val foldSize = data.size / numberOfFolds
    val folds = scala.util.Random.shuffle(data).grouped(foldSize).toList.take(10)

    val errors: List[Map[String, Map[String, Int]]] = List.range(0, folds.size).map(testingFold => folds.splitAt(testingFold) match {
      case (left, right) =>
        println("Testing with Fold: " + testingFold)
        val output = testForest(
          forestBuilder.trainForest(numberOfTrees, leafSize, numberOfFeatures, (left ++ right.tail).flatten),
          right.head
        )
        println(output)
        output
    })

    val output = errors.reduceLeft((l,r) => mergeErrors(l,r))
    println(output)
    output
  }
}