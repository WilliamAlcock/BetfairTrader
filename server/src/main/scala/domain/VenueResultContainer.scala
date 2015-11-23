package domain

import play.api.libs.json.Json

case class VenueResultContainer(result: List[VenueResult])

object VenueResultContainer {
  implicit val formatVenueResultContainer = Json.format[VenueResultContainer]
}