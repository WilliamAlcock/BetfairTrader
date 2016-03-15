package catalogueBuilder

import domain._

case class MarketDocument(marketId: String,
                          marketCatalogue: Option[MarketCatalogue] = None,
                          data: List[MarketBookUpdate] = List.empty)