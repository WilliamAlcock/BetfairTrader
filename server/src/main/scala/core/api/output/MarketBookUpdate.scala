package core.api.output

import domain.MarketBook
import org.joda.time.DateTime

case class MarketBookUpdate(timestamp: DateTime, data: MarketBook) extends Output