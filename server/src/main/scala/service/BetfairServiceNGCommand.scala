package service

import akka.actor.ActorSystem
import domain._
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import server.Configuration
import spray.client.pipelining._
import spray.http.{HttpResponse, StatusCodes}
import spray.httpx.encoding.{Deflate, Gzip}
import spray.httpx.unmarshalling.FromResponseUnmarshaller

import scala.concurrent._
import scala.language.postfixOps

class BetfairServiceNGCommand(val config: Configuration)
                             (implicit executionContext: ExecutionContext, system: ActorSystem) {

  private def checkStatusCodeAndUnmarshal[T](implicit unmarshaller: FromResponseUnmarshaller[T]): Future[HttpResponse] => Future[Option[T]] =
    (futRes: Future[HttpResponse]) => futRes.map {
      res =>
        if (res.status == StatusCodes.OK) {
          system.log.debug(res.toString)
          Some(unmarshal[T](unmarshaller)(res))
        } else None
    }

  def makeLoginRequest(request: LoginRequest)(implicit unmarshaller: FromResponseUnmarshaller[LoginResponse]): Future[Option[LoginResponse]] = {

    val pipeline =
        addHeader("Accept", "application/json") ~>
        addHeader("Accept-Charset", "UTF-8") ~>
        addHeader("X-Application", config.appKey) ~>
        sendReceive ~>
        checkStatusCodeAndUnmarshal[LoginResponse]

    system.log.debug(request.toString)

    pipeline {
      Post(config.isoUrl + "/login?username=" + request.username + "&password=" + request.password)
    }
  }

  def makeLogoutRequest(sessionToken: String)(implicit unmarshaller: FromResponseUnmarshaller[LogoutResponse]) {

    val pipeline =
        addHeader("Accept", "application/json") ~>
        addHeader("Accept-Charset", "UTF-8") ~>
        addHeader("X-Application", config.appKey) ~>
        addHeader("X-Authentication", sessionToken) ~>
        sendReceive ~>
        checkStatusCodeAndUnmarshal[LogoutResponse]

    system.log.debug("Logout")

    pipeline {
      Post(config.isoUrl + "/logout")
    }
  }

  def makeNavigationDataRequest(sessionToken: String): Future[String] = Future[String] {
    val httpGet = new HttpGet(config.navUrl)
    // set the desired header values
    httpGet.setHeader("Accept", "application/json")
    httpGet.setHeader("Accept-Charset", "UTF-8")
    httpGet.setHeader("X-Application", config.appKey)
    httpGet.setHeader("X-Authentication", sessionToken)
    // execute the request
    val response = HttpClientBuilder.create().build().execute(httpGet)
    val inputStream = response.getEntity().getContent()
    val output = scala.io.Source.fromInputStream(inputStream).mkString
    inputStream.close()
    output
  }


  def makeAPIRequest[T](sessionToken: String, request: JsonrpcRequest)(implicit unmarshaller: FromResponseUnmarshaller[T]): Future[Option[T]] = {

    import spray.httpx.PlayJsonSupport._

    val pipeline =
        addHeader("Accept", "application/json") ~>
        addHeader("Accept-Charset", "UTF-8") ~>
        addHeader("X-Application", config.appKey) ~>
        addHeader("X-Authentication", sessionToken) ~>
        sendReceive ~>
        decode(Gzip) ~>
        decode(Deflate) ~>
        checkStatusCodeAndUnmarshal[T]

    system.log.debug(request.toString)

    pipeline {
      Post(config.apiUrl, request)
    }
  }

}