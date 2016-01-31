package core.dataStore

import core.api.output.MarketBookUpdate
import domain._
import play.api.libs.json._

import scala.collection.immutable.HashMap

case class MarketDocument(_id: Option[OID] = None,
                          marketId: String,
                          marketCatalogue: Option[MarketCatalogue] = None,
                          data: Map[String, List[MarketBookUpdate]] = HashMap.empty)

object MarketDocument {
  implicit val marketDocumentFormat = Json.format[MarketDocument]
}