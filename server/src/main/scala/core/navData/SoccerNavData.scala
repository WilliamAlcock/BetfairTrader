package core.navData

import java.util.Locale

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.Json

case class SoccerData(today: List[NavData], tomorrow: List[NavData], topCountries: List[SoccerCountry], allFootball: List[SoccerCountry])

case class SoccerCountry(name: String, navData: List[NavData])

object SoccerData {
  implicit val formatSoccerCountry = Json.format[SoccerCountry]
  implicit val formatSoccerData = Json.format[SoccerData]
}

trait SoccerNavData extends NavDataUtils {

  /*
    Today:
    Tomorrow:
    Top Countries:
    All Football:
   */
  def getSoccerData(navData: NavData): SoccerData = {
    val soccerData = getEventType(navData, "1").get
    val fixtures = getFixturesByDay(soccerData)
    val countries = groupByCountry(soccerData)
    val topCountries = List("GB", "ES", "IT", "DE", "", "FR").map(x => SoccerCountry(getDisplayCountry(x), countries(getDisplayCountry(x))))
    val allFootball = countries.map{case (k,v) => SoccerCountry(k, v)}.toList.sortBy(_.name)

    SoccerData(
      getFixturesForDay(fixtures, DateTime.now()),
      getFixturesForDay(fixtures, DateTime.now().plusDays(1)),
      topCountries,
      allFootball
    )
  }

  def getDisplayCountry(code: String): String = if (code == "") "International" else new Locale("", code).getDisplayCountry

  def explodeCountries(navData: NavData): List[NavData] = navData match {
    case x: Group if x.name.endsWith("Soccer") => x.children
    case x: NavData => List(x)
  }

  def groupByCountry(eventType: EventType): Map[String, List[NavData]] = {
    eventType.children.groupBy(x => getDisplayCountry(x.countryCode)).mapValues(x => x.map(explodeCountries).flatten)
  }

  /*
    Return all groups that start with "Fixture"
   */
  def getFixtures(navData: NavData): List[Group] = navData match {
    case x: Event => x.children.map(getFixtures).flatten
    case x: Group if x.name.startsWith("Fixtures") => List(x)
    case x: Group => x.children.map(getFixtures).flatten
    case x: EventType => x.children.map(getFixtures).flatten
    case x: Market => List.empty[Group]
    case x: Race => List.empty[Group]
  }

  def getFixturesForDay(fixtures: Map[DateTime, List[Group]], date: DateTime): List[NavData] = fixtures(date.withTimeAtStartOfDay().withZone(DateTimeZone.UTC)).map(x => x.children).flatten

  def getFixturesByDay(navData: NavData): Map[DateTime, List[Group]] = getFixtures(navData).groupBy(x => getFixturesDate(x.name))

  def getFixturesDate(name: String): DateTime = {
    val months = Map(
      "January" -> 0,
      "February" -> 2,
      "March" -> 3,
      "April" -> 4,
      "May" -> 5,
      "June" -> 6,
      "July" -> 7,
      "August" -> 8,
      "September" -> 9,
      "October" -> 10,
      "November" -> 11,
      "December" -> 12
    )
    val dayMonth = name.stripPrefix("Fixtures").trim.split(" ")

    DateTime.now().withDayOfMonth(dayMonth(0).toInt).withMonthOfYear(months(dayMonth(1))).withTimeAtStartOfDay().withZone(DateTimeZone.UTC)
  }
}


