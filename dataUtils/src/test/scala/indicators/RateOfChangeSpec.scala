package indicators

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

class RateOfChangeSpec extends FlatSpec with Matchers with TestHelpers {
  val testData = Table(
    ("close",   "roc"),
    (11045.27,  None),
    (11167.32,  None),
    (11008.61,  None),
    (11151.83,  None),
    (10926.77,  None),
    (10868.12,  None),
    (10520.32,  None),
    (10380.43,  None),
    (10785.14,  None),
    (10748.26,  None),
    (10896.91,  None),
    (10782.95,  None),
    (10620.16,  Some(-3.85)),
    (10625.83,  Some(-4.85)),
    (10510.95,  Some(-4.52)),
    (10444.37,  Some(-6.34)),
    (10068.01,  Some(-7.86)),
    (10193.39,  Some(-6.21)),
    (10066.57,  Some(-4.31)),
    (10043.75,  Some(-3.24))
  )

  case class Data(tick: Tick, rateOfChange: Option[Double]) extends RateOfChangeData

  "RateOfChange" should "produce the correct figures" in {
    var prevData = List.empty[Data]

    forAll(testData) {(close: Double, roc: Option[Double]) =>
      val tick = Tick.getNext(close, Range(0, 0), 0.0, 0.0, prevData)
      val rateOfChange = RateOfChange.getNext(tick, prevData)
      matchOptionDouble(rateOfChange, roc, 0.01)
      prevData = Data(tick, rateOfChange) :: prevData
    }
  }
}

