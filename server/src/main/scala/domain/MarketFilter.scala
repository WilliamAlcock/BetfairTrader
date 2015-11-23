package domain

import domain.MarketBettingType.MarketBettingType
import domain.OrderStatus.OrderStatus
import play.api.libs.json.{JsNull, Json, JsValue, Writes}

// TODO why are these variables ints not string

case class MarketFilter(textQuery: Option[String] = None,
                        exchangeIds: Set[Int] = Set.empty,
                        eventTypeIds: Set[Int] = Set.empty,
                        marketIds: Set[Int] = Set.empty,
                        inPlayOnly: Option[Boolean] = None,
                        eventIds: Set[Int] = Set.empty,
                        competitionIds: Set[Int] = Set.empty,
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