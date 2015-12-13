package services

import play.api.libs.json.{JsObject, Json}

case class JsonrpcResponseResult(resultType: String, result: JsObject)

object JsonrpcResponseResult {
  implicit val formatJsonrpcResponseResult = Json.format[JsonrpcResponseResult]
}

case class JsonrpcResponseError(code: Int, message: String, data: Option[String] = None)

object JsonrpcResponseError {
  implicit val formatJsonrpcResponseError = Json.format[JsonrpcResponseError]
}

case class JsonrpcResponse(jsonrpc: String = "2.0", result: Option[JsonrpcResponseResult] = None, error: Option[JsonrpcResponseError] = None, id: Int = 0) {}

object JsonrpcResponse {
  implicit val formatJsonrpcResponse = Json.format[JsonrpcResponse]
}