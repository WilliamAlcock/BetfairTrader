package services

import core.api.commands._
import play.api.libs.json._

case class JsonrpcRequest(jsonrpc: String = "2.0", method: String, id: Int, params: JsObject) {
  def toCommand(): Command = {
    method match {
      case "subscribeToNavData" => SubscribeToNavData
      case "subscribeToMarkets" => params.as[SubscribeToMarkets]
      case "unSubscribeFromMarkets" => params.as[UnSubscribeFromMarkets]
      case "listMarketCatalogue" => params.as[ListMarketCatalogue]

      case "listEventTypes" => ListEventTypes
      case "listEvents" => params.as[ListEvents]
//      case "startPollingMarkets" => params.as[StartPollingMarkets]
//      case "stopPollingMarkets" => params.as[StopPollingMarkets]
//      case "stopPollingAllMarkets" => StopPollingAllMarkets()
      case "placeOrders" => params.as[PlaceOrders]
      case "replaceOrders" => params.as[ReplaceOrders]
      case "updateOrders" => params.as[UpdateOrders]
      case "cancelOrders" => params.as[CancelOrders]

      case _ => throw new Exception("Invalid Request")
    }
  }
}

object JsonrpcRequest {
  implicit val readJsonrpcRequest = Json.reads[JsonrpcRequest]
}