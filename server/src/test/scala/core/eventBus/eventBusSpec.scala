package core.eventBus

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps

class eventBusSpec extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(""))) with FlatSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    system.shutdown()
  }

  // Assuming channels take the format base/event/market
  val testData = Table(
    ("publisherChannel",  "subscriptionChannel",  "isMatch"),     // Subscription Description

    ("base",              "base",                 true),          // all base
    ("base/1",            "base",                 true),
    ("base/1/2",          "base",                 true),

    ("base",              "base/1",               false),         // all base event 1
    ("base/1",            "base/1",               true),
    ("base/1/2",          "base/1",               true),

    ("base",              "base/1/2",             false),         // all base event 1, market 2
    ("base/1",            "base/1/2",             false),
    ("base/1/2",          "base/1/2",             true),
    ("base/3/2",          "base/1/2",             false),

    ("base",              "base/*/2",             false),         // all base market 2
    ("base/1",            "base/*/2",             false),
    ("base/1/2",          "base/*/2",             true),
    ("base/3/2",          "base/*/2",             true)
  )

  val tester = new core.eventBus.EventBus()

  forAll(testData) {(publisherChannel: String, subscriptionChannel: String, isMatch: Boolean) =>
    "eventBus" should (if(isMatch) "match: " else "NOT match ") + "publisher: " + publisherChannel + ", subscriber: " + subscriptionChannel in {
      val eventBus = new EventBus()
      val subscriber = TestProbe()
      val sender = TestProbe()
      class TestMessage extends Message
      val testMessage = new TestMessage

      eventBus.subscribe(subscriber.ref, subscriptionChannel)
      eventBus.publish(MessageEvent(publisherChannel, testMessage, sender.ref))

      if (isMatch) subscriber.expectMsg(500 millis, testMessage) else subscriber.expectNoMsg(500 millis)
    }
  }
}
