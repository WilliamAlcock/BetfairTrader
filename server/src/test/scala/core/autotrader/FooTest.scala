package core.autotrader

import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpecLike, Matchers}

class StashTestSpec extends TestKit(ActorSystem("TestSystem", ConfigFactory.parseString(""))) with FlatSpecLike with Matchers {

  "StashTest" should "stash messages when in idle and unstash when in Running" in {
    val stashTest = TestFSMRef(new FooTest("a test"))

    val sender = TestProbe()

    sender.send(stashTest, Message)
    sender.send(stashTest, Message)
    sender.send(stashTest, Message)
    sender.send(stashTest, Message)

    sender.send(stashTest, Start)

    stashTest.stateName should be (Running)
    stashTest.stateData should be (GotData)
  }
}