package core.dataProvider.polling

import scala.collection.immutable.HashMap

case class PollingGroups(markets: HashMap[String, MarketRef] = HashMap.empty) {

  def addSubscriber(marketId: String, ref: String, pollingGroup: PollingGroup): PollingGroups = {
    this.copy(markets = markets + (marketId -> markets.getOrElse(marketId, new MarketRef()).addSubscriber(ref, pollingGroup)))
  }

  private def checkIfEmpty(marketRef: MarketRef, marketId: String): PollingGroups = {
    marketRef.pollingGroup match {
      case EMPTY => this.copy(markets = markets - marketId)
      case _ => this.copy(markets = markets + (marketId -> marketRef))
    }
  }

  def removeSubscriber(marketId: String, ref: String, pollingGroup: PollingGroup): PollingGroups = {
    markets.contains(marketId) match {
      case true => checkIfEmpty(markets(marketId).removeSubscriber(ref, pollingGroup), marketId)
      case false => this
    }
  }

  def removeSubscriber(ref: String): PollingGroups = {
    markets.foldLeft[PollingGroups](this)((acc, x) =>
      acc.checkIfEmpty(x._2.removeSubscriber(ref), x._1)
    )
  }

  def removeAllSubscribers(): PollingGroups = {
    PollingGroups()
  }

  lazy val pollingGroups: HashMap[PollingGroup, Set[String]] = markets.foldLeft[HashMap[PollingGroup, Set[String]]](HashMap.empty)(
    (acc, x) => acc + (x._2.pollingGroup -> acc.getOrElse(x._2.pollingGroup, Set[String]()).+(x._1))
  )
}