package domain

import play.api.libs.json.{Writes, Reads}

object MarketBettingType extends Enumeration {
  type MarketBettingType = Value
  val ODDS, LINE, RANGE, ASIAN_HANDICAP_DOUBLE_LINE, ASIAN_HANDICAP_SINGLE_LINE, FIXED_ODDS = Value

  implicit def enumReads: Reads[MarketBettingType] = EnumUtils.enumReads(MarketBettingType)

  implicit def enumWrites: Writes[MarketBettingType] = EnumUtils.enumWrites
}
