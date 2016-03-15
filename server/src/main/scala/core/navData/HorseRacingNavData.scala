package core.navData

import org.joda.time.DateTime
import play.api.libs.json.Json

case class HorseRacingData(today: List[HorseRacingCountry], tomorrow: List[HorseRacingCountry], groups: List[NavData])

case class Venue(name: String, races: List[Race])

case class HorseRacingCountry(name: String, venues: List[Venue])

object HorseRacingData {
  implicit val formatVenue = Json.format[Venue]
  implicit val formatHorseRacingCountry = Json.format[HorseRacingCountry]
  implicit val formatHorseRacingData = Json.format[HorseRacingData]
}

trait HorseRacingNavData extends NavDataUtils {

  /*
    Today:
    Tomorrow:
    Groups:
   */
  def getHorseRacingData(navData: NavData): HorseRacingData = {
    val data = getEventType(navData, "7").get
    val today = getRacesForDay(data, DateTime.now)
    val tomorrow = getRacesForDay(data, DateTime.now.plusDays(1))

    HorseRacingData(
      today,
      tomorrow,
      data.children.filter(!_.isInstanceOf[Race])
    )
  }

  def getRacesForDay(eventType: EventType, date: DateTime): List[HorseRacingCountry] = {
    val races = eventType.children.map{
      case x: Race if x.startTime.withTimeAtStartOfDay() == date.withTimeAtStartOfDay() => List(x)
      case _ => List()
    }.flatten

    races.groupBy(_.countryCode).map{case (k,v) => HorseRacingCountry(k, v.groupBy(_.venue).map{case (l,w) => Venue(l, w)}.toList)}.toList
  }
}
