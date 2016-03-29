package core.autotrader

import akka.actor.FSM.{CurrentState, Transition, UnsubscribeTransitionCallBack, SubscribeTransitionCallBack}
import akka.actor._
import core.autotrader.AutoTrader.{StrategyStopped, StrategyStateChange, StrategyStarted, AutoTraderOutput}
import core.autotrader.Runner.Finished
import core.eventBus.{MessageEvent, EventBus}
import server.Configuration

class Monitor(config: Configuration, eventBus: EventBus, subject: ActorRef, marketId: String, selectionId: Long, handicap: Double, subjectId: String) extends Actor {

  override def preStart() ={
    subject ! SubscribeTransitionCallBack(self)
    context watch subject
  }

  def broadcast(output: AutoTraderOutput): Unit = {
    eventBus.publish(MessageEvent(
      config.getAutoTraderUpdateChannel(Some(subjectId)),
      output,
      self
    ))
  }

  override def receive = {
    case CurrentState(actorRef, stateName) =>
      broadcast(StrategyStarted(marketId, selectionId, handicap, subjectId, stateName.toString))
    case Transition(actorRef, oldState, newState) if newState == Finished =>
      subject ! PoisonPill
    case Terminated(ref) =>
      broadcast(StrategyStopped(marketId, selectionId, handicap, subjectId))
    case Transition(actorRef, oldState, newState) =>
      broadcast(StrategyStateChange(marketId, selectionId, handicap, subjectId, oldState.toString, newState.toString))
    case x => println("AUTO TRADER MONITOR received: ", x)
  }

  override def postStop() = {
    subject ! UnsubscribeTransitionCallBack(subject)
  }
}

object Monitor {
  def props(config: Configuration, eventBus: EventBus, subject: ActorRef, marketId: String, selectionId: Long, handicap: Double, subjectId: String) =
    Props(new Monitor(config, eventBus, subject, marketId, selectionId, handicap, subjectId))
}

