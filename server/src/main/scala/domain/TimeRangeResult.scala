/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class TimeRangeResult(timeRange: TimeRange, marketCount: Int)

object TimeRangeResult {
  implicit val formatTimeRangeResult = Json.format[TimeRangeResult]
}