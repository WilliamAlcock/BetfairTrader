package indicators

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{Matchers, FlatSpec}

class MACDSpec extends FlatSpec with Matchers with TestHelpers {
  val testData = Table(
    ("Close",	"12 Day EMA",	      "26 Day EMA",   	  "MACD",	            "Signal",	          "Histogram"),
    (459.99,  None,               None,               None,               None,               None),
    (448.85,  None,               None,               None,               None,               None),
    (446.06,  None,               None,               None,               None,               None),
    (450.81,  None,               None,               None,               None,               None),
    (442.8,   None,               None,               None,               None,               None),
    (448.97,  None,               None,               None,               None,               None),
    (444.57,  None,               None,               None,               None,               None),
    (441.4,   None,               None,               None,               None,               None),
    (430.47,  None,               None,               None,               None,               None),
    (420.05,  None,               None,               None,               None,               None),
    (431.14,  None,               None,               None,               None,               None),
    (425.66,	Some(440.8975),     None,               None,               None,               None),
    (430.58,	Some(439.3101923),  None,               None,               None,               None),
    (431.72,	Some(438.1424704),  None,               None,               None,               None),
    (437.87,	Some(438.1005519),  None,               None,               None,               None),
    (428.43,	Some(436.6127747),  None,               None,               None,               None),
    (428.35,	Some(435.3415786),  None,               None,               None,               None),
    (432.5,	  Some(434.9044126),  None,               None,               None,               None),
    (443.66,	Some(436.2514261),  None,               None,               None,               None),
    (455.72,	Some(439.2465913),  None,               None,               None,               None),
    (454.49,	Some(441.5917311),  None,               None,               None,               None),
    (452.08,	Some(443.2053109),  None,               None,               None,               None),
    (452.73,	Some(444.6706477),  None,               None,               None,               None),
    (461.91,	Some(447.3228558),  None,               None,               None,               None),
    (463.58,	Some(449.8239549),  None,               None,               None,               None),
    (461.14,	Some(451.5648849),	Some(443.2896154),	Some(8.275269504),  None,               None),
    (452.08,	Some(451.6441334),	Some(443.940755),	  Some(7.703378381),  None,               None),
    (442.66,	Some(450.261959),	  Some(443.8458842),	Some(6.416074757),  None,               None),
    (428.91,	Some(446.9770422),	Some(442.7395225),	Some(4.237519783),  None,               None),
    (429.79,	Some(444.3328819),	Some(441.7802986),	Some(2.552583325),  None,               None),
    (431.99,	Some(442.433977),	  Some(441.0550913),	Some(1.37888572),   None,               None),
    (427.72,	Some(440.1702882),	Some(440.0673067),	Some(0.102981491),  None,               None),
    (423.2,	  Some(437.5594746),	Some(438.8178766),	Some(-1.258401953), None,               None),
    (426.21,	Some(435.8134016),	Some(437.8839598),	Some(-2.07055819),	Some(3.037525869),	Some(-5.108084059)),
    (426.98,	Some(434.4544168),	Some(437.0762591),	Some(-2.621842328),	Some(1.905652229),	Some(-4.527494558)),
    (435.69,	Some(434.6445065),	Some(436.9735732),	Some(-2.32906674),	Some(1.058708435),	Some(-3.387775176)),
    (434.33,	Some(434.5961209),	Some(436.777753),	  Some(-2.181632115),	Some(0.410640325),	Some(-2.59227244)),
    (429.8,	  Some(433.8582561),	Some(436.2608824),	Some(-2.402626273),	Some(-0.152012994),	Some(-2.250613279)),
    (419.85,	Some(431.7031398),	Some(435.0452615),	Some(-3.342121681),	Some(-0.790034732),	Some(-2.55208695)),
    (426.24,	Some(430.8626568),	Some(434.3930199),	Some(-3.530363136),	Some(-1.338100413),	Some(-2.192262723)),
    (402.8,	  Some(426.5453249),	Some(432.0527962),	Some(-5.507471249),	Some(-2.17197458),	Some(-3.335496669)),
    (392.05,	Some(421.2383519),	Some(429.0896261),	Some(-7.851274229),	Some(-3.30783451),	Some(-4.543439719)),
    (390.53,	Some(416.51399),	  Some(426.2333575),	Some(-9.719367455),	Some(-4.590141099),	Some(-5.129226357)),
    (398.67,	Some(413.7687608),	Some(424.1916273),	Some(-10.42286651),	Some(-5.756686181),	Some(-4.666180327)),
    (406.13,	Some(412.5935668),	Some(422.853729),	  Some(-10.26016216),	Some(-6.657381376),	Some(-3.602780783)),
    (405.46,	Some(411.496095),	  Some(421.5653046),	Some(-10.06920961),	Some(-7.339747023),	Some(-2.729462587)),
    (408.38,	Some(411.0166958),	Some(420.5886154),	Some(-9.571919612),	Some(-7.786181541),	Some(-1.785738071)),
    (417.2,	  Some(411.9679734),	Some(420.3376068),	Some(-8.369633492),	Some(-7.902871931),	Some(-0.466761561)),
    (430.12,	Some(414.7605928),	Some(421.0622286),	Some(-6.301635724),	Some(-7.58262469),	Some(1.280988966)),
    (442.78,	Some(419.0712709),	Some(422.6709524),	Some(-3.599681509),	Some(-6.786036054),	Some(3.186354544)),
    (439.29,	Some(422.1818446),	Some(423.9019929),	Some(-1.720148361),	Some(-5.772858515),	Some(4.052710154)),
    (445.52,	Some(425.77233),	  Some(425.5033268),	Some(0.269003232),	Some(-4.564486166),	Some(4.833489398)),
    (449.98,	Some(429.4965869),	Some(427.3164137),	Some(2.180173247),	Some(-3.215554283),	Some(5.39572753)),
    (460.71,	Some(434.2986505),	Some(429.7900127),	Some(4.508637809),	Some(-1.670715865),	Some(6.179353673)),
    (458.66,	Some(438.0465504),	Some(431.9285303),	Some(6.118020154),	Some(-0.112968661),	Some(6.230988815)),
    (463.84,	Some(442.0147734),	Some(434.2923428),	Some(7.722430594),	Some(1.45411119),	  Some(6.268319404)),
    (456.77,	Some(444.2848083),	Some(435.9573545),	Some(8.327453809),	Some(2.828779714),	Some(5.498674095)),
    (452.97,	Some(445.6209916),	Some(437.2175504),	Some(8.403441185),	Some(3.943712008),	Some(4.459729177)),
    (454.74,	Some(447.023916),	  Some(438.5155097),	Some(8.508406323),	Some(4.856650871),	Some(3.651755452)),
    (443.86,	Some(446.5371597),	Some(438.9113978),	Some(7.625761844),	Some(5.410473066),	Some(2.215288778)),
    (428.85,	Some(443.8160582),	Some(438.1661091),	Some(5.649949083),	Some(5.458368269),	Some(0.191580814)),
    (434.58,	Some(442.3951262),	Some(437.9004714),	Some(4.494654765),	Some(5.265625568),	Some(-0.770970803)),
    (433.26,	Some(440.9897221),	Some(437.5567328),	Some(3.432989362),	Some(4.899098327),	Some(-1.466108965)),
    (442.93,	Some(441.2882264),	Some(437.9547526),	Some(3.333473854),	Some(4.585973432),	Some(-1.252499579)),
    (439.66,	Some(441.0377301),	Some(438.0810672),	Some(2.956662856),	Some(4.260111317),	Some(-1.303448461)),
    (441.35,	Some(441.0857716),	Some(438.3232104),	Some(2.762561216),	Some(3.960601297),	Some(-1.198040081))
  )

  case class Data(tick: Tick, macd: MACD) extends MACDData

  "MACD" should "produce the correct figures" in {
    var prevData = List.empty[Data]

    forAll(testData) { (close: Double, firstEMA: Option[Double], secondEMA: Option[Double], _macd: Option[Double], signal: Option[Double], histogram: Option[Double]) =>
      val tick = Tick.getNext(close, Range(0, 0), 0.0, 0.0, prevData)
      val macd = MACD.getNext(tick, prevData)
      matchOptionDouble(macd.firstEMA, firstEMA, 0.000001)
      matchOptionDouble(macd.secondEMA, secondEMA, 0.000001)
      matchOptionDouble(macd.line, _macd, 0.00000001)
      matchOptionDouble(macd.signalLine, signal, 0.00000001)
      matchOptionDouble(macd.histogram, histogram, 0.00000001)
      prevData = Data(tick, macd) :: prevData
    }
  }
}
