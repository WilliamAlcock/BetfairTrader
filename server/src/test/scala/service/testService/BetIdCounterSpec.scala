package service.testService

import org.scalatest._

/**
 * Created by Alcock on 27/10/2015.
 */

class BetIdCounterSpec extends FlatSpec with ShouldMatchers with BeforeAndAfterEach{

  var betIdCounter: BetIdCounter = _

  override def beforeEach {
    betIdCounter = new BetIdCounter()
  }

  "BetIdCounter" should "start at 1" in {
    betIdCounter.getNextBetId() should be("1")
  }

  "BetIdCounter" should "increment by 1" in {
    val x = betIdCounter.getNextBetId()
    val y = betIdCounter.getNextBetId()

    y should be((x.toInt + 1).toString)
  }
}
