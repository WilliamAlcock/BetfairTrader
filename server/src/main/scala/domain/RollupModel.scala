package domain

import play.api.libs.json.{Writes, Reads}

object RollupModel extends Enumeration {
  type RollupModel = Value
  val STAKE, PAYOUT, MANAGED_LIABILITY, NONE = Value

  implicit val enumReads: Reads[RollupModel] = EnumUtils.enumReads(RollupModel)

  implicit def enumWrites: Writes[RollupModel] = EnumUtils.enumWrites
}
