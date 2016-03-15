package domain

import play.api.libs.json.{Writes, Reads}

object SortDir extends Enumeration {
   type SortDir = Value
   val EARLIEST_TO_LATEST, LATEST_TO_EARLIEST = Value

   implicit def enumReads: Reads[SortDir] = EnumUtils.enumReads(SortDir)

   implicit def enumWrites: Writes[SortDir] = EnumUtils.enumWrites
 }
