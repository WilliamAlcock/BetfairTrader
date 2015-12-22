/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.{Writes, Reads}

object BetStatus extends Enumeration {
  type BetStatus = Value
  val SETTLED, VOIDED, LAPSED, CANCELLED = Value

  implicit def enumReads: Reads[BetStatus] = EnumUtils.enumReads(BetStatus)

  implicit def enumWrites: Writes[BetStatus] = EnumUtils.enumWrites
}