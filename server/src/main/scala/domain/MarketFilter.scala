package domain

import domain.MarketBettingType.MarketBettingType
import domain.OrderStatus.OrderStatus
import play.api.libs.json.{JsNull, Json, JsValue, Writes}

// TODO why are these variables ints not string

case class MarketFilter(textQuery: Option[String] = None,
                        exchangeIds: Set[String] = Set.empty,
                        eventTypeIds: Set[String] = Set.empty,
                        marketIds: Set[String] = Set.empty,
                        inPlayOnly: Option[Boolean] = None,
                        eventIds: Set[String] = Set.empty,
                        competitionIds: Set[String] = Set.empty,
                        venues: Set[String] = Set.empty,
                        bspOnly: Option[Boolean] = None,
                        turnInPlayEnabled: Option[Boolean] = None,
                        marketBettingTypes: Set[MarketBettingType] = Set.empty,
                        marketCountries: Set[String] = Set.empty,
                        marketTypeCodes: Set[String] = Set.empty,
                        marketStartTime: Option[TimeRange] = None,
                        withOrders: Set[OrderStatus] = Set.empty)


object MarketFilter {

  implicit val writeTimeRangeFormat = new Writes[TimeRange] {

    def writes(timeRange: TimeRange): JsValue = {
      if (timeRange == null) JsNull else Json.toJson(timeRange)
    }
  }

  implicit val formatMarketFilter = Json.format[MarketFilter]
}