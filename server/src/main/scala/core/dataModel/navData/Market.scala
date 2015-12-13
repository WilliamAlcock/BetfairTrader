package core.dataModel.navData

import org.joda.time.DateTime

case class Market(exchangeId: String,
                  override val id: String,
                  marketStartTime: DateTime,
                  marketType: String,
                  numberOfWinners: String,
                  override val name: String) extends NavData {
  override def getType: String = "MARKET"
  override lazy val hasChildren: Boolean = false
  override lazy val numberOfMarkets: Int = 1
  override lazy val countryCode: String = ""
  override lazy val hasGroupChildren: Boolean = false
  override lazy val hasGroupGrandChildren: Boolean = false
  override val startTime: DateTime = marketStartTime
}

