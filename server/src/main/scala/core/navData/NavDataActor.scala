package core.navData

import akka.actor.{Actor, Props}
import core.api.commands.GetNavigationData
import core.api.output.NavDataUpdated
import core.navData.NavDataActor.Update
import core.eventBus.{EventBus, MessageEvent}
import play.api.libs.json.Json
import server.Configuration
import service.BetfairService

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class NavDataActor(config: Configuration, sessionToken: String, betfairService: BetfairService, eventBus: EventBus) extends Actor with SoccerNavData with HorseRacingNavData with NavDataUtils {

  import context._

  var timer = context.system.scheduler.schedule(1 hour, 1 hour, self, Update)             // TODO Get interval from config
  var soccerData: SoccerData = _
  var horseRacingData: HorseRacingData = _

  def updateNavData(): Unit = {
    println("updating nav data")
    val navData = restrictToExchange(
      Json.parse(Await.result(betfairService.getNavigationData(sessionToken), Duration.Inf)).validate[NavData].get,
      Set("1", "Multiple")                                                                // TODO Get exchange id from config
    )
    println("got nav data from betfair")
    soccerData = getSoccerData(navData)
    horseRacingData = getHorseRacingData(navData)

    eventBus.publish(MessageEvent(
      config.systemAlertsChannel,
      NavDataUpdated,
      self
    ))
  }

  override def preStart(): Unit = {
    super.preStart()
    updateNavData()
  }

  override def receive = {
    case GetNavigationData(eventTypeId) => eventTypeId match {
      case "1" =>
        println("sending soccer nav data")
        sender ! soccerData
      case "7" =>
        println("sending horse racing nav data")
        sender ! horseRacingData
      case _ => println("request for unknown nav data")  // Do Nothing
    }
    case Update =>
      val (oldSoccerData, oldHorseRacingData) = (soccerData, horseRacingData)
      try {
        updateNavData()
      } catch {
        case _: Exception =>                // If the update fails continue using the old data
          soccerData = oldSoccerData
          horseRacingData = oldHorseRacingData
      }

  }
}

object NavDataActor {
  case object Update
  def props(config: Configuration, sessionToken: String, betfairService: BetfairService, eventBus: EventBus) = Props(new NavDataActor(config, sessionToken, betfairService, eventBus))
}

