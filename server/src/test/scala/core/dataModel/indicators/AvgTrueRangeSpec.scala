package core.dataModel.indicators

import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.prop.TableDrivenPropertyChecks._

class AvgTrueRangeSpec extends FlatSpec with Matchers {
  val testData = Table(
    ("high",  "low",    "close",  "trueRange", "avgTrueRange"),
    (48.7,	  47.79,	  48.16,    0.91,        None),
    (48.72,	  48.14,	  48.61,    0.58,        None),
    (48.9,	  48.39,	  48.75,    0.51,        None),
    (48.87,	  48.37,	  48.63,    0.5,         None),
    (48.82,	  48.24,	  48.74,    0.58,        None),
    (49.05,	  48.635,	  49.03,    0.415,       None),
    (49.2,	  48.94,	  49.07,    0.26,        None),
    (49.35,	  48.86,	  49.32,    0.49,        None),
    (49.92,	  49.5,	    49.91,    0.6,         None),
    (50.19,	  49.87,	  50.13,    0.32,        None),
    (50.12,	  49.2,	    49.53,    0.93,        None),
    (49.66,	  48.9,	    49.5,     0.76,        None),
    (49.88,	  49.43,	  49.75,    0.45,        None),
    (50.19,	  49.725,	  50.03,    0.465,	     Some(0.555000000)),
    (50.36,	  49.26,	  50.31,    1.1,	       Some(0.593928571)),
    (50.57,	  50.09,	  50.52,    0.48,	       Some(0.585790816)),
    (50.65,	  50.3,	    50.41,    0.35,	       Some(0.568948615)),
    (50.43,	  49.21,	  49.34,    1.22,	       Some(0.615452286)),
    (49.63,	  48.98,	  49.37,    0.65,	       Some(0.617919979)),
    (50.33,	  49.61,	  50.23,    0.96,	       Some(0.642354267)),
    (50.29,	  49.2,	    49.2375,  1.09,	       Some(0.674328962)),
    (50.17,	  49.43,	  49.93,    0.9325,	     Some(0.69276975)),
    (49.32,	  48.08,	  48.43,    1.85,	       Some(0.775429054)),
    (48.5,	  47.64,	  48.18,    0.86,	       Some(0.781469836)),
    (48.3201,	41.55,	  46.57,    6.7701,	     Some(1.209229133)),
    (46.8,	  44.2833,	45.41,    2.5167,	     Some(1.302619909)),
    (47.8,	  47.31,	  47.77,    2.39,	       Some(1.380289916)),
    (48.39,	  47.2,	    47.72,    1.19,	       Some(1.366697779)),
    (48.66,	  47.9,	    48.62,    0.94,	       Some(1.336219366)),
    (48.79,	  47.7301,	47.85,    1.0599,	     Some(1.316482269))
  )


  "AvgTrueRange" should "produce the correct figures" in {
    var prevData = List.empty[TickData]

    forAll(testData) { (high: Double, low: Double, close: Double, trueRange: Double, _avgTrueRange: Option[Double]) =>
      val data = TickData.getNextTick(Range(high, low), close, 0.0, prevData, avgTrueRange = true)
      data.avgTrueRange match {
        case Some(x) =>
          x.trueRange should be(trueRange +- 0.01)
          TestHelpers.matchOptionDouble(x.avg, _avgTrueRange, 0.000000001)
        case None => fail()
      }
      prevData = data :: prevData
    }
  }
}