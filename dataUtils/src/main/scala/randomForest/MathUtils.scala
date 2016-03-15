package randomForest

trait MathUtils {
  // Returns log base 2 of x
  def log2(x: Double): Double = scala.math.log(x) / scala.math.log(2)

  // Returns a weight given two integers
  def getWeight(n: Int, d: Int): Double = n.toDouble / d

  // Returns entropy given a probability
  def getEntropy(p: Double): Double = -p * log2(p)
}