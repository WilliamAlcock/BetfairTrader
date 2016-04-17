package services

import core.api.commands._
import play.api.libs.json._

case class JsonrpcRequest(jsonrpc: String = "2.0", method: String, id: Int, params: JsObject) {
  // Converts request to server commands
  def toCommand: Command = method match {
    case "getNavigationData"          => params.as[GetNavigationData]

    case "placeOrders"                => params.as[PlaceOrders]
    case "replaceOrders"              => params.as[ReplaceOrders]
    case "updateOrders"               => params.as[UpdateOrders]
    case "cancelOrders"               => params.as[CancelOrders]

    case "listMarketCatalogue"        => params.as[ListMarketCatalogue]
    case "listMarketBook"             => params.as[ListMarketBook]
    case "listCurrentOrders"          => params.as[ListCurrentOrders]
    case "listMatches"                => ListMatches

    case "subscribeToSystemAlerts"    => SubscribeToSystemAlerts
    case "subscribeToMarkets"         => params.as[SubscribeToMarkets]
    case "unSubscribeFromMarkets"     => params.as[UnSubscribeFromMarkets]
    case "unSubscribe"                => UnSubscribe

    case "subscribeToOrderUpdates"    => params.as[SubscribeToOrderUpdates]

    case "subscribeToAutoTraderUpdates" => params.as[SubscribeToAutoTraderUpdates]
    case "startStrategy"                => params.as[StartStrategy]
    case "stopStrategy"                 => params.as[StopStrategy]
    case "listRunningStrategies"        => ListRunningStrategies

    case x =>
      println("Request " + x)
      throw new Exception("Invalid Request")
  }
}

object JsonrpcRequest {
  implicit val readJsonrpcRequest = Json.reads[JsonrpcRequest]
}