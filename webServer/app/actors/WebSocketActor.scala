package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import core.navData.{HorseRacingData, SoccerData}
import core.orderManager.OrderManager._
import domain.{ListCurrentOrdersContainer, ListMarketCatalogueContainer, MarketBookUpdate}
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
      val command = request.getOrElse(throw new Exception("Invalid Request")).toCommand
      println ("sending command to server: " + command)
      server ! command
    case x: SoccerData                    => sendResponse("SoccerData", Json.toJson(x).as[JsObject])
    case x: HorseRacingData               => sendResponse("HorseRacingData", Json.toJson(x).as[JsObject])
    case x: MarketBookUpdate              => sendResponse("MarketBookUpdate", Json.toJson(UIMarketBook(x.data)).as[JsObject])
    case x: ListMarketCatalogueContainer if x.result.nonEmpty => sendResponse("MarketCatalogueUpdate", Json.toJson(x.result.head).as[JsObject])
    case x: ListCurrentOrdersContainer =>
      if (x.result.currentOrders.nonEmpty) {
        sendResponse("CurrentOrdersUpdate", Json.toJson(x.result).as[JsObject])
      } else println("OrderBook has no current orders")
    case x: ListMatchesContainer =>
      if (x.matches.nonEmpty) {
        sendResponse("CurrentMatchesUpdate", Json.toJson(x).as[JsObject])
      } else println("OrderBook has no matches")
    case x: OrderMatched                   => sendResponse("OrderMatched", Json.toJson(x).as[JsObject])
    case x: OrderPlaced                   => sendResponse("OrderPlaced", Json.toJson(x).as[JsObject])
    case x: OrderUpdated                  => sendResponse("OrderUpdated", Json.toJson(x).as[JsObject])
    case x: OrderExecuted                 => sendResponse("OrderExecuted", Json.toJson(x).as[JsObject])
    case x => println("Message Received: " + x)
  }
}

object WebSocketActor {
  def props(out: ActorRef) = Props(new WebSocketActor(out))
}