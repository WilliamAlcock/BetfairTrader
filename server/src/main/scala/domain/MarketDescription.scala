package domain

import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes, Reads}

case class MarketDescription(persistenceEnabled: Boolean,
                        bspMarket: Boolean,
                        marketTime: Option[DateTime] = None,
                        suspendTime: Option[DateTime] = None,
                        settleTime: Option[DateTime] = None,
                        bettingType: String,
                        turnInPlayEnabled: Boolean,
                        marketType: String,
                        regulator: String,
                        marketBaseRate: Double,
                        discountAllowed: Boolean,
                        wallet: String,
                        rules: String,
                        rulesHasDate: Boolean,
                        clarifications: Option[String] = None)

object MarketDescription {

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)

  implicit val formatMarketDescription = Json.format[MarketDescription]
}