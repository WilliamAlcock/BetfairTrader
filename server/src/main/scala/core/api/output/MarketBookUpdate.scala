package core.api.output

import core.dataModel.indicators.TickData
import domain.MarketBook

import scala.collection.immutable.HashMap

case class MarketBookUpdate(data: MarketBook, tickData: HashMap[String, List[TickData]]) extends Output