//package randomForest
//
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.{Matchers, WordSpec}
//import org.scalatest.prop.TableDrivenPropertyChecks._
//
//
//class DecisionTreeSpec extends WordSpec with Matchers with MockFactory {
//
//  class TestDecisionTreeUtils extends DecisionTreeUtils
//
//  "DecisionTreeUtils.getDecisionTree" when {
//    val mockData = List(Instance(List(1.0), "TEST_LABEL"))
//    val mockLeafSize = 1
//    val mockNumberOfFeatures = 0
//
//    "the number of classes in the data is <= leafSize" should {
//      "return a LeafNode" in {
//        val mockGetLabels = mockFunction[List[Instance], Map[String, Int]]
//        val mockGetInnerNode = mockFunction[Int, Int, List[Instance], InnerNode]
//
//        val decisionTreeUtils = new TestDecisionTreeUtils() {
//          override def getLabels(data: List[Instance]): Map[String, Int] = mockGetLabels.apply(data)
//          override def getInnerNode(leafSize: Int, numberOfFeatures: Int, data: List[Instance], level: List[Int]): InnerNode = mockGetInnerNode(leafSize, numberOfFeatures, data)
//        }
//
//        val mockLabels = Map("TEST_LABEL" -> 1)
//        mockGetLabels.expects(mockData).returns(mockLabels)
//
//        val mockLeafNode = LeafNode(mockLabels)
//        decisionTreeUtils.getDecisionTree(mockLeafSize, mockNumberOfFeatures, mockData) should be (mockLeafNode)
//      }
//    }
//
//    "the number of classes in the data is > leafSize" should {
//      "return an InnerNode" in {
//        val mockGetLabels = mockFunction[List[Instance], Map[String, Int]]
//        val mockGetInnerNode = mockFunction[Int, Int, List[Instance], InnerNode]
//
//        val decisionTreeUtils = new TestDecisionTreeUtils() {
//          override def getLabels(data: List[Instance]): Map[String, Int] = mockGetLabels.apply(data)
//          override def getInnerNode(leafSize: Int, numberOfFeatures: Int, data: List[Instance], level: List[Int]): InnerNode = mockGetInnerNode(leafSize, numberOfFeatures, data)
//        }
//
//        val mockLabels = Map("TEST_LABEL_1" -> 1, "TEST_LABEL_2" -> 2)
//        mockGetLabels.expects(mockData).returns(mockLabels)
//
//        val mockInnerNode = InnerNode(1, 2, LeafNode(Map.empty), LeafNode(Map.empty))
//        mockGetInnerNode.expects(mockLeafSize, mockNumberOfFeatures, mockData).returns(mockInnerNode)
//
//        decisionTreeUtils.getDecisionTree(mockLeafSize, mockNumberOfFeatures, mockData) should be (mockInnerNode)
//      }
//    }
//  }
//
//  "DecisionTreeUtils.getInnerNode" when {
//    "given a leafSize l, numberOfFeatures nf, and a list of Instances" should {
//      "Select nf random features, For each feature select the threshold with min entropy and split the data using the feature with the lowest entropy returning an InnerNode" in {
//        val mockGetRandomFeatures = mockFunction[Int, Int, List[Int]]
//        val mockGetThresholdAndEntropyForFeature = mockFunction[Int, List[Instance], (Double, Double)]
//        val mockSplitData = mockFunction[Int, Double, List[Instance], (List[Instance], List[Instance])]
//        val mockGetDecisionTree = mockFunction[Int, Int, List[Instance], DecisionTree]
//
//        val decisionTreeUtils = new TestDecisionTreeUtils() {
//          override def getRandomFeatures(numberOfFeatures: Int, maxIndex: Int): List[Int] = mockGetRandomFeatures.apply(numberOfFeatures, maxIndex)
//          override def getThresholdAndEntropyForFeature(feature: Int, data: List[Instance]): (Double, Double) = mockGetThresholdAndEntropyForFeature.apply(feature, data)
//          override def splitData(feature: Int, threshold: Double, data: List[Instance]): (List[Instance], List[Instance]) = mockSplitData.apply(feature, threshold, data)
//          override def getDecisionTree(leafSize: Int, numberOfFeatures: Int, data: List[Instance], level: List[Int]): DecisionTree = mockGetDecisionTree.apply(leafSize, numberOfFeatures, data)
//        }
//
//        val mockLeafSize = 1
//        val mockNumberOfFeatures = 2
//        val mockData = List(
//          Instance(List(1, 4, 6), "TEST_LABEL_1"),
//          Instance(List(2, 5, 7), "TEST_LABEL_2"),
//          Instance(List(3, 6, 8), "TEST_LABEL_3")
//        )
//
//        val mockRandomFeatures = List(0, 1)
//        val mockThresholdAndEntropy = List(1.0 -> 4.0, 2.0 -> 5.0)
//
//        mockGetRandomFeatures.expects(mockNumberOfFeatures, mockData.size).returns(mockRandomFeatures)
//        mockGetThresholdAndEntropyForFeature.expects(mockRandomFeatures(0), mockData).returns(mockThresholdAndEntropy(0))
//        mockGetThresholdAndEntropyForFeature.expects(mockRandomFeatures(1), mockData).returns(mockThresholdAndEntropy(1))
//
//        val mockLeftSplit = List(Instance(List.empty, "LEFT SPLIT"))
//        val mockRightSplit = List(Instance(List.empty, "RIGHT SPLIT"))
//
//        mockSplitData.expects(mockRandomFeatures(0), mockThresholdAndEntropy(0)._1, mockData).returns((mockLeftSplit, mockRightSplit))
//
//        val mockDecisionTrees = List(LeafNode(Map("Label_1" -> 1)), LeafNode(Map("Label_2" -> 2)))
//
//        mockGetDecisionTree.expects(mockLeafSize, mockNumberOfFeatures, mockLeftSplit).returns(mockDecisionTrees(0))
//        mockGetDecisionTree.expects(mockLeafSize, mockNumberOfFeatures, mockRightSplit).returns(mockDecisionTrees(1))
//
//        val mockInnerNode = InnerNode(mockRandomFeatures(0), mockThresholdAndEntropy(0)._1, mockDecisionTrees(0), mockDecisionTrees(1))
//
//        decisionTreeUtils.getInnerNode(mockLeafSize, mockNumberOfFeatures, mockData, List(0)) should be (mockInnerNode)
//      }
//    }
//  }
//
//  "DecisionTreeUtils.getLabels" when {
//    val decisionTreeUtils = new TestDecisionTreeUtils()
//
//    "called" should {
//      val data = List(
//        Instance(List(), "LABEL_1"),
//        Instance(List(), "LABEL_1"),
//        Instance(List(), "LABEL_1"),
//        Instance(List(), "LABEL_2"),
//        Instance(List(), "LABEL_2")
//      )
//
//      val expectedOutput = Map(
//        "LABEL_1" -> 3,
//        "LABEL_2" -> 2
//      )
//
//      "return for each label the number of instances in the data" in {
//        decisionTreeUtils.getLabels(data) should be(expectedOutput)
//      }
//    }
//  }
//
//  "DecisionTreeUtils.splitData" should {
//    val decisionTreeUtils = new TestDecisionTreeUtils()
//
//    val mockFeature = 0           // split on the 0 indexed feature in the data
//    val mockThreshold = 5
//
//    val mockData = List(          // splitData should ignore the 2nd feature as the instruction is to split on the 0 indexed (1st) feature
//      Instance(List(1, 9), "Instance_1"),
//      Instance(List(2, 8), "Instance_2"),
//      Instance(List(5, 8), "Instance_3"),
//      Instance(List(8, 2), "Instance_4"),
//      Instance(List(9, 1), "Instance_5")
//    )
//
//    val expectedOutput = (
//      List (
//        Instance(List(1, 9), "Instance_1"),
//        Instance(List(2, 8), "Instance_2"),
//        Instance(List(5, 8), "Instance_3")
//
//      ),
//      List(
//        Instance(List(8, 2), "Instance_4"),
//        Instance(List(9, 1), "Instance_5")
//      )
//    )
//
//    "split the data into two halves, the first contains all the data who's feature is <= threshold, the second contains the rest" in {
//      decisionTreeUtils.splitData(mockFeature, mockThreshold, mockData) should be(expectedOutput)
//    }
//  }
//
//  "DecisionTreeUtils.getThresholdAndEntropyForFeature" when {
//    "the possible number of thresholds is 0" should {
//      "throw an IllegalArgumentException" in {
//        val mockGetPossibleThresholds = mockFunction[Int, List[Instance], Set[Double]]
//
//        val decisionTreeUtils = new TestDecisionTreeUtils() {
//          override def getPossibleThresholds(feature: Int, data: List[Instance]): Set[Double] = mockGetPossibleThresholds.apply(feature, data)
//        }
//
//        val mockFeature = 1
//        val mockData = List(Instance(List(), "TEST_INSTANCE"))
//
//        mockGetPossibleThresholds.expects(mockFeature, mockData).returns(Set.empty[Double])
//
//        try {
//          decisionTreeUtils.getThresholdAndEntropyForFeature(mockFeature, mockData)
//        } catch {
//          case x: IllegalArgumentException => x.getMessage() should be("No possible thresholds in the data")
//          case _: Throwable => fail()
//        }
//      }
//    }
//  }
//
//  "DecisionTreeUtils.getThresholdAndEntropyForFeature" when {
//    "the possible number of thresholds is 1" should {
//      "return the threshold and its entropy" in {
//        val mockGetPossibleThresholds = mockFunction[Int, List[Instance], Set[Double]]
//        val mockGetEntropyOfThreshold = mockFunction[Int, Double, List[Instance], Double]
//
//        val decisionTreeUtils = new TestDecisionTreeUtils() {
//          override def getPossibleThresholds(feature: Int, data: List[Instance]): Set[Double] = mockGetPossibleThresholds.apply(feature, data)
//          override def getEntropyOfThreshold(feature: Int, threshold: Double, data: List[Instance]): Double = mockGetEntropyOfThreshold(feature, threshold, data)
//        }
//
//        val mockFeature = 1
//        val mockData = List(Instance(List(), "TEST_INSTANCE"))
//        val mockThreshold = 10.0
//        val mockEntropy = 20.0
//
//        mockGetPossibleThresholds.expects(mockFeature, mockData).returns(Set(mockThreshold))
//        mockGetEntropyOfThreshold.expects(mockFeature, mockThreshold, mockData).returns(mockEntropy)
//
//        decisionTreeUtils.getThresholdAndEntropyForFeature(mockFeature, mockData)
//      }
//    }
//  }
//
//  "DecisionTreeUtils.getThresholdAndEntropyForFeature" when {
//    "the possible number of thresholds is greater than 1" should {
//      "return the threshold that yields the lowest entropy" in {
//        val mockGetPossibleThresholds = mockFunction[Int, List[Instance], Set[Double]]
//        val mockGetEntropyOfThreshold = mockFunction[Int, Double, List[Instance], Double]
//
//        val decisionTreeUtils = new TestDecisionTreeUtils() {
//          override def getPossibleThresholds(feature: Int, data: List[Instance]): Set[Double] = mockGetPossibleThresholds.apply(feature, data)
//          override def getEntropyOfThreshold(feature: Int, threshold: Double, data: List[Instance]): Double = mockGetEntropyOfThreshold(feature, threshold, data)
//        }
//
//        val mockFeature = 1
//        val mockData = List(Instance(List(), "TEST_INSTANCE"))
//        val mockThresholds = Set(10.0, 20.0, 30.0)
//
//        mockGetPossibleThresholds.expects(mockFeature, mockData).returns(mockThresholds)
//        mockGetEntropyOfThreshold.expects(mockFeature, 10.0, mockData).returns(100.0)
//        mockGetEntropyOfThreshold.expects(mockFeature, 20.0, mockData).returns(50.0)
//        mockGetEntropyOfThreshold.expects(mockFeature, 30.0, mockData).returns(200.0)
//
//        decisionTreeUtils.getThresholdAndEntropyForFeature(mockFeature, mockData) should be((20.0, 50.0))
//      }
//    }
//  }
//
//  "DecisionTreeUtils.getPossibleThresholds" when {
//    val decisionTreeUtils = new TestDecisionTreeUtils()
//
//    val mockFeature = 0         // return possible thresholds for the 0 indexed feature in the data
//    val mockData = List(        // should ignore the 2nd feature as the instruction is to return thresholds for the 0 indexed (1st) feature
//      Instance(List(1, 6), "Instance_1"),
//      Instance(List(2, 7), "Instance_2"),
//      Instance(List(3, 8), "Instance_3"),
//      Instance(List(4, 9), "Instance_4"),
//      Instance(List(5, 10), "Instance_5")
//    )
//
//    val expectedOutput = Set(1, 2, 3, 4)
//
//    "return all values for the given feature except the last value" in {
//      decisionTreeUtils.getPossibleThresholds(mockFeature, mockData) should be(expectedOutput)
//    }
//  }
//
//  "DecisionTreeUtils.getEntropyOfThreshold" when {
//    "given a list of instances, the index of a feature and a threshold" should {
//      "split the data in half and return the sum of: the weight of each half * the entropy of each half" in {
//        val mockSplitData = mockFunction[Int, Double, List[Instance], (List[Instance], List[Instance])]
//        val mockGetWeight = mockFunction[Int, Int, Double]
//        val mockGetEntropyOfData = mockFunction[List[Instance], Double]
//
//        val decisionTreeUtils = new TestDecisionTreeUtils() {
//          override def splitData(feature: Int, threshold: Double, data: List[Instance]): (List[Instance], List[Instance]) = mockSplitData.apply(feature, threshold, data)
//          override def getWeight(n: Int, d: Int) = mockGetWeight.apply(n, d)
//          override def getEntropyOfData(data: List[Instance]) = mockGetEntropyOfData.apply(data)
//        }
//
//        val mockFeature = 1
//        val mockThreshold = 2.0
//
//        val mockData = List(
//          Instance(List(), "TEST_INSTANCE"),
//          Instance(List(), "TEST_INSTANCE"),
//          Instance(List(), "TEST_INSTANCE")
//        )
//
//        val leftHalf = List(
//          Instance(List(), "TEST_INSTANCE")
//        )
//
//        val rightHalf = List(
//          Instance(List(), "TEST_INSTANCE"),
//          Instance(List(), "TEST_INSTANCE")
//        )
//
//        mockSplitData.expects(mockFeature, mockThreshold, mockData).returns((leftHalf, rightHalf))
//        mockGetWeight.expects(leftHalf.size, mockData.size).returns(1)
//        mockGetWeight.expects(rightHalf.size, mockData.size).returns(2)
//        mockGetEntropyOfData.expects(leftHalf).returns(10)
//        mockGetEntropyOfData.expects(rightHalf).returns(20)
//
//        val expectedOutput = 50.0 //  (1 * 10) + (2 * 20)
//
//        decisionTreeUtils.getEntropyOfThreshold(mockFeature, mockThreshold, mockData) should be(expectedOutput)
//      }
//    }
//  }
//
//  "DecisionTreeUtils.getEntropyOfData" when {
//    "given a list of instances" should {
//      "return the sum of entropy (derived using the probability for each label) for each label in the data" in {
//        val mockGetProbabilities = mockFunction[List[Instance], Map[String, Double]]
//        val mockGetEntropy = mockFunction[Double, Double]
//
//        val decisionTreeUtils = new TestDecisionTreeUtils() {
//          override def getProbabilities(data: List[Instance]): Map[String, Double] = mockGetProbabilities(data)
//          override def getEntropy(p: Double): Double = mockGetEntropy.apply(p)
//        }
//
//        val mockData = List(Instance(List(), "MOCK_INSTANCE"))
//        val mockProbabilities = Map("LABEL_1" -> 0.5, "LABEL_2" -> 0.3, "LABEL_3" -> 0.2)
//
//        mockGetProbabilities.expects(mockData).returns(mockProbabilities)
//        mockGetEntropy.expects(0.5).returns(1.1)
//        mockGetEntropy.expects(0.3).returns(2.1)
//        mockGetEntropy.expects(0.2).returns(3.1)
//
//        val expectedOutput = 6.3
//
//        decisionTreeUtils.getEntropyOfData(mockData) should be (expectedOutput +- 0.00000000001)
//      }
//    }
//  }
//
//  "DecisionTreeUtils.getProbabilities" when {
//    "given a list of instances" should {
//      "return the implied probability (the weight) of each label given the data" in {
//        val decisionTreeUtils = new TestDecisionTreeUtils()
//
//        val mockData = List(
//          Instance(List(), "TEST_1"),
//          Instance(List(), "TEST_1"),
//          Instance(List(), "TEST_1"),
//          Instance(List(), "TEST_1"),
//          Instance(List(), "TEST_2")
//        )
//
//        val expectedOutput = Map("TEST_1" -> 0.8, "TEST_2" -> 0.2)
//
//        decisionTreeUtils.getProbabilities(mockData) should be(expectedOutput)
//      }
//    }
//  }
//
//  "DecisionTreeUtils.getEntropy" when {
//    "given a probability p" should {
//      "return - p * log base 2 of p" in {
//        val mockLog2 = mockFunction[Double, Double]
//
//        val decisionTreeUtils = new TestDecisionTreeUtils() {
//          override def log2(x: Double): Double = mockLog2.apply(x)
//        }
//
//        val p = 10.0
//        mockLog2.expects(10.0).returns(2)
//        val expectedOutput = -20
//
//        decisionTreeUtils.getEntropy(p) should be(expectedOutput)
//      }
//    }
//  }
//
//  "DecisionTreeUtils.getWeight" when {
//    "given two integers" should {
//      "divide the first by the second returning a double" in {
//        val decisionTreeUtils = new TestDecisionTreeUtils()
//
//        val testData = Table(
//          ("x", "y", "expectedOutput"),
//          (10,  4,   2.5),
//          (10,  5,   2.0)
//        )
//
//        forAll(testData){(x: Int, y: Int, expectedOutput: Double) => decisionTreeUtils.getWeight(x, y) should be(expectedOutput)}
//      }
//    }
//  }
//
//  "DecisionTreeUtils.log2" when {
//    "given a double" should {
//      "return log base 2 of the input" in {
//        val decisionTreeUtils = new TestDecisionTreeUtils()
//
//        val testData = Table(
//          ("input", "expectedOutput"),
//          (1.0,     0.0),
//          (2.0,     1.0),
//          (3.0,     1.584962500721156),
//          (4.0,     2.0),
//          (5.0,     2.321928094887362),
//          (6.0,     2.584962500721156),
//          (7.0,     2.807354922057604),
//          (8.0,     3.0),
//          (9.0,     3.169925001442312),
//          (10.0,    3.321928094887362)
//        )
//
//        forAll(testData){(input: Double, expectedOutput: Double) => decisionTreeUtils.log2(input) should be(expectedOutput +- 0.00000000000001)}
//      }
//    }
//  }
//}