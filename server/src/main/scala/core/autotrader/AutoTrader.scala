package core.autotrader

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import core.api.commands.{ListRunningStrategies, StartStrategy, StopStrategy, SubscribeToAutoTraderUpdates}
import core.api.output.Output
import core.autotrader.AutoTrader._
import core.autotrader.Runner.Init
import core.eventBus.EventBus
import play.api.libs.json.Json
import server.Configuration

class AutoTrader(config: Configuration, controller: ActorRef, eventBus: EventBus) extends Actor {

  sealed case class StrategyKey(marketId: String, selectionId: Long, handicap: Double)
  sealed case class StrategyActors(strategyId: String, strategy: ActorRef, monitor: ActorRef, state: String = "")

  var runningStrategies = Map.empty[StrategyKey, StrategyActors]
  var nextStrategyId: Int = 0

  def strategyId(): String = {
    nextStrategyId = nextStrategyId + 1
    "Strategy_" + (nextStrategyId - 1)
  }

  override def preStart() = {
    controller ! SubscribeToAutoTraderUpdates()
  }

  override def receive = {
    case ListRunningStrategies =>
      runningStrategies.foreach{ case (key, data) => sender() ! StrategyStarted(key.marketId, key.selectionId, key.handicap, data.strategyId, data.state)}

    case StrategyStarted(marketId, selectionId, handicap, strategyId, _state) =>
      val key = StrategyKey(marketId, selectionId, handicap)
      runningStrategies.get(key) match {
        case Some(x) => runningStrategies = runningStrategies + (key -> x.copy(state = _state))
        case _ => // Do Nothing
      }

    case StrategyStateChange(marketId, selectionId, handicap, strategyId, oldState, newState) =>
      val key = StrategyKey(marketId, selectionId, handicap)
      runningStrategies.get(key) match {
        case Some(x) => runningStrategies = runningStrategies + (key -> x.copy(state = newState))
        case _ => // Do Nothing
      }

    case StrategyStopped(marketId, selectionId, handicap, strategyId) =>
      runningStrategies -= StrategyKey(marketId, selectionId, handicap)

    case StartStrategy(marketId, selectionId, handicap, strategyConfig) =>
      val key = StrategyKey(marketId, selectionId, handicap)
      runningStrategies.get(key) match {
        case Some(x) => sender() ! AutoTraderException("Strategy already running on market")
        case None =>
          val id = strategyId()
          val strategy = context.actorOf(Runner.props(controller))
          val monitor = context.actorOf(Monitor.props(config, eventBus, strategy, marketId, selectionId, handicap, id))
          runningStrategies = runningStrategies + (key -> StrategyActors(id, strategy, monitor))
          strategy ! Init(strategyConfig.getStrategy(marketId, selectionId, handicap))
          sender() ! StrategyCreated(marketId, selectionId, handicap, id)
    }
    case StopStrategy(marketId, selectionId, handicap) =>
      val key = StrategyKey(marketId, selectionId, handicap)
      runningStrategies.get(key) match {
        case Some(x) => x.strategy ! PoisonPill
        case None => sender() ! AutoTraderException("No strategy running on market")
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
