package domain

import play.api.libs.json.Json

case class LoginResponse(token: String, product: String, status: String, error: String)

object LoginResponse {
  implicit val formatLoginResponse = Json.format[LoginResponse]
}

