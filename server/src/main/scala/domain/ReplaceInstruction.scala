/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.Json

case class ReplaceInstruction(betId: String, newPrice: Double)

object ReplaceInstruction {
  implicit val formatReplaceInstruction = Json.format[ReplaceInstruction]
}