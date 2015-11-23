/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class TimeRangeResultContainer(result: List[TimeRangeResult])

object TimeRangeResultContainer {
  implicit val formatTimeRangeResultContainer = Json.format[TimeRangeResultContainer]
}
