package indicators

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

class WilliamsPercentRSpec extends FlatSpec with Matchers with TestHelpers {
  val testData = Table(
    ("high",    "low",    "close",  "highestHigh",   "lowestLow",     "williamsPercentR"),
    (127.009,	  125.3574, 0.0,      None,            None,            None),
    (127.6159,	126.1633, 0.0,      None,            None,            None),
    (126.5911,	124.9296, 0.0,      None,            None,            None),
    (127.3472,	126.0937, 0.0,      None,            None,            None),
    (128.173,	  126.8199, 0.0,      None,            None,            None),
    (128.4317,	126.4817, 0.0,      None,            None,            None),
    (127.3671,	126.034,  0.0,      None,            None,            None),
    (126.422,	  124.8301, 0.0,      None,            None,            None),
    (126.8995,	126.3921, 0.0,      None,            None,            None),
    (126.8498,	125.7156, 0.0,      None,            None,            None),
    (125.646,	  124.5615, 0.0,      None,            None,            None),
    (125.7156,	124.5715, 0.0,      None,            None,            None),
    (127.1582,	125.0689, 0.0,      None,            None,            None),
    (127.7154,	126.8597,	127.2876, Some(128.4317),	 Some(124.5615),  Some(-29.56177975)),
    (127.6855,	126.6309,	127.1781, Some(128.4317),	 Some(124.5615),  Some(-32.3910909)),
    (128.2228,	126.8001,	128.0138, Some(128.4317),	 Some(124.5615),  Some(-10.79789158)),
    (128.2725,	126.7105,	127.1085, Some(128.4317),	 Some(124.5615),  Some(-34.18944757)),
    (128.0934,	126.8001,	127.7253, Some(128.4317),	 Some(124.5615),  Some(-18.2522867)),
    (128.2725,	126.1335,	127.0587, Some(128.4317),	 Some(124.5615),  Some(-35.47620278)),
    (127.7353,	125.9245,	127.3273, Some(128.2725),	 Some(124.5615),  Some(-25.47022366)),
    (128.77,	  126.9891,	128.7103, Some(128.77),	   Some(124.5615),  Some(-1.418557681)),
    (129.2873,	127.8148,	127.8745, Some(129.2873),	 Some(124.5615),  Some(-29.89546743)),
    (130.0633,	128.4715,	128.5809, Some(130.0633),	 Some(124.5615),  Some(-26.94390927)),
    (129.1182,	128.0641,	128.6008, Some(130.0633),	 Some(124.5615),  Some(-26.58220946)),
    (129.2873,	127.6059,	127.9342, Some(130.0633),	 Some(124.5715),  Some(-38.76870971)),
    (128.4715,	127.596,	128.1133, Some(130.0633),	 Some(125.0689),  Some(-39.04372898)),
    (128.0934,	126.999,	127.596,  Some(130.0633),	 Some(125.9245),  Some(-59.61389775)),
    (128.6506,	126.8995,	127.596,  Some(130.0633),	 Some(125.9245),  Some(-59.61389775)),
    (129.1381,	127.4865,	128.6904, Some(130.0633),	 Some(125.9245),  Some(-33.17145066)),
    (128.6406,	127.397,	128.2725, Some(130.0633),	 Some(125.9245),  Some(-43.26858026))
  )

  case class Data(tick: Tick, williamsPercentR: WilliamsPercentR) extends WilliamsPercentRData

  "WilliamsPercentR" should "produce the correct results" in {
    var prevData = List.empty[Data]

    forAll(testData) {(high: Double,
                       low: Double,
                       close: Double,
                       highestHigh: Option[Double],
                       lowestLow: Option[Double],
                       _williamsPercentR: Option[Double]) =>
      val tick = Tick.getNext(close, Range(high, low), 0.0, 0.0, prevData)
      val williamsPercentR = WilliamsPercentR.getNext(tick, prevData)
      matchOptionDouble(williamsPercentR.highestHigh, highestHigh, 0.0001)
      matchOptionDouble(williamsPercentR.lowestLow, lowestLow, 0.0001)
      matchOptionDouble(williamsPercentR.percentR, _williamsPercentR, 0.0000001)
      prevData = Data(tick, williamsPercentR) :: prevData
    }
  }
}
