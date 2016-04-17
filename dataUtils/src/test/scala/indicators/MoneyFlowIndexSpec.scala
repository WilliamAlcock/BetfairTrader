package indicators

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

class MoneyFlowIndexSpec extends FlatSpec with Matchers with TestHelpers {
  val testData = Table(
    ("typicalPrice", "closeDelta",    "volume",  "rawFlow",   "posFlow",    "negFlow",    "avgPosFlow",       "avgNegFlow",       "ratio",            "index"),

    (24.63246667,    "NONE",          18730.144, None,              None,               None,               None,               None,               None,               None),
    (24.6889,	       "UP",            12271.74,	 Some(302975.7617), Some(302975.7617),  Some(0.0),          None,               None,               None,               None),
    (24.99086667,	   "UP",	          24691.414, Some(617059.8351), Some(617059.8351),  Some(0.0),          None,               None,               None,               None),
    (25.35916667,	   "UP",            18357.606, Some(465533.5902),	Some(465533.5902),	Some(0.0),          None,               None,               None,               None),
    (25.18663333,	   "DOWN",	        22964.08,	 Some(578387.8628),	Some(0.0),	        Some(578387.8628),  None,               None,               None,               None),
    (25.16673333,	   "DOWN",	        15918.948, Some(400627.9193),	Some(0.0),	        Some(400627.9193),  None,               None,               None,               None),
    (25.00746667,	   "DOWN",	        16067.044, Some(401796.0673),	Some(0.0),	        Some(401796.0673),  None,               None,               None,               None),
    (24.958,	       "DOWN",	        16568.487, Some(413516.2985),	Some(0.0),	        Some(413516.2985),  None,               None,               None,               None),
    (25.08376667,	   "UP",	          16018.729, Some(401810.0605),	Some(401810.0605),	Some(0.0),          None,               None,               None,               None),
    (25.2497,	       "UP",	          9773.569,	 Some(246779.6852),	Some(246779.6852),	Some(0.0),          None,               None,               None,               None),
    (25.2132,	       "DOWN",	        22572.712, Some(569130.3022),	Some(0.0),	        Some(569130.3022),  None,               None,               None,               None),
    (25.37083333,	   "UP",	          12986.669, Some(329482.6148),	Some(329482.6148), 	Some(0.0),          None,               None,               None,               None),
    (25.6147,	       "UP",	          10906.659, Some(279370.7983),	Some(279370.7983),	Some(0.0),          None,               None,               None,               None),
    (25.5782,	       "DOWN",	        5799.259,	 Some(148334.6066),	Some(0.0),	        Some(148334.6066),  None,               None,               None,               None),
    (25.45543333,	   "DOWN",	        7395.274,	 Some(188249.9043),	Some(0.0),	        Some(188249.9043),	Some(2643012.346),	Some(2700042.961),	Some(0.978877886),	Some(49.46631083)),
    (25.326,	       "DOWN",	        5818.162,	 Some(147350.7708),	Some(0.0),	        Some(147350.7708),	Some(2340036.584),	Some(2847393.732),	Some(0.821817003),	Some(45.10974493)),
    (25.0904,	       "DOWN",	        7164.726,	 Some(179765.8412),	Some(0.0),	        Some(179765.8412),	Some(1722976.749),	Some(3027159.573),	Some(0.569172753),	Some(36.27215373)),
    (25.02733333,	   "DOWN",	        5672.914,	 Some(141977.9096),	Some(0.0),	        Some(141977.9096),	Some(1257443.159),	Some(3169137.483),	Some(0.396777724),	Some(28.40664749)),
    (24.91453333,	   "DOWN",	        5624.742,	 Some(140137.8221),	Some(0.0),	        Some(140137.8221),	Some(1257443.159),	Some(2730887.442),	Some(0.46045221),	  Some(31.52805734)),
    (24.8946,	       "DOWN",	        5023.469,	 Some(125057.2514),	Some(0.0),	        Some(125057.2514),	Some(1257443.159),	Some(2455316.774),	Some(0.512130725),	Some(33.8681515)),
    (25.12693333,	   "UP",	          7457.091,	 Some(187373.8284),	Some(187373.8284),  Some(0.0),	        Some(1444816.987),	Some(2053520.707),  Some(0.703580433),  Some(41.30010061)),
    (24.6358,	       "DOWN",	        11798.009, Some(290653.3901),	Some(0.0),       	  Some(290653.3901),	Some(1444816.987),	Some(1930657.798),	Some(0.748354778),	Some(42.80337076)),
    (24.5097,	       "DOWN",	        12366.132, Some(303090.1855),	Some(0.0),       	  Some(303090.1855),	Some(1043006.927),	Some(2233747.984),	Some(0.466931334),	Some(31.83048336)),
    (24.15463333,	   "DOWN",	        13294.865, Some(321132.5893),	Some(0.0),       	  Some(321132.5893),	Some(796227.2415),	Some(2554880.573),	Some(0.311649495),	Some(23.7601201)),
    (23.9771,	       "DOWN",	        9256.87,	 Some(221952.8977),	Some(0.0),       	  Some(221952.8977),	Some(796227.2415),	Some(2207703.169),	Some(0.360658649),	Some(26.50618133)),
    (24.06503333,	   "UP",	          9690.604,	 Some(233204.7083),	Some(233204.7083),	Some(0.0),	        Some(699949.335),	  Some(2207703.169),	Some(0.317048662),	Some(24.07266116)),
    (24.36036667,	   "UP",	          8870.318,	 Some(216084.1989),	Some(216084.1989),	Some(0.0),	        Some(636662.7356),	Some(2207703.169),	Some(0.288382399),	Some(22.38329234)),
    (24.3504,	       "DOWN",	        7168.965,	 Some(174567.1653),	Some(0.0),	        Some(174567.1653),	Some(636662.7356),	Some(2233935.727),	Some(0.284995995),	Some(22.17874579)),
    (24.14466667,	   "DOWN",	        11356.18,	 Some(274191.1807),	Some(0.0),	        Some(274191.1807),	Some(636662.7356),	Some(2319877.004),	Some(0.274438142),	Some(21.53404966)),
    (24.81,	         "UP",	          13379.374, Some(331942.2689),	Some(331942.2689),  Some(0.0),	        Some(968605.0046),	Some(2172526.233),	Some(0.44584272),	  Some(30.83618389))
  )

  case class Data(tick: Tick, moneyFlowIndex: MoneyFlowIndex) extends MoneyFlowIndexData

  // Helper class to override typical Price to match test data
  class TestTick(override val close: Double,
                      override val range: Range,
                      override val volume: Double,
                      override val closeDelta: Option[Double],
                      override val typicalPrice: Double) extends Tick(close, range, volume, closeDelta, 0.0, None)

  def getTestTick(tick: Tick, typicalPrice: Double): TestTick = new TestTick(tick.close, tick.range, tick.volume, tick.closeDelta, typicalPrice)

  "MoneyFlowIndex" should "produce the correct figures" in {
    var prevData = List.empty[Data]
    var prevClose = 0.0

    forAll(testData) {(typicalPrice: Double,
                       closeDelta: String,
                       volume: Double,
                       rawFlow: Option[Double],
                       posFlow: Option[Double],
                       negFlow: Option[Double],
                       avgPosFlow: Option[Double],
                       avgNegFlow: Option[Double],
                       ratio: Option[Double],
                       index: Option[Double]) =>

      val close = closeDelta match {
        case "DOWN" => prevClose - 1
        case "UP" => prevClose + 1
        case _ => prevClose
      }

      val tick = getTestTick(Tick.getNext(close, Range(0, 0), volume, 0.0, prevData), typicalPrice)
      val moneyFlowIndex = MoneyFlowIndex.getNext(tick, prevData)
      tick.typicalPrice should be (typicalPrice)
      matchOptionDouble(moneyFlowIndex.rawFlow, rawFlow, 0.0001)
      matchOptionDouble(moneyFlowIndex.posFlow, posFlow, 0.0001)
      matchOptionDouble(moneyFlowIndex.negFlow, negFlow, 0.0001)
      matchOptionDouble(moneyFlowIndex.avgPosFlow, avgPosFlow, 0.001)
      matchOptionDouble(moneyFlowIndex.avgNegFlow, avgNegFlow, 0.001)
      matchOptionDouble(moneyFlowIndex.ratio, ratio, 0.00000001)
      matchOptionDouble(moneyFlowIndex.index, index, 0.00000001)

      prevData = Data(tick, moneyFlowIndex) :: prevData
      prevClose = close
    }
  }
}

