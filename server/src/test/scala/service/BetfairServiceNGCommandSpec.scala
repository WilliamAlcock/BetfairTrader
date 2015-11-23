package service

import _root_.service.UnitSpec
import _root_.service.WireMockFixture
import akka.actor.ActorSystem
import com.betfair.Configuration
import com.betfair.domain.{JsonrpcRequest, ListEventResultContainer, MarketFilter}
import com.betfair.service.BetfairServiceNGCommand
import org.scalatest.concurrent.Eventually._
import org.scalatest.time.{Second, Span}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global


class BetfairServiceNGCommandSpec extends UnitSpec with WireMockFixture {

  implicit val system = ActorSystem("on-spray-can")

  val sessionToken = "12345"

  val testConfig = Configuration(
    appKey = "testAppKey",
    username = "testUsername",
    password = "testPassword",
    apiUrl = "http://localhost:8080/",
    isoUrl = "http://localhost:8080/"
  )

  val service = new BetfairServiceNGCommand(testConfig)

  "The BetfairServiceNGCommand when sending http request" should {

    "add the app key to http header" in {

      import spray.httpx.PlayJsonSupport._

      val requests = addRequestHeaderListener()

      val marketFilter = new MarketFilter()
      val params = mutable.HashMap[String, Object]("filter" -> marketFilter)
      val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listEvents", params = params)
      service.makeAPIRequest[ListEventResultContainer](sessionToken, request)

      val timeout = org.scalatest.concurrent.Eventually.PatienceConfig(Span(1, Second))
      eventually(requests should have length(1))(timeout)
      val headerAppToken = requests(0).getHeader("X-Application")
      headerAppToken.firstValue() should be("testAppKey")
    }

    "add the session token to http header" in {

      import spray.httpx.PlayJsonSupport._

      val requests = addRequestHeaderListener()

      val marketFilter = new MarketFilter()
      val params = mutable.HashMap[String, Object]("filter" -> marketFilter)
      val request = new JsonrpcRequest(id = "1", method = "SportsAPING/v1.0/listEvents", params = params)
      service.makeAPIRequest[ListEventResultContainer](sessionToken, request)

      val timeout = org.scalatest.concurrent.Eventually.PatienceConfig(Span(1, Second))
      eventually(requests should have length(1))(timeout)
      val headerSessionToken = requests(0).getHeader("X-Authentication")
      headerSessionToken.firstValue() should be(sessionToken)
    }

  }

}