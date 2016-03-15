package domain

import play.api.libs.json.{Writes, Reads}

object PersistenceType extends Enumeration {
    type PersistenceType = Value
    val LAPSE, PERSIST, MARKET_ON_CLOSE = Value

    implicit def enumReads: Reads[PersistenceType] = EnumUtils.enumReads(PersistenceType)

    implicit def enumWrites: Writes[PersistenceType] = EnumUtils.enumWrites
  }
