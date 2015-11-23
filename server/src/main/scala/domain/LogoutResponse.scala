package domain

import play.api.libs.json.Json

case class LogoutResponse(token: String, product: String, status: String, error: String)

object LogoutResponse {
  implicit val formatLogoutResponse = Json.format[LogoutResponse]
}

