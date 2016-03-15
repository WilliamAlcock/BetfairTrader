package core.navData

import org.joda.time.DateTime
import play.api.libs.json.Json

case class Market(override val exchangeId: String,
                  override val id: String,
                  marketStartTime: DateTime,
                  marketType: String,
                  numberOfWinners: String,
                  override val name: String) extends NavData {
  override def getType: String = "MARKET"
  override lazy val hasChildren: Boolean = false
  override lazy val numberOfMarkets: Int = 1
  override lazy val countryCode: String = ""
  override val startTime: DateTime = marketStartTime
}

object Market {
  implicit val formatMarket = Json.format[Market]
}