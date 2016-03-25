package core.autotrader

import akka.actor.{Stash, FSM}

trait Data

case object NoData extends Data
case object GotData extends Data

trait State

case object Idle extends State
case object Running extends State

case object Start
case object Message
case object Stop

class FooTest(test: String) extends FSM[State, Data] with Stash {

  startWith(Idle, NoData)

  when(Idle) {
    case Event(Start, NoData) =>
      println("GOING TO RUNNING")
      goto(Running)
    case Event(_, _) =>
      stash()
      stay()
  }

  onTransition {
    case Idle -> Running => unstashAll()
  }

  when(Running) {
    case Event(Message, _) =>
      println("I HAVE THE MESSAGE")
      stay() using GotData
    case Event(Stop, _) => goto(Idle) using NoData
  }
}