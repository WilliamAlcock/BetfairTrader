package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import core.navData.{HorseRacingData, SoccerData}
import domain.{ListMarketCatalogueContainer, MarketBookUpdate}
import play.api.libs.json.{JsObject, Json}
import services._

class WebSocketActor(out: ActorRef) extends Actor with ActorLogging {
  // TODO get this from config
  val server = context.actorSelection("akka.tcp://BFTrader@127.0.0.1:2552/user/controller")

  def sendResponse(responseType: String, data: JsObject) = {
    val response = JsonrpcResponse(result = Some(JsonrpcResponseResult(responseType, data)))
    val r = Json.toJson(response).toString()
    out ! r
  }

  override def receive = {
    // TODO separate actors for data from server and from ui
    // TODO handle errors
    // TODO Implement responses

    case x: String =>
      println("request", x)
      val request = Json.parse(x).validate[JsonrpcRequest]
      val command = request.getOrElse(throw new Exception("Invalid Request")).toCommand()
      println ("sending command to server: " + command)
      server ! command
    case x: SoccerData        => sendResponse("SoccerData", Json.toJson(x).as[JsObject])
    case x: HorseRacingData   => sendResponse("HorseRacingData", Json.toJson(x).as[JsObject])
    case x: MarketBookUpdate  => sendResponse("MarketBookUpdate", Json.toJson(UIMarketBook(x.data)).as[JsObject])
    case x: ListMarketCatalogueContainer =>
      if (x.result.nonEmpty) {
        sendResponse("MarketCatalogueUpdate", Json.toJson(x.result.head).as[JsObject])
      }

    case x =>
      println("Message Received: " + x)

//    case x: EventTypeUpdate       => sendResponse("EventTypeUpdate", Json.toJson(x.data).as[JsObject])
//    case x: EventUpdate           => sendResponse("EventUpdate", Json.toJson(x.data).as[JsObject])
//    case x: MarketCatalogueUpdate => sendResponse("MarketCatalogueUpdate", Json.toJson(x.data).as[JsObject])

//    case x: NavigationDataUpdate  =>
  }
}

object WebSocketActor {
  def props(out: ActorRef) = Props(new WebSocketActor(out))
}