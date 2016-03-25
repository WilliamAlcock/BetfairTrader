package server

import domain.MatchProjection._
import domain.{MatchProjection, OrderProjection}
import domain.OrderProjection._

import scala.concurrent.duration._
import scala.language.postfixOps

case class Configuration(appKey: String,
                         username: String,
                         password: String,
                         apiUrl: String,
                         isoUrl: String,
                         navUrl: String,
                         orderManagerUpdateInterval: FiniteDuration = 1 hour,
                         systemAlertsChannel: String        = "systemAlerts",
                         marketUpdateChannel: String        = "marketUpdates",
                         orderUpdateChannel: String         = "orderUpdates",
                         autoTraderChannel: String          = "autoTraderUpdates",
                         navDataInstructions: String        = "navDataInstructions",
                         dataModelInstructions: String      = "dataModelInstructions",
                         orderManagerInstructions: String   = "orderManagerInstructions",
                         dataProviderInstructions: String   = "dataProviderInstructions",
                         autoTraderInstructions: String     = "autoTraderInstructions") {

  val orderProjection: OrderProjection   = OrderProjection.ALL
  val matchProjection: MatchProjection   = MatchProjection.ROLLED_UP_BY_AVG_PRICE

  private def getPublishChannel(ids: Seq[String]): String = ids.reduce(_ + "/" + _)

  private def toSeqString[T](i: Option[T]): Seq[String] = if (i.isDefined) Seq(i.get.toString) else Seq("*")

  private def stripPostFix(channel: Seq[String]): Seq[String] = channel.reverse.dropWhile(_ == "*").reverse

  def getMarketUpdateChannel(marketId: Option[String] = None): String = getPublishChannel(stripPostFix(Seq[String](marketUpdateChannel) ++ toSeqString(marketId)))

  def getOrderUpdateChannel(marketId: Option[String] = None, selectionId: Option[Long] = None, handicap: Option[Double] = None): String = getPublishChannel(
    stripPostFix(Seq[String](orderUpdateChannel) ++ toSeqString(marketId) ++ toSeqString(selectionId) ++ toSeqString(handicap))
  )

  def getAutoTraderUpdateChannel(strategyId: Option[String]): String = getPublishChannel(stripPostFix(Seq[String](autoTraderChannel) ++ toSeqString(strategyId)))
}