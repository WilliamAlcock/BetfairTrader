package domain

import domain.PersistenceType.PersistenceType
import play.api.libs.json.Json

case class UpdateInstruction(betId: String, newPersistenceType: PersistenceType)

object UpdateInstruction {
  implicit val formatUpdateInstruction = Json.format[UpdateInstruction]
}