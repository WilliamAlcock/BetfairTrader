package indicators

import org.scalatest.Matchers

trait TestHelpers extends Matchers {
  def matchOptionDouble(a: Option[Double], b: Option[Double], precision: Double = 0.0) = (a, b) match {
    case (Some(x), Some(y)) => x should be (y +- precision)
    case _ => (a, b) should be ((None, None))
  }
}

