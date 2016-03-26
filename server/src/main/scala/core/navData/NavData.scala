package core.navData

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

trait NavData {
  def getType: String
  val exchangeId: String
  val name: String
  val id: String
  val hasChildren: Boolean
  val numberOfMarkets: Int
  val countryCode: String
  val startTime: DateTime
}

object NavData {
  implicit val readsNavData: Reads[NavData] = {
    new Reads[NavData] {
      def reads(json: JsValue) = JsSuccess(convertData(json))
    }
  }

  implicit val writesNavData: Writes[NavData] = {
    new Writes[NavData] {
      def writes(n: NavData) = convertNavData(n)
    }
  }

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.jodaDateWrites(dateFormat)

  private def getBaseObject(n: NavData): JsObject = Json.obj(
    "id"                    -> JsString(n.id),
    "name"                  -> JsString(n.name),
    "type"                  -> JsString(n.getType),
    "countryCode"           -> JsString(n.countryCode),
    "numberOfMarkets"       -> JsNumber(n.numberOfMarkets),
    "startTime"             -> JsString(n.startTime.toString)
  )

  private def convertNavData(n: NavData): JsObject = n match {
    case x: EventType =>  getBaseObject(x) + ("children" -> Json.toJson(x.children))
    case x: Group =>      getBaseObject(x) + ("children" -> Json.toJson(x.children))
    case x: Event =>      getBaseObject(x) + ("children" -> Json.toJson(x.children))
    case x: Race =>       getBaseObject(x) ++ Json.obj(
      "children"    -> Json.toJson(x.children),
      "startTime"   -> JsString(x.startTime.toString),
      "venue"       -> JsString(x.venue),
      "raceNumber"  -> JsString(x.raceNumber))
    case x: Market =>     getBaseObject(x) ++ Json.obj(
      "exchangeId"      -> JsString(x.exchangeId),
      "marketStartTime" -> Json.toJson(x.marketStartTime),
      "marketType"      -> JsString(x.marketType),
      "numberOfWinners" -> JsString(x.numberOfWinners))
  }

  private def convertData(data: JsValue): NavData = {
    get(data, "type") match {
      case "EVENT_TYPE" =>  new EventType(getChildren(data), get(data, "id"), get(data, "name"))
      case "GROUP" =>       new Group(getChildren(data), get(data, "id"), get(data, "name"))
      case "EVENT" =>       new Event(getChildren(data), get(data, "id"), get(data, "name"), get(data, "countryCode"))
      case "RACE" =>
        new Race(
          getChildren(data),
          get(data, "id"),
          get(data, "name"),
          DateTime.parse((data \ "startTime").as[String], DateTimeFormat.forPattern(dateFormat).withZoneUTC()),
          get(data, "venue"),
          get(data, "raceNumber"),
          get(data, "countryCode")
        )
      case "MARKET" =>
        new Market(
            get(data, "exchangeId"),
            get(data, "id"),
            DateTime.parse((data \ "marketStartTime").as[String], DateTimeFormat.forPattern(dateFormat).withZoneUTC()),
            get(data, "marketType"),
            get(data, "numberOfWinners"),
            get(data, "name")
          )
    }
  }

  private def get(data: JsValue, property: String): String = {
    val prop = (data \ property)
    if (prop.toOption.isDefined)
      prop.get match {
        case x: JsString => x.value
        case x: JsNumber => x.toString
        case _ => ""
      }
    else ""
  }


  private def getChildren(data: JsValue): List[NavData] = (data \ "children").as[JsArray].value.map(x => convertData(x)).toList
}