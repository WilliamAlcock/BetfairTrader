package core.dataStore.csvReader

import scala.collection.immutable.HashMap

case class EventData(markets: HashMap[String, MarketData] = HashMap.empty) extends Data {
  def addCSV(csv: CSVData): EventData = this.copy(markets = markets + (csv.marketId -> markets.getOrElse(csv.marketId, MarketData(csv.marketId)).addCSV(csv)))
}


