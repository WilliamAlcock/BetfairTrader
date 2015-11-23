/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.{Writes, Reads}

object GroupBy extends Enumeration {
  type GroupBy = Value
  val EVENT_TYPE, EVENT, MARKET, SIDE, BET = Value

  implicit val enumReads: Reads[GroupBy] = EnumUtils.enumReads(GroupBy)

  implicit def enumWrites: Writes[GroupBy] = EnumUtils.enumWrites
}