/**
 * Created by Alcock on 06/10/2015.
 */

package domain

import play.api.libs.json.{Writes, Reads}

object TimeGranularity extends Enumeration {
  type TimeGranularity = Value
  val DAYS, HOURS, MINUTES = Value

  implicit def enumReads: Reads[TimeGranularity] = EnumUtils.enumReads(TimeGranularity)

  implicit def enumWrites: Writes[TimeGranularity] = EnumUtils.enumWrites
}