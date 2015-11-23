/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class CountryCodeResult(countryCode: String, marketCount: Int)

object CountryCodeResult {
  implicit val formatCompetitionResult = Json.format[CountryCodeResult]
}