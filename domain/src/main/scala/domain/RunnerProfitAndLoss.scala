/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class RunnerProfitAndLoss(selectedId: Long,
                               ifWin: Double,
                               ifLose: Double,
                               ifPlace: Double)

object RunnerProfitAndLoss {
  implicit val formatRunnerProfitAndLoss = Json.format[RunnerProfitAndLoss]
}