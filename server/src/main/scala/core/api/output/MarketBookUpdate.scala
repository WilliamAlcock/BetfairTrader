package core.api.output

import domain.{Runner, MarketBook}

import scala.collection.immutable.HashMap

case class MarketBookUpdate(data: MarketBook, runners: HashMap[String, Runner]) extends Output