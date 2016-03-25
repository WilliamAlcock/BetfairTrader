package core.autotrader

import akka.actor.{Props, ActorRef, Actor}
import akka.actor.FSM.{UnsubscribeTransitionCallBack, Transition, CurrentState, SubscribeTransitionCallBack}
import core.autotrader.AutoTrader.{StrategyStateChange, StrategyStarted, AutoTraderOutput}
import core.eventBus.{EventBus, MessageEvent}
import server.Configuration

class Monitor(config: Configuration, eventBus: EventBus, subject: ActorRef, marketId: String, selectionId: Long, handicap: Double, subjectId: String) extends Actor {

  override def preStart() ={
    SubscribeTransitionCallBack(subject)
  }

  def broadcast(output: AutoTraderOutput): Unit = {
    eventBus.publish(MessageEvent(
      config.getAutoTraderUpdateChannel(Some(subjectId)),
      output,
      self
    ))
  }

  override def receive = {
    case CurrentState(actorRef, stateName) => broadcast(StrategyStarted(marketId, selectionId, handicap, subjectId, stateName.toString))
    case Transition(actorRef, oldState, newState) => broadcast(StrategyStateChange(marketId, selectionId, handicap, subjectId, oldState.toString, newState.toString))
    case x => println("AUTO TRADER MONITOR received: ", x)
  }

  override def postStop() = {
    UnsubscribeTransitionCallBack(subject)
  }
}

object Monitor {
  def props(config: Configuration, eventBus: EventBus, subject: ActorRef, marketId: String, selectionId: Long, handicap: Double, subjectId: String) =
    Props(new Monitor(config, eventBus, subject, marketId, selectionId, handicap, subjectId))
}

