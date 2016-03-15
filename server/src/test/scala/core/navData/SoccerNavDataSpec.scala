package core.navData

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

import scala.io.Source

class SoccerNavDataSpec extends FlatSpec with Matchers with SoccerNavData {

  val navData = Json.parse(Source.fromFile("server/src/test/scala/core/dataModel/navData/navData.json").getLines().mkString).validate[NavData].get

//  "Soccer Nav Data" should "get fixtures" in {
//    //val data = getSoccerData(navData)
//    val soccer = getSoccer(navData)
//    val data = getSoccerData(navData)
//  }
}
