package indicators

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

class EaseOfMovementSpec extends FlatSpec with Matchers with TestHelpers {
  val period = 14 // 14 period SMA of EMV

  val testData = Table(
    ("high", "low", "volume",   "emv",              "smaOfEMV"),
    (63.74,	 62.63,	32178836.0, None,               None),
    (64.51,	 63.85,	36461672.0,	Some(1.801069353),  None),
    (64.57,	 63.81,	51372680.0,	Some(0.014793855),  None),
    (64.31,	 62.62,	42476356.0,	Some(-2.884545934), None),
    (63.43,	 62.73,	29504176.0,	Some(-0.913430017), None),
    (62.85,	 61.95,	33098600.0,	Some(-1.849020805), None),
    (62.7,	 62.06,	30577960.0,	Some(-0.041860216), None),
    (63.18,	 62.69,	35693928.0,	Some(0.761894292),  None),
    (62.47,	 61.54,	49768136.0,	Some(-1.737858938), None),
    (64.16,	 63.21,	44759968.0,	Some(3.565686195),  None),
    (64.38,	 63.87,	33425504.0,	Some(0.671343654),  None),
    (64.89,	 64.29,	15895085.0,	Some(1.755259566),  None),
    (65.25,	 64.48,	37015388.0,	Some(0.572059382),  None),
    (64.69,	 63.65,	40672116.0,	Some(-1.777138913), None),
    (64.26,	 63.68,	35627200.0,	Some(-0.325593928),	Some(-0.027667318)),
    (64.51,	 63.12,	47337336.0,	Some(-0.455137568),	Some(-0.188824955)),
    (63.46,	 62.5, 	43373576.0,	Some(-1.848129838),	Some(-0.321890933)),
    (62.69,	 61.86,	57651752.0,	Some(-1.014973491),	Some(-0.188350045)),
    (63.52,	 62.56,	32357184.0,	Some(2.269665988),	Some(0.039013956)),
    (63.52,	 62.95,	27620876.0,	Some(0.402413015),	Some(0.199830657)),
    (63.74,	 62.63,	42467704.0,	Some(-0.130687546),	Some(0.193485848)),
    (64.58,	 63.39,	44460240.0,	Some(2.141239004),	Some(0.29201047)),
    (65.31,	 64.72,	52992592.0,	Some(1.146764061),	Some(0.49805497)),
    (65.1,	 64.21,	40561552.0,	Some(-0.789910603),	Some(0.186940913)),
    (63.68,	 62.51,	48636228.0,	Some(-3.752758129),	Some(-0.129066357)),
    (63.66,	 62.55,	57230032.0,	Some(0.019395411),	Some(-0.253056654)),
    (62.95,	 62.15,	46260856.0,	Some(-0.959774718),	Some(-0.362473375)),
    (63.73,	 62.97,	41926492.0,	Some(1.450157099),	Some(-0.131952232)),
    (64.99,	 63.64,	42620976.0,	Some(3.056593542),	Some(0.109632588)),
    (65.31,	 64.6, 	37809176.0,	Some(1.201824658),	Some(0.227987032))
  )

  case class Data(tick: Tick, easeOfMovement: EaseOfMovement) extends EaseOfMovementData

  "EaseOfMovement" should "produce the correct figures" in {
    var prevData = List.empty[Data]

    forAll(testData) {(high: Double, low: Double, volume: Double, _emv: Option[Double], smaOfEMV: Option[Double]) =>
      val tick = Tick.getNext(0.0, Range(high, low), volume, 0.0, prevData)
      val easeOfMovement = EaseOfMovement.getNext(tick, prevData)
      matchOptionDouble(easeOfMovement.emv, _emv, 0.000000001)
      matchOptionDouble(easeOfMovement.smaOfEMV, smaOfEMV, 0.000000001)
      prevData = Data(tick, easeOfMovement) :: prevData
    }
  }
}
