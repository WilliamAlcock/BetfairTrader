package core.navData

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

import scala.io.Source

class HorseRacingNavDataSpec extends FlatSpec with Matchers with HorseRacingNavData {
  val navData = Json.parse(Source.fromFile("server/src/test/scala/core/navData/navData.json").getLines().mkString).validate[NavData].get

//  "Horse Racing Nav Data" should "get fixtures" in {
//    //val data = getSoccerData(navData)
////    val horseRacingData = getHorseRacing(navData)
////    val data = getHorseRacingData(horseRacingData.get)
}