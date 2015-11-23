/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class ReplaceExecutionReportContainer(result: ReplaceExecutionReport)

object ReplaceExecutionReportContainer {
  implicit val formatUpdateExecutionReportContainer = Json.format[ReplaceExecutionReportContainer]
}