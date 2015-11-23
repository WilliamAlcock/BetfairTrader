/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class VenueResult(venue: String, marketCount: Int)

object VenueResult {
  implicit val formatVenueResult = Json.format[VenueResult]
}