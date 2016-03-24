package server

import domain.MatchProjection.MatchProjection
import domain.OrderProjection.OrderProjection
import domain.{MatchProjection, OrderProjection}

import scala.concurrent.duration._
import scala.language.postfixOps

case class Configuration(appKey: String,
                         username: String,
                         password: String,
                         apiUrl: String,
                         isoUrl: String,
                         navUrl: String,
                         orderProjection: OrderProjection   = OrderProjection.EXECUTABLE,
                         matchProjection: MatchProjection   = MatchProjection.ROLLED_UP_BY_AVG_PRICE,
                         orderManagerUpdateInterval: FiniteDuration = 1 hour,
                         systemAlertsChannel: String        = "systemAlerts",
                         marketUpdateChannel: String        = "marketUpdates",
                         orderUpdateChannel: String         = "orderUpdates",
                         navDataInstructions: String        = "navDataInstructions",
                         dataModelInstructions: String      = "dataModelInstructions",
                         orderManagerInstructions: String   = "orderManagerInstructions",
                         dataProviderInstructions: String   = "dataProviderInstructions") {

  private def getPublishChannel(ids: Seq[String]): String = ids.reduce(_ + "/" + _)

  def getMarketUpdateChannel(ids: Seq[String]): String = getPublishChannel(Seq(marketUpdateChannel) ++ ids)

  def getOrderUpdateChannel(ids: Seq[String]): String = getPublishChannel(Seq(orderUpdateChannel) ++ ids)
}