package core

import akka.actor._
import akka.testkit._
import core.api.commands._
import core.dataProvider.polling.BEST
import core.eventBus.{EventBus, MessageEvent}
import domain.{MarketFilter, MarketSort}
import org.scalamock.function.{MockFunction1, MockFunction2}
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterEach, FlatSpecLike, Matchers}
import server.Configuration

import scala.concurrent.duration._
import scala.language.postfixOps

class ControllerSpec extends TestKit(ActorSystem("TestSystem")) with FlatSpecLike with Matchers with BeforeAndAfterEach with MockFactory with ImplicitSender {

  var eventBus: EventBus = _
  var controller: TestActorRef[Controller] = _
  var sender: TestProbe = _

  var _publish: MockFunction1[MessageEvent, Unit] = _
  var _unsubscribe: MockFunction1[ActorRef, Unit] = _
  var _unsubscribe_from: MockFunction2[ActorRef, String, Boolean] = _
  var _subscribe: MockFunction2[ActorRef, String, Boolean] = _

  var testConfig: Configuration = _

  val config = Configuration(
    appKey                    = "TEST_APP_KEY",
    username                  = "TEST_USERNAME",
    password                  = "TEST_PASSWORD",
    apiUrl                    = "TEST_API_URL",
    isoUrl                    = "TEST_ISO_URL",
    navUrl                    = "TEST_NAV_URL",
    orderManagerUpdateInterval = 1 second,
    systemAlertsChannel       = "TEST_SYSTEM_ALERTS",
    marketUpdateChannel       = "TEST_MARKET_UPDATES",
    orderUpdateChannel        = "TEST_ORDER_UPDATES",
    navDataInstructions       = "TEST_NAV_DATA_INSTRUCTIONS",
    dataModelInstructions     = "TEST_DATA_MODEL_INSTRUCTIONS",
    orderManagerInstructions  = "TEST_ORDER_MANAGER_INSTRUCTIONS",
    dataProviderInstructions  = "TEST_DATA_PROVIDER_INSTRUCTIONS"
  )

  override def beforeEach(): Unit = {
    sender = TestProbe()
    _publish = mockFunction[MessageEvent, Unit]
    _unsubscribe = mockFunction[ActorRef, Unit]
    _unsubscribe_from = mockFunction[ActorRef, String, Boolean]
    _subscribe = mockFunction[ActorRef, String, Boolean]

    class TestEventBus extends EventBus {
      override def publish(m: MessageEvent) = _publish.apply(m)
      override def unsubscribe(a: ActorRef, c: String) = _unsubscribe_from.apply(a, c)
      override def unsubscribe(a: ActorRef) = _unsubscribe.apply(a)
      override def subscribe(a: ActorRef, c: String) = _subscribe.apply(a, c)
    }

    eventBus = new TestEventBus

    // Creating a mock using the values from the original config so assertions can be made on the method calls
    class MockConfig extends Configuration(
      config.appKey,
      config.username,
      config.password,
      config.apiUrl,
      config.isoUrl,
      config.navUrl,
      config.orderManagerUpdateInterval,
      config.systemAlertsChannel,
      config.marketUpdateChannel,
      config.orderUpdateChannel,
      config.navDataInstructions,
      config.dataModelInstructions,
      config.orderManagerInstructions,
      config.dataProviderInstructions
    )

    testConfig = mock[MockConfig]
  }

  val command_Cases = Table(
    ("command",                                                        "outputChannel"),
    GetNavigationData("1")                                          -> config.navDataInstructions,
    ListEventTypes(MarketFilter())                                  -> config.dataProviderInstructions,
    ListEvents(MarketFilter())                                      -> config.dataProviderInstructions,
    ListMarketCatalogue(MarketFilter(), MarketSort.FIRST_TO_START)  -> config.dataProviderInstructions,
    StopPollingAllMarkets                                           -> config.dataProviderInstructions,
    PlaceOrders("1", Set.empty, None)                               -> config.orderManagerInstructions,
    CancelOrders("1", Set.empty, None)                              -> config.orderManagerInstructions,
    ReplaceOrders("1", Set.empty, None)                             -> config.orderManagerInstructions,
    UpdateOrders("1", Set.empty, None)                              -> config.orderManagerInstructions,
    ListCurrentOrders(Set.empty, Set.empty)                         -> config.orderManagerInstructions
  )

  forAll(command_Cases) { (command: Command, outputChannel: String) =>
    "Controller.receive -> " + command should "watch the sender for termination and forward the command to " + outputChannel in {
      val _watch = mockFunction[ActorRef, Unit]

      controller = TestActorRef(Props(new Controller(testConfig, eventBus) {
        override def watch(s: ActorRef) = _watch.apply(s)
      }))

      _watch.expects(sender.ref)
      _publish.expects(MessageEvent(outputChannel, command, sender.ref))

      sender.send(controller, command)
    }
  }

  "Controller.receive -> SubscribeToSystemAlerts" should "watch the sender for termination and subscribe the sender to the system alerts channel" in {
    val _watch = mockFunction[ActorRef, Unit]

    controller = TestActorRef(Props(new Controller(testConfig, eventBus) {
      override def watch(s: ActorRef) = _watch.apply(s)
    }))

    _watch.expects(sender.ref)
    _subscribe.expects(sender.ref, testConfig.systemAlertsChannel)

    sender.send(controller, SubscribeToSystemAlerts)
  }

  "Controller.receive -> SubscribeToMarkets" should "watch the sender for termination, subscribe the sender to the markets and request the current copy of the marketBook" in {
    val _watch = mockFunction[ActorRef, Unit]

    controller = TestActorRef(Props(new Controller(testConfig, eventBus) {
      override def watch(s: ActorRef) = _watch.apply(s)
    }))

    val markets = List("TEST_MARKET_1", "TEST_MARKET_2")
    val pollingGroup = BEST

    val command = SubscribeToMarkets(markets.toSet, pollingGroup)

    // Watch the sender for termination
    _watch.expects(sender.ref)

    // Subscribe the sender to the markets
    (testConfig.getMarketUpdateChannel _).expects(Seq(markets(0))).returns("MARKET_1_CHANNEL")
    (testConfig.getMarketUpdateChannel _).expects(Seq(markets(1))).returns("MARKET_2_CHANNEL")

    _subscribe.expects(sender.ref, "MARKET_1_CHANNEL")
    _subscribe.expects(sender.ref, "MARKET_2_CHANNEL")

    // publish changes to the dataProvider
    _publish.expects(MessageEvent(testConfig.dataProviderInstructions, command, sender.ref))

    // Request the current copy fo the marketBook
    _publish.expects(MessageEvent(testConfig.dataModelInstructions, ListMarketBook(markets.toSet), sender.ref))

    sender.send(controller, command)
  }

  "Controller.receive -> UnSubscribeFromMarkets" should "unsubscribe the sender from the given markets and publish the changes to the dataProvider" in {
    val _watch = mockFunction[ActorRef, Unit]

    controller = TestActorRef(Props(new Controller(testConfig, eventBus) {
      override def watch(s: ActorRef) = _watch.apply(s)
    }))

    val markets = List("TEST_MARKET_1", "TEST_MARKET_2")
    val pollingGroup = BEST

    val command = UnSubscribeFromMarkets(markets.toSet, pollingGroup)

    // Subscribe the sender to the markets
    (testConfig.getMarketUpdateChannel _).expects(Seq(markets(0))).returns("MARKET_1_CHANNEL")
    (testConfig.getMarketUpdateChannel _).expects(Seq(markets(1))).returns("MARKET_2_CHANNEL")

    _unsubscribe_from.expects(sender.ref, "MARKET_1_CHANNEL")
    _unsubscribe_from.expects(sender.ref, "MARKET_2_CHANNEL")

    // publish changes to the dataProvider
    _publish.expects(MessageEvent(testConfig.dataProviderInstructions, command, sender.ref))

    sender.send(controller, command)
  }

  "Controller.receive -> Terminated" should "unsubsribe the terminated actor from the eventbus and inform the dataProvider" in {
    val _watch = mockFunction[ActorRef, Unit]

    controller = TestActorRef(Props(new Controller(testConfig, eventBus) {
      override def watch(s: ActorRef) = _watch.apply(s)
    }))

    controller.underlyingActor.subscribers = Set(sender.ref)
    controller.watch(sender.ref)

    _unsubscribe.expects(sender.ref)
    _publish.expects(MessageEvent(config.dataProviderInstructions, UnSubscribe, sender.ref))

    sender.ref ! PoisonPill

    controller.underlyingActor.subscribers.isEmpty should be (true)
  }

  "Controller.watch" should "watch the subscriber and add it to the subscribers set if it is not already being watch" in {
    val _watch = mockFunction[ActorRef, Unit]

    controller = TestActorRef(Props(new Controller(testConfig, eventBus) {
      override def watch(s: ActorRef) = _watch.apply(s)
    }))

    controller.underlyingActor.watch(sender.ref)

    controller.underlyingActor.subscribers should be (Set(sender.ref))

    // Send a poison pill and assert unsubscribe has been called (this is an indication that the controller is watching the sender)
    _unsubscribe.expects(sender.ref)

    sender.ref ! PoisonPill
  }

  "Controller.watch" should "do nothing if the subscriber is already a member of the subscribers set" in {
    val _watch = mockFunction[ActorRef, Unit]

    controller = TestActorRef(Props(new Controller(testConfig, eventBus) {
      override def watch(s: ActorRef) = _watch.apply(s)
    }))

    controller.underlyingActor.subscribers = Set(sender.ref)

    controller.underlyingActor.watch(sender.ref)

    controller.underlyingActor.subscribers should be (Set(sender.ref))

    // Send a poison pill and assert nothing happens
    sender.ref ! PoisonPill
  }
}