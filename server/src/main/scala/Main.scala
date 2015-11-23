import akka.actor.{ActorSystem, Props}
import core.Controller
import core.dataModel.DataModelActor
import core.dataProvider.DataProvider
import core.eventBus.EventBus
import core.orderManager.OrderManager
import server.Configuration
import service.{BetfairServiceNG, BetfairServiceNGCommand, BetfairServiceNGException}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main {

  def main(args: Array[String]) {
    // create the system and the dispatcher
    implicit val system = ActorSystem("BFTrader")
    implicit val executionContext = system.dispatcher

    // TODO handle deadletters

    // TODO get these from config
    val ORDER_MANAGER_CHANNEL = "orderManagerInstructions"
    val DATA_PROVIDER_CHANNEL = "dataProviderInstructions"
    val DATA_PROVIDER_OUTPUT_CHANNEL = "dataProviderOutput"

    // load and create the config object
    val conf = ConfigFactory.load()
    val appKey = conf.getString("betfairService.appKey")
    val username = conf.getString("betfairService.username")
    val password = conf.getString("betfairService.password")
    val apiUrl = conf.getString("betfairService.apiUrl")
    val isoUrl = conf.getString("betfairService.isoUrl")
    val config = new Configuration(appKey, username, password, apiUrl, isoUrl)

    // create command, api and event bus objects
    val betfairServiceNGCommand = new BetfairServiceNGCommand(config)
    val betfairServiceNG = new BetfairServiceNG(config, betfairServiceNGCommand)

    // Login
    val sessionTokenFuture = betfairServiceNG.login.map {
      case Some(loginResponse) => loginResponse.token
      case _ => throw new BetfairServiceNGException("no session token")
    }

    val sessionToken = Await.result(sessionTokenFuture, 10 seconds)

    // Start Actors
    val eventBus = new EventBus
    val dataModelActor = system.actorOf(Props(new DataModelActor(eventBus)), "dataModel")
    val dataProvider = system.actorOf(Props(new DataProvider(config, sessionToken, betfairServiceNG, eventBus)), "dataProvider")
    val orderManager = system.actorOf(Props(new OrderManager(config, sessionToken, betfairServiceNG, eventBus)), "orderManager")

    // Subscribe to event bus
    eventBus.subscribe(dataModelActor, DATA_PROVIDER_OUTPUT_CHANNEL)
    eventBus.subscribe(dataProvider, DATA_PROVIDER_CHANNEL)
    eventBus.subscribe(orderManager, ORDER_MANAGER_CHANNEL)

    system.actorOf(Props(new Controller(config, eventBus)), "controller")
  }
}