package indicators

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

class CommodityChannelIndexSpec extends FlatSpec with Matchers with TestHelpers {
  val period = 20 // 20 day look-back period

  val testData = Table(
    ("high",  "low",    "close",  "typicalPrice", "smaOfTP",          "cciMeanDeviation", "cci"),

    (24.2013,	23.8534,	23.8932,	23.98263333,    None,               None,               None),
    (24.0721,	23.7242,	23.9528,	23.91636667,    None,               None,               None),
    (24.0423,	23.6447,	23.6745,	23.78716667,    None,               None,               None),
    (23.8733,	23.3664,	23.7839,	23.67453333,    None,               None,               None),
    (23.6745,	23.4559,	23.4956,	23.54200000,    None,               None,               None),
    (23.5851,	23.1776,	23.3217,	23.36146667,    None,               None,               None),
    (23.8037,	23.3962,	23.754, 	23.65130000,    None,               None,               None),
    (23.8036,	23.5652,	23.7938,	23.72086667,    None,               None,               None),
    (24.3007,	24.0522,	24.1417,	24.16486667,    None,               None,               None),
    (24.1516,	23.7739,	23.8137,	23.91306667,    None,               None,               None),
    (24.0522,	23.595,	  23.7839,	23.81036667,    None,               None,               None),
    (24.0622,	23.8435,	23.8634,	23.92303333,    None,               None,               None),
    (23.8833,	23.6447,	23.7044,	23.74413333,    None,               None,               None),
    (25.1356,	23.9429,	24.9567,	24.67840000,    None,               None,               None),
    (25.1952,	24.738,	  24.8771,	24.93676667,    None,               None,               None),
    (25.066,	24.7678,	24.9616,	24.93180000,    None,               None,               None),
    (25.2151,	24.897,	  25.1753,	25.09580000,    None,               None,               None),
    (25.3741,	24.9268,	25.066, 	25.12230000,    None,               None,               None),
    (25.3642,	24.9567,	25.2747,	25.19853333,    None,               None,               None),
    (25.2648,	24.9268,	24.9964,	25.06266667,	  Some(24.21090333),	Some(0.554994333),	Some(102.3149586)),
    (24.8175,	24.2112,	24.4597,	24.49613333,	  Some(24.23657833),	Some(0.562977333),	Some(30.73599174)),
    (24.4398,	24.2112,	24.2808,	24.31060000,	  Some(24.25629),	    Some(0.552639),	    Some(6.551594561)),
    (24.6485,	24.4299,	24.6237,	24.56736667,	  Some(24.2953),	    Some(0.544736667),	Some(33.29641437)),
    (24.8374,	24.4398,	24.5815,	24.61956667,	  Some(24.34255167),	Some(0.528381667),	Some(34.95137669)),
    (24.7479,	24.2013,	24.5268,	24.49200000,	  Some(24.39005167),	Some(0.4910765),	  Some(13.84011566)),
    (24.5094,	24.251,	  24.3504,	24.37026667,	  Some(24.44049167),	Some(0.4355925),	  Some(-10.74781284)),
    (24.6784,	24.2112,	24.3404,	24.41000000,	  Some(24.47842667),	Some(0.3938640),	  Some(-11.58211407)),
    (24.6684,	24.1516,	24.2311,	24.35036667,	  Some(24.50990167),	Some(0.3624085),	  Some(-29.34717775)),
    (23.8435,	23.6348,	23.764,	  23.74743333,	  Some(24.48903),	    Some(0.382200333),	Some(-129.3556637)),
    (24.3007,	23.764,	  24.2013,	24.08866667,	  Some(24.49781),	    Some(0.373291),	    Some(-73.06959509))
  )

  val lookBackPeriod = 20     // look-back period specific to the data above

  case class Data(tick: Tick, commodityChannelIndex: CommodityChannelIndex) extends CommodityChannelIndexData

  "CommodityChannelIndex" should "produce the correct figures" in {
    var prevData = List.empty[Data]

    forAll(testData) {(high: Double, low: Double, close: Double, typicalPrice: Double, smaOfTP: Option[Double], cciMeanDeviation: Option[Double], cci: Option[Double]) =>
      val tick = Tick.getNext(close, Range(high, low), 0.0, 0.0, prevData)
      val commodityChannelIndex = CommodityChannelIndex.getNext(tick, prevData, period = lookBackPeriod)
      tick.typicalPrice should be (typicalPrice +- 0.0000001)
      matchOptionDouble(commodityChannelIndex.smaOfTypicalPrice, smaOfTP, 0.0000001)
      matchOptionDouble(commodityChannelIndex.meanDeviation, cciMeanDeviation, 0.0000001)
      matchOptionDouble(commodityChannelIndex.index, cci, 0.0000001)
      prevData = Data(tick, commodityChannelIndex) :: prevData
    }
  }
}
