import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import core.Controller
import core.autotrader.AutoTrader
import core.dataModel.DataModelActor
import core.dataProvider.DataProvider
import core.eventBus.EventBus
import core.navData.NavDataActor
import core.orderManager.OrderManager
import server.Configuration
import service.simService.{SimOrderBook, SimService}
import service.{BetfairServiceNGCommand, BetfairServiceNGException}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main {

  def main(args: Array[String]) {
    // create the system and the dispatcher
    implicit val system = ActorSystem("BFTrader")
    implicit val executionContext = system.dispatcher

    // TODO handle deadletters

    // load and create the config object
    val conf = ConfigFactory.load()
    val appKey = conf.getString("betfairService.appKey")
    val username = conf.getString("betfairService.username")
    val password = conf.getString("betfairService.password")
    val apiUrl = conf.getString("betfairService.apiUrl")
    val isoUrl = conf.getString("betfairService.isoUrl")
    val navUrl = conf.getString("betfairService.navUrl")
    val config = new Configuration(appKey, username, password, apiUrl, isoUrl, navUrl)

    // create command, api and event bus objects
    val betfairServiceNGCommand = new BetfairServiceNGCommand(config)
//    val betfairService = new BetfairServiceNG(config, betfairServiceNGCommand)
    // create simService
    val simOrderBook = system.actorOf(SimOrderBook.props(), "simOrderBook")
    val betfairService = new SimService(config, betfairServiceNGCommand, simOrderBook)

    // Login
    val sessionTokenFuture = betfairService.login().map {
      case Some(loginResponse) => loginResponse.token
      case _ => throw new BetfairServiceNGException("no session token")
    }
    val sessionToken = Await.result(sessionTokenFuture, 10 seconds)

    // Start Actors
    val eventBus = new EventBus

    val navDataActor = system.actorOf(NavDataActor.props(config, sessionToken, betfairService, eventBus), "navDataActor")
    val dataModelActor = system.actorOf(DataModelActor.props(config, eventBus), "dataModel")
    val dataProvider = system.actorOf(DataProvider.props(config, sessionToken, betfairService, eventBus), "dataProvider")

    val controller = system.actorOf(Props(new Controller(config, eventBus)), "controller")
    val orderManager = system.actorOf(OrderManager.props(config, sessionToken, controller, betfairService, eventBus), "orderManager")
    val autoTrader = system.actorOf(AutoTrader.props(config, controller, eventBus), "autoTrader")

    // Subscribe to event bus
    eventBus.subscribe(navDataActor, config.navDataInstructions)
    eventBus.subscribe(dataModelActor, config.dataModelInstructions)
    eventBus.subscribe(dataProvider, config.dataProviderInstructions)
    eventBus.subscribe(orderManager, config.orderManagerInstructions)
    eventBus.subscribe(autoTrader, config.autoTraderInstructions)

    // DataStoreWriter
//    system.actorOf(DataStoreWriter.props(config, controller), "dataStoreWriter")
  }
}