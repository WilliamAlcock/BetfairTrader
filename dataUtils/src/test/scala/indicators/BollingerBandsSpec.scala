package indicators

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

class BollingerBandsSpec  extends FlatSpec with Matchers with TestHelpers {
  val testData = Table(
    ("close", "middle",         "stdDev",           "upper",            "lower",            "bandWidth"),
    (86.1557, None,             None,               None,               None,               None),
    (89.0867, None,             None,               None,               None,               None),
    (88.7829, None,             None,               None,               None,               None),
    (90.3228, None,             None,               None,               None,               None),
    (89.0671, None,             None,               None,               None,               None),
    (91.1453, None,             None,               None,               None,               None),
    (89.4397, None,             None,               None,               None,               None),
    (89.175,  None,             None,               None,               None,               None),
    (86.9302, None,             None,               None,               None,               None),
    (87.6752, None,             None,               None,               None,               None),
    (86.9596, None,             None,               None,               None,               None),
    (89.4299, None,             None,               None,               None,               None),
    (89.3221, None,             None,               None,               None,               None),
    (88.7241, None,             None,               None,               None,               None),
    (87.4497, None,             None,               None,               None,               None),
    (87.2634, None,             None,               None,               None,               None),
    (89.4985, None,             None,               None,               None,               None),
    (87.9006, None,             None,               None,               None,               None),
    (89.126,  None,             None,               None,               None,               None),
    (90.7043, Some(88.707940),	Some(1.291961214),	Some(91.29186243),	Some(86.12401757),	Some(5.167844856)),
    (92.9001,	Some(89.045160),	Some(1.452053812),	Some(91.94926762),	Some(86.14105238),	Some(5.808215247)),
    (92.9784,	Some(89.239745),	Some(1.686425070),	Some(92.61259514),	Some(85.86689486),	Some(6.745700280)),
    (91.8021,	Some(89.390705),	Some(1.771747269),	Some(92.93419954),	Some(85.84721046),	Some(7.086989075)),
    (92.6647,	Some(89.507800),	Some(1.902074986),	Some(93.31194997),	Some(85.70365003),	Some(7.608299946)),
    (92.6843,	Some(89.688660),	Some(2.019894973),	Some(93.72844995),	Some(85.64887005),	Some(8.079579892)),
    (92.3021,	Some(89.746500),	Some(2.076546090),	Some(93.89959218),	Some(85.59340782),	Some(8.306184360)),
    (92.7725,	Some(89.913140),	Some(2.176557434),	Some(94.26625487),	Some(85.56002513),	Some(8.706229735)),
    (92.5373,	Some(90.081255),	Some(2.241920574),	Some(94.56509615),	Some(85.59741385),	Some(8.967682297)),
    (92.949,	Some(90.382195),	Some(2.202358660),	Some(94.78691232),	Some(85.97747768),	Some(8.809434639)),
    (93.2039,	Some(90.658630),	Some(2.192185489),	Some(95.04300098),	Some(86.27425902),	Some(8.768741954)),
    (91.0669,	Some(90.863995),	Some(2.021805012),	Some(94.90760502),	Some(86.82038498),	Some(8.087220047)),
    (89.8318,	Some(90.884090),	Some(2.009410759),	Some(94.90291152),	Some(86.86526848),	Some(8.037643037)),
    (89.7435,	Some(90.905160),	Some(1.995080022),	Some(94.89532004),	Some(86.91499996),	Some(7.980320087)),
    (90.3994,	Some(90.988925),	Some(1.936043967),	Some(94.86101293),	Some(87.11683707),	Some(7.744175867)),
    (90.7387,	Some(91.153375),	Some(1.760127094),	Some(94.67362919),	Some(87.63312081),	Some(7.040508375)),
    (88.0177,	Some(91.191090),	Some(1.682751489),	Some(94.55659298),	Some(87.82558702),	Some(6.731005957)),
    (88.0867,	Some(91.120500),	Some(1.779125753),	Some(94.67875151),	Some(87.56224849),	Some(7.116503012)),
    (88.8439,	Some(91.167665),	Some(1.704060294),	Some(94.57578559),	Some(87.75954441),	Some(6.816241176)),
    (90.7781,	Some(91.250270),	Some(1.642000653),	Some(94.53427131),	Some(87.96626869),	Some(6.568002613)),
    (90.5416,	Some(91.242135),	Some(1.645085549),	Some(94.53230610),	Some(87.95196390),	Some(6.580342196)),
    (91.3894,	Some(91.166600),	Some(1.601325351),	Some(94.36925070),	Some(87.96394930),	Some(6.405301403)),
    (90.65,	  Some(91.050180),	Some(1.549161734),	Some(94.14850347),	Some(87.95185653),	Some(6.196646937))
  )

  case class Data(tick: Tick, bollingerBands: BollingerBands) extends BollingerBandData

  "BollingerBands" should "produce the correct figures" in {
    var prevData = List.empty[Data]

    forAll(testData) { (close: Double, middle: Option[Double], stdDev: Option[Double], upper: Option[Double], lower: Option[Double], bandWidth: Option[Double]) =>
      val tick = Tick.getNext(close, Range(0, 0), 0.0, 0.0, prevData)
      val bollingerBands = BollingerBands.getNext(tick, prevData)
      matchOptionDouble(bollingerBands.middle, middle, 0.000001)
      matchOptionDouble(bollingerBands.stdDev, stdDev, 0.000000001)
      matchOptionDouble(bollingerBands.upper, upper, 0.00000001)
      matchOptionDouble(bollingerBands.lower, lower, 0.00000001)
      matchOptionDouble(bollingerBands.bandWidth, bandWidth, 0.000000001)
      prevData = Data(tick, bollingerBands) :: prevData
    }
  }
}

