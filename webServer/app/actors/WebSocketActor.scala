package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import core.api.output._
import play.api.libs.json.{JsObject, Json}
import services.{JsonrpcRequest, JsonrpcResponse, JsonrpcResponseResult}

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
      val request = Json.parse(x).validate[JsonrpcRequest]
      val command = request.getOrElse(throw new Exception("Invalid Request")).toCommand()
      println ("sending command to server: " + command)
      server ! command
    case x: EventTypeUpdate       => sendResponse("EventTypeUpdate", Json.toJson(x.data).as[JsObject])
    case x: EventUpdate           => sendResponse("EventUpdate", Json.toJson(x.data).as[JsObject])
    case x: MarketCatalogueUpdate => sendResponse("MarketCatalogueUpdate", Json.toJson(x.data).as[JsObject])
    case x: MarketBookUpdate      =>
      sendResponse("MarketBookUpdate", Json.obj(
        "data" -> Json.toJson(x.data).as[JsObject],
        "runners" -> Json.toJson(x.runners).as[JsObject]
      ))
    case x: NavigationDataUpdate  =>
      sendResponse("NavigationDataUpdate", Json.obj(
        "navData" -> Json.toJson(x.data).as[JsObject],
        "competitions" -> Json.toJson(x.competitions).as[JsObject]
      ))
  }

}

object WebSocketActor {
  def props(out: ActorRef) = Props(new WebSocketActor(out))
}