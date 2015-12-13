package services

import play.api.libs.json.Json

case class JsonRpcRequest(implicit val jsonrpc = "2.0") {}

object JsonRpcRequest {
  implicit val formatJsonRpcRequest = Json.format[JsonRpcRequest]
}