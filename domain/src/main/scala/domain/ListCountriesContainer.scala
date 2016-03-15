/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class ListCountriesContainer(result: List[CountryCodeResult])

object ListCountriesContainer {
  implicit val formatCountryCodeResultContainer = Json.format[ListCountriesContainer]
}