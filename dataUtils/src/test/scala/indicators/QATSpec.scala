package indicators

import org.scalatest.{FlatSpec, Matchers}

class QATSpec extends FlatSpec with Matchers with QAT {
  val testData = Array[Double](
    86.1557,
    89.0867,
    88.7829,
    90.3228,
    89.0671,
    91.1453,
    89.4397,
    89.175,
    86.9302,
    87.6752,
    86.9596,
    89.4299,
    89.3221,
    88.7241,
    87.4497,
    87.2634,
    89.4985,
    87.9006,
    89.126,
    90.7043,
    92.9001,
    92.9784,
    91.8021,
    92.6647,
    92.6843,
    92.3021,
    92.7725,
    92.5373,
    92.949,
    93.2039,
    91.0669,
    89.8318,
    89.7435,
    90.3994,
    90.7387,
    88.0177,
    88.0867,
    88.8439,
    90.7781,
    90.5416,
    91.3894,
    90.65
  )

  "getMean and getSimpleMovingAvergae" should "should produce the same figures" in {
    val period = 20 // 20 day simple moving average

    var prevData = List.empty[Double]
    var prevSMA: Option[Double] = None

    /*
      This test asserts that both functions for producing the mean return the same output
     */
    for (data <- testData) {
      prevData = data :: prevData
      val mean: Option[Double] = prevData.size match {
        case x if x >= period => Some(getMean(prevData.take(period)))
        case _ => None
      }
      val sma: Option[Double] = prevSMA match {
        case Some(x) =>
          Some(getNextSMA(period, data, prevData(period), x))
        case _ => None
      }
      (mean, sma) match {
        case (Some(x), Some(y)) =>
          x should be(y +- 0.000001)
          prevSMA = sma
        case (None, Some(x)) => fail()
        case (Some(x), None) => prevSMA = mean
        case (None, None) =>
      }
    }
  }
}
