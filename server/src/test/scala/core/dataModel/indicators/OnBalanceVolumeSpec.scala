package core.dataModel.indicators

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{Matchers, FlatSpec}

class OnBalanceVolumeSpec extends FlatSpec with Matchers {
  val testData = Table(
    ("close", "volume",   "obv"),

    (53.26,   0.0,        0.0),
    (53.30,   8200.0,     8200.0),
    (53.32,   8100.0,     16300.0),
    (53.72,   8300.0,     24600.0),
    (54.19,   8900.0,     33500.0),
    (53.92,   9200.0,     24300.0),
    (54.65,   13300.0,    37600.0),
    (54.60,   10300.0,    27300.0),
    (54.21,   9900.0,     17400.0),
    (54.53,   10100.0,    27500.0),
    (53.79,   11300.0,    16200.0),
    (53.66,   12600.0,    3600.0),
    (53.56,   10700.0,    -7100.0),
    (53.57,   11500.0,    4400.0),
    (53.94,   23800.0,    28200.0),
    (53.27,   14600.0,    13600.0),
    (53.60,   11700.0,    25300.0),
    (53.65,   10400.0,    35700.0),
    (53.62,   9500.0,     26200.0),
    (53.27,   13900.0,    12300.0),
    (53.44,   10400.0,    22700.0),
    (53.28,   4200.0,     18500.0),
    (53.14,   10900.0,    7600.0),
    (53.31,   17600.0,    25200.0),
    (54.09,   17900.0,    43100.0),
    (54.08,   15800.0,    27300.0),
    (54.01,   10200.0,    17100.0),
    (54.17,   8700.0,     25800.0),
    (54.24,   16000.0,    41800.0),
    (54.45,   12500.0,    54300.0)
  )

  "OnBalanceVolume" should "produce the correct figures" in {
    var prevData = List.empty[TickData]

    forAll(testData) {(close: Double, volume: Double, _obv: Double) =>
      val data = TickData.getNextTick(Range(0, 0), close, volume, prevData, onBalanceVolume = true)
      data.onBalanceVolume match {
        case Some(x) => x.obv should be(_obv +- 0.1)
        case None => fail()
      }
      prevData = data :: prevData
    }
  }
}