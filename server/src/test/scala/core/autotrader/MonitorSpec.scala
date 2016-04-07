package core.autotrader

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}

class MonitorSpec extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(""))) with FlatSpecLike with Matchers with MockFactory with BeforeAndAfterEach {

}

