package core.autotrader

import akka.actor.{PoisonPill, Actor, ActorRef, Props}
import core.api.commands.{StartStrategy, StopStrategy}
import core.api.output.Output
import core.autotrader.AutoTrader.{AutoTraderException, StrategyCreated}
import core.autotrader.layTheDraw.LayTheDrawConfig
import core.eventBus.EventBus
import play.api.libs.json.Json
import server.Configuration

trait Strategy
trait StrategyConfig

class AutoTrader(config: Configuration, controller: ActorRef, eventBus: EventBus) extends Actor {

  sealed case class StrategyKey(marketId: String, selectionId: Long, handicap: Double)
  sealed case class StrategyActors(strategyId: String, strategy: ActorRef, monitor: ActorRef)

  var runningStrategies = Map.empty[StrategyKey, StrategyActors]
  var nextStrategyId: Int = 0

  def strategyId(): String = {
    nextStrategyId = nextStrategyId + 1
    "Strategy_" + (nextStrategyId - 1)
  }

  def getStrategy(sc: StrategyConfig): ActorRef = sc match {
    case x: LayTheDrawConfig => context.actorOf(layTheDraw.LayTheDraw.props(controller, x))
  }

  override def receive = {
    case StartStrategy(marketId, selectionId, handicap, layTheDrawConfig) =>
      val key = StrategyKey(marketId, selectionId, handicap)
      runningStrategies.get(key) match {
        case Some(x) => sender() ! AutoTraderException("Strategy already running on market")
        case None =>
          val id = strategyId()
          val strategy = getStrategy(layTheDrawConfig)
          val monitor = context.actorOf(Monitor.props(config, eventBus, strategy, marketId, selectionId, handicap, id))
          runningStrategies = runningStrategies + (key -> StrategyActors(id, strategy, monitor))
          sender() ! StrategyCreated(marketId, selectionId, handicap, id)
    }
    case StopStrategy(marketId, selectionId, handicap) =>
      val key = StrategyKey(marketId, selectionId, handicap)
      runningStrategies.get(key) match {
        case Some(x) => x.strategy ! PoisonPill
        case None =>
      }

  }
}

object AutoTrader {
  def props(config: Configuration, controller: ActorRef, eventBus: EventBus) = Props(new AutoTrader(config, controller, eventBus))

  trait AutoTraderOutput extends Output {
    val marketId: String
    val selectionId: Long
    val handicap: Double
    val strategyId: String
  }

  final case class StrategyCreated(marketId: String, selectionId: Long, handicap: Double, strategyId: String) extends AutoTraderOutput
  final case class StrategyStarted(marketId: String, selectionId: Long, handicap: Double, strategyId: String, state: String) extends AutoTraderOutput
  final case class StrategyStateChange(marketId: String, selectionId: Long, handicap: Double, strategyId: String, oldState: String, newState: String) extends AutoTraderOutput
  final case class StrategyStopped(marketId: String, selectionId: Long, handicap: Double, strategyId: String) extends AutoTraderOutput

  final case class AutoTraderException(message: String) extends Throwable

  implicit val formatStrategyCreated = Json.format[StrategyCreated]
  implicit val formatStrategyStarted = Json.format[StrategyStarted]
  implicit val formatStrategyStateChange = Json.format[StrategyStateChange]
  implicit val formatStrategyStopped = Json.format[StrategyStopped]
}
