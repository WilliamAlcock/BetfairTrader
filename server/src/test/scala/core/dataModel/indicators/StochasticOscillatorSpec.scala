package core.dataModel.indicators

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._

class StochasticOscillatorSpec extends FlatSpec with Matchers {
  val testData = Table(
    ("high",    "low",    "highestHigh", "lowestLow",     "close", "stochasticOscillatorK", "stochasticOscillatorD"),
    (127.009,	  125.3574, None,           None,           0.0,      None,               None),
    (127.6159,	126.1633, None,           None,           0.0,      None,               None),
    (126.5911,	124.9296, None,           None,           0.0,      None,               None),
    (127.3472,	126.0937, None,           None,           0.0,      None,               None),
    (128.173,	  126.8199, None,           None,           0.0,      None,               None),
    (128.4317,	126.4817, None,           None,           0.0,      None,               None),
    (127.3671,	126.034,  None,           None,           0.0,      None,               None),
    (126.422,	  124.8301, None,           None,           0.0,      None,               None),
    (126.8995,	126.3921, None,           None,           0.0,      None,               None),
    (126.8498,	125.7156, None,           None,           0.0,      None,               None),
    (125.646,	  124.5615, None,           None,           0.0,      None,               None),
    (125.7156,	124.5715, None,           None,           0.0,      None,               None),
    (127.1582,	125.0689, None,           None,           0.0,      None,               None),
    (127.7154,	126.8597,	Some(128.4317),	Some(124.5615),	127.2876,	Some(70.43822025),  None),
    (127.6855,	126.6309,	Some(128.4317),	Some(124.5615),	127.1781,	Some(67.6089091),   None),
    (128.2228,	126.8001,	Some(128.4317),	Some(124.5615),	128.0138,	Some(89.20210842),	Some(75.74974592)),
    (128.2725,	126.7105,	Some(128.4317),	Some(124.5615),	127.1085,	Some(65.81055243),	Some(74.20718998)),
    (128.0934,	126.8001,	Some(128.4317),	Some(124.5615),	127.7253,	Some(81.7477133),	  Some(78.92012471)),
    (128.2725,	126.1335,	Some(128.4317),	Some(124.5615),	127.0587,	Some(64.52379722),	Some(70.69402098)),
    (127.7353,	125.9245,	Some(128.2725),	Some(124.5615),	127.3273,	Some(74.52977634),	Some(73.60042895)),
    (128.77,	  126.9891,	Some(128.77),	  Some(124.5615),	128.7103,	Some(98.58144232),	Some(79.21167196)),
    (129.2873,	127.8148,	Some(129.2873),	Some(124.5615),	127.8745,	Some(70.10453257),	Some(81.07191708)),
    (130.0633,	128.4715,	Some(130.0633),	Some(124.5615),	128.5809,	Some(73.05609073),	Some(80.58068854)),
    (129.1182,	128.0641,	Some(130.0633),	Some(124.5615),	128.6008,	Some(73.41779054),	Some(72.19280461)),
    (129.2873,	127.6059,	Some(130.0633),	Some(124.5715),	127.9342,	Some(61.23129029),	Some(69.23505719)),
    (128.4715,	127.596,	Some(130.0633),	Some(125.0689),	128.1133,	Some(60.95627102),	Some(65.20178395)),
    (128.0934,	126.999,	Some(130.0633),	Some(125.9245),	127.596,	Some(40.38610225),	Some(54.19122119)),
    (128.6506,	126.8995,	Some(130.0633),	Some(125.9245),	127.596,	Some(40.38610225),	Some(47.24282518)),
    (129.1381,	127.4865,	Some(130.0633),	Some(125.9245),	128.6904,	Some(66.82854934),	Some(49.20025128)),
    (128.6406,	127.397,	Some(130.0633),	Some(125.9245),	128.2725,	Some(56.73141974),	Some(54.64869044))
  )

  "StochasticOscillator" should "produce the correct figues" in {
    var prevData = List.empty[TickData]

    forAll(testData) {(high: Double,
                       low: Double,
                       highestHigh: Option[Double],
                       lowestLow: Option[Double],
                       close: Double,
                       stochasticOscillatorK: Option[Double],
                       stochasticOscillatorD: Option[Double]) =>
      val data = TickData.getNextTick(Range(high, low), close, 0.0, prevData, stochasticOscillator = true)
      data.stochasticOscillator match {
        case Some(x) =>
          TestHelpers.matchOptionDouble(x.highestHigh, highestHigh, 0.0001)
          TestHelpers.matchOptionDouble(x.lowestLow, lowestLow, 0.0001)
          TestHelpers.matchOptionDouble(x.percentK, stochasticOscillatorK, 0.00000001)
          TestHelpers.matchOptionDouble(x.percentD, stochasticOscillatorD, 0.00000001)
        case None => fail()
      }
      prevData = data :: prevData
    }
  }
}

