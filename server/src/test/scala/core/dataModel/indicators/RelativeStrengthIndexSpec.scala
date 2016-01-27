package core.dataModel.indicators

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

class RelativeStrengthIndexSpec extends FlatSpec with Matchers {
  val testData = Table(
    ("close", "closeDelta",   "gain",       "loss",         "avgGain",          "avgLoss",          "rs",               "rsi"),

    (44.3389, None,           None,         None,           None,               None,               None,               None),
    (44.0902,	Some(-0.2487),	Some(0.0),	  Some(0.2487),   None,               None,               None,               None),
    (44.1497,	Some(0.0595),	  Some(0.0595), Some(0.0),      None,               None,               None,               None),
    (43.6124,	Some(-0.5373),  Some(0.0),    Some(0.5373),   None,               None,               None,               None),
    (44.3278,	Some(0.7154), 	Some(0.7154), Some(0.0),      None,               None,               None,               None),
    (44.8264,	Some(0.4986),	  Some(0.4986), Some(0.0),      None,               None,               None,               None),
    (45.0955,	Some(0.2691), 	Some(0.2691), Some(0.0),      None,               None,               None,               None),
    (45.4245,	Some(0.329),  	Some(0.329),  Some(0.0),      None,               None,               None,               None),
    (45.8433,	Some(0.4188), 	Some(0.4188), Some(0.0),      None,               None,               None,               None),
    (46.0826,	Some(0.2393), 	Some(0.2393), Some(0.0),      None,               None,               None,               None),
    (45.8931,	Some(-0.1895),  Some(0.0),		Some(0.1895),   None,               None,               None,               None),
    (46.0328,	Some(0.1397),   Some(0.1397), Some(0.0),      None,               None,               None,               None),
    (45.614,	Some(-0.4188),  Some(0.0),  	Some(0.4188),   None,               None,               None,               None),
    (46.282,	Some(0.668),  	Some(0.668),  Some(0.0),		  None,               None,               None,               None),
    (46.282,	Some(0.0),      Some(0.0),    Some(0.0),			Some(0.238385714),	Some(0.099592857),	Some(2.393602525),	Some(70.53278948)),
    (46.0028,	Some(-0.2792),  Some(0.0),		Some(0.2792),	  Some(0.221358163),	Some(0.112421939),	Some(1.968994359),	Some(66.31856181)),
    (46.0328,	Some(0.03),   	Some(0.03),   Some(0.0),  		Some(0.207689723),	Some(0.1043918),	  Some(1.989521423),	Some(66.54982994)),
    (46.4116,	Some(0.3788), 	Some(0.3788), Some(0.0),		  Some(0.219911886),	Some(0.096935243),	Some(2.268647383),	Some(69.40630534)),
    (46.2222,	Some(-0.1894),  Some(0.0),  	Some(0.1894), 	Some(0.204203894),	Some(0.103539869),	Some(1.972224773),	Some(66.35516906)),
    (45.6439,	Some(-0.5783),  Some(0.0),		Some(0.5783), 	Some(0.189617901),	Some(0.137451307),	Some(1.379527821),	Some(57.97485571)),
    (46.2122,	Some(0.5683),	  Some(0.5683), Some(0.0),		  Some(0.216666623),	Some(0.127633356),	Some(1.69757052),	  Some(62.92960675)),
    (46.2521,	Some(0.0399),	  Some(0.0399), Some(0.0),		  Some(0.204040435),	Some(0.118516688),	Some(1.721617767),	Some(63.25714756)),
    (45.7137,	Some(-0.5384),  Some(0.0),		Some(0.5384), 	Some(0.189466119),	Some(0.148508353),	Some(1.275794356),	Some(56.05929872)),
    (46.4515,	Some(0.7378), 	Some(0.7378), Some(0.0),  		Some(0.228632824),	Some(0.137900613),	Some(1.657953643),	Some(62.37707144)),
    (45.7835,	Some(-0.668),	  Some(0.0),    Some(0.668),  	Some(0.212301908),	Some(0.175764855),	Some(1.207874623),	Some(54.70757308)),
    (45.3548,	Some(-0.4287),	Some(0.0),    Some(0.4287), 	Some(0.197137486),	Some(0.193831651),	Some(1.017055186),	Some(50.42277441)),
    (44.0288,	Some(-1.326),	  Some(0.0),    Some(1.326),  	Some(0.183056237),	Some(0.274700819),	Some(0.666384024),	Some(39.98982315)),
    (44.1783,	Some(0.1495),	  Some(0.1495), Some(0.0),  		Some(0.180659363),	Some(0.255079332),	Some(0.708247751),	Some(41.46048198)),
    (44.2181,	Some(0.0398),	  Some(0.0398),	Some(0.0),    	Some(0.17059798),	  Some(0.23685938),	  Some(0.72025005),	  Some(41.86891609)),
    (44.5672,	Some(0.3491),	  Some(0.3491),	Some(0.0),      Some(0.183348124),	Some(0.219940853),	Some(0.833624687),	Some(45.46321245)),
    (43.4205,	Some(-1.1467),	Some(0.0),    Some(1.1467),	  Some(0.17025183),	  Some(0.286137935),	Some(0.594999157),	Some(37.30404209)),
    (42.6628,	Some(-0.7577),	Some(0.0),    Some(0.7577),	  Some(0.158090985),	Some(0.319820939),	Some(0.494310927),	Some(33.07952299)),
    (43.1314,	Some(0.4686), 	Some(0.4686), Some(0.0),		  Some(0.1802702),	  Some(0.296976586),	Some(0.607018224),	Some(37.77295211))
  )

  "RelativeStrengthIndex" should "produce the correct figures" in {
    var prevData = List.empty[TickData]

    forAll(testData) { (close: Double,
                        closeDelta: Option[Double],
                        gain: Option[Double],
                        loss: Option[Double],
                        avgGain: Option[Double],
                        avgLoss: Option[Double],
                        rs: Option[Double],
                        _rsi: Option[Double]) =>

      val data = TickData.getNextTick(Range(0, 0), close, 0.0, prevData, relativeStrengthIndex = true)

      TestHelpers.matchOptionDouble(data.closeDelta, closeDelta, 0.0001)
      data.relativeStrengthIndex match {
        case Some(x) =>
          TestHelpers.matchOptionDouble(x.gain, gain, 0.0001)
          TestHelpers.matchOptionDouble(x.loss, loss, 0.0001)
          TestHelpers.matchOptionDouble(x.avgGain, avgGain, 0.000000001)
          TestHelpers.matchOptionDouble(x.avgLoss, avgLoss, 0.000000001)
          TestHelpers.matchOptionDouble(x.rs, rs, 0.000000001)
          TestHelpers.matchOptionDouble(x.rsi, _rsi, 0.00000001)
        case None => fail()
      }
      prevData = data :: prevData
    }
  }
}

