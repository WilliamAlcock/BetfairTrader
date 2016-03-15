//package randomForest
//
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.{WordSpec, Matchers}
//
//class RandomForestSpec extends WordSpec with Matchers with MockFactory {
//
//  "RandomForestUtils.sampleWithReplacement" when {
//    "given a list of instances" should {
//      "return a list of instances the same size, randomly picked the input list" in {
//
//      }
//
//      "return a set of all the indexes randomly picked from the input list" in {
//
//      }
//    }
//  }
//
//  "RandomForestUtils.buildTree" when {
//    "given a leafSize l, numberOfFeatures n and a list of Instance I" should {
//      "create a sample of I with replacement" in {
//
//      }
//
//      "train a decision tree using the sample" in {
//
//      }
//
//      "return the tree and the indexes used for the sample" in {
//
//      }
//    }
//  }
//
//  "RandomForestUtils.getOOBLabel" when {
//    "given a list of trees and a set of indexes for the instances used to train them, a list of features and an index for the features" should {
//      "classify the features using every tree that WAS NOT trained with the corresponding index" in {
//
//      }
//
//      "return the majority vote of the classifications" in {
//
//      }
//    }
//  }
//
//  "RandomForestUtils.getOOBErrorRate" when {
//    "given a set of trees and a set of indexes for the instances used to train them and a the original dataset" should {
//      "get the out-of-bag classification for each instance" in {
//
//      }
//
//      "return the resulting error rate of the forest" in {
//
//      }
//
//    }
//  }
//
//  "RandomForestUtils.trainForest" when {
//    "given a numberOfTrees n, leafSize l and a list of instance l" should {
//      "return a forest of n trees, and the out-of-bag error rate of the forest" in {
//
//      }
//    }
//
//  }
//}
//
//
//// TODO add a test for classification
//// TODO add a functionality for missing variables
