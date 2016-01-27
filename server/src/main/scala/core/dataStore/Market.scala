package core.dataStore

import domain._
import play.api.libs.json.{JsValue, Json, Writes}

import scala.collection.immutable.HashMap

case class Market(marketId: String,
                  inplay: Boolean,
                  marketCatalogue: Option[MarketCatalogue] = None,
                  dataByMinute: HashMap[String, List[Entry]] = HashMap.empty)

object Market {
  implicit val writeAnyMapFormat = new Writes[HashMap[String, List[Entry]]] {
    def writes(map: HashMap[String, List[Entry]]): JsValue = {
      Json.obj(map.map {
        case (s, a) => s -> Json.toJsFieldJsValueWrapper(a)
      }.toSeq: _*)
    }

  }
// TODO add reads, change to format
  implicit val format = Json.writes[Market]
}