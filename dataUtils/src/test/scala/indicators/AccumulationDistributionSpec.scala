package indicators

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

class AccumulationDistributionSpec extends FlatSpec with Matchers with TestHelpers {
  val testData = Table(
    ("high",  "low",    "close", "volume",    "moneyFlowMultiplier", "moneyFlowVolume", "accDistLine",  "lineDelta"),
    (62.34,	  61.37,	  62.15,	 7849.025, 	   0.608247423,           4774.149227, 	     4774.149227,   None),
    (62.05,	  60.69,	  60.81,   11692.075,   -0.823529412,	         -9628.767647,	    -4854.61842,    Some(-201.685519)),
    (62.27,	  60.1,	    60.45,   10575.307,   -0.677419355,	         -7163.917645,	    -12018.53607,   Some(147.5691193)),
    (60.79,	  58.61,	  59.18,	 13059.128, 	-0.47706422,           -6230.042716,	    -18248.57878,   Some(51.83695147)),
    (59.93,	  58.712,	  59.24,   20733.508,   -0.133004926,	         -2757.6587,	      -21006.23748,   Some(15.11163545)),
    (61.75,	  59.86,	  60.2,	   29630.096, 	-0.64021164,           -18969.53236,	    -39975.76984,   Some(90.30428404)),
    (60.0,	  57.97,	  58.48,   17705.294,   -0.497536946,	         -8809.037901,	    -48784.80774,   Some(22.03594311)),
    (59.0,	  58.02,	  58.24,   7259.203, 	  -0.551020408,          -3999.969,	        -52784.77674,   Some(8.199210339)),
    (59.07,	  57.48,	  58.69,	 10474.629, 	 0.522012579,           5467.888094,	    -47316.88865,   Some(-10.35883531)),
    (59.22,	  58.3,	    58.65,   5203.714, 	  -0.239130435,          -1244.366391,	    -48561.25504,   Some(2.62985675)),
    (58.75,	  57.8276,	58.47,	 3422.865,	   0.392888118,           1344.802988,	    -47216.45205,   Some(-2.76929208)),
    (58.65,	  57.86,	  58.02,   3962.15,	    -0.594936709,          -2357.228481,	    -49573.68053,   Some(4.992387987)),
    (58.47,	  57.91,	  58.17,   4095.905,	  -0.071428571,          -292.5646429,	    -49866.24517,   Some(0.590161224)),
    (58.25,	  57.8333,	58.07,	 3766.006,	   0.136069114,           512.4371015,	    -49353.80807,   Some(-1.027623191)),
    (58.35,	  57.53,	  58.13,	 4239.335,	   0.463414634,           1964.569878,	    -47389.2382,    Some(-3.980584167)),
    (59.86,	  58.58,	  58.94,	 8039.979,	  -0.4375,               -3517.490813,	    -50906.72901,   Some(7.42255192)),
    (59.5299, 58.3,	    59.1 ,	 6956.717,	   0.300918774,           2093.40675,	      -48813.32226,   Some(-4.112239758)),
    (62.1,	  58.53,	  61.92,	 18171.552,	   0.899159664,           16339.12659,	    -32474.19567,   Some(-33.47267884)),
    (62.16,	  59.8,	    61.37,	 22225.894,	   0.330508475,           7345.846322,	    -25128.34935,   Some(-22.62056432)),
    (62.67,	  60.93,	  61.68,   14613.509,   -0.137931034,	         -2015.656414,	    -27144.00576,   Some(8.021443756)),
    (62.38,	  60.15,	  62.09,	 12319.763,	   0.739910314,           9115.519709,	    -18028.48605,   Some(-33.58207256)),
    (63.73,	  62.2618,	62.89,   15007.69,	  -0.144258275,          -2164.983478,	    -20193.46953,   Some(12.00868156)),
    (63.85,	  63.0,	    63.53,	 8879.667,	   0.247058824,           2193.800082,	    -17999.66945,   Some(-10.86390864)),
    (66.15,	  63.58,	  64.01,	 22693.812,	  -0.66536965,           -15099.77374,	    -33099.44319,   Some(83.88917242)),
    (65.34,	  64.07,	  64.77,	 10191.814,	   0.102362205,           1043.256551,	    -32056.18664,   Some(-3.151885499)),
    (66.48,	  65.2,	    65.22,	 10074.152,	  -0.96875,              -9759.33475,	      -41815.52139,   Some(30.44446571)),
    (65.23,	  63.21,	  63.28,   9411.62,	    -0.930693069,          -8759.329505,	    -50574.8509,    Some(20.9475554)),
    (63.4,	  61.88,	  62.4,    10391.69,	  -0.315789474,          -3281.586316,	    -53856.43721,   Some(6.488573375)),
    (63.18,	  61.11,	  61.55,   8926.512,	  -0.574879227,          -5131.666319,	    -58988.10353,   Some(9.528417745)),
    (62.7,	  61.25,	  62.69,	 7459.575,	   0.986206897,           7356.68431,	      -51631.41922,   Some(-12.47147114))
  )

  case class Data(tick: Tick, accumulationDistribution: AccumulationDistribution) extends AccumulationDistributionData

  "AccumulationDistribution" should "produce the correct figures" in {
    var prevData = List.empty[Data]

    forAll(testData) { (high: Double, low: Double, close: Double, volume: Double, moneyFlowMultiplier: Double, moneyFlowVolume: Double, accDistLine: Double, lineDelta: Option[Double]) =>
      val tick = Tick.getNext(close, Range(high, low), volume, 0.0, prevData)
      val accumulationDistribution = AccumulationDistribution.getNext(tick, prevData)
      accumulationDistribution.moneyFlowMultiplier should be(moneyFlowMultiplier +- 0.0001)
      accumulationDistribution.moneyFlowVolume should be(moneyFlowVolume +- 0.0001)
      accumulationDistribution.line should be(accDistLine +- 0.0001)
      matchOptionDouble(accumulationDistribution.lineDelta, lineDelta, 0.0000001)
      prevData = Data(tick, accumulationDistribution):: prevData
    }
  }
}