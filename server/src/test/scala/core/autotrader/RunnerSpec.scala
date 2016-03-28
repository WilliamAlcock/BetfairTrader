package core.autotrader

import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import core.autotrader.Runner.{Idle, NoData}
import domain.PlaceInstruction
import domain.Side._
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}

class RunnerSpec extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(""))) with FlatSpecLike with Matchers with MockFactory with BeforeAndAfterEach {

  var mockController : TestProbe = _

  override def beforeEach(): Unit = {
    mockController = TestProbe()

  }

  "mockRunner" should "start in Idle state with NoData" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))
    mockRunner.stateName should be (Idle)
    mockRunner.stateData should be (NoData)
  }

  "mockRunner" should "transition to Initialised when Init(strategy) received" in {
    val mockRunner = TestFSMRef(new Runner(mockController.ref))

    val mockStrategy = Strategy(
      "TEST_MARKET_ID",
      1L,
      None,
      instructions: List[PlaceInstruction],
      orders: List[OrderData] = List.empty,
      stopPrice: Double,
      stopSide: Side,
      startTime: Option[DateTime],
      stopTime: Option[DateTime]) extends Data
    )

    mockRunner.tell(Init())
  }

}

