package core.eventBus

import akka.actor.ActorRef
import akka.event.SubchannelClassification
import akka.util.Subclassification

// TODO remove use of Any

class EventBus extends akka.event.EventBus with SubchannelClassification {
  override type Event = MessageEvent
  override type Classifier = String
  override type Subscriber = ActorRef

  override protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event.message
  }

  override protected def classify(event: Event): Classifier = event.channel

  override protected def subclassification = new Subclassification[Classifier] {
    override def isEqual(x: Classifier, y: Classifier) = x == y
    // is y subclass of x ?
    override def isSubclass(x: Classifier, y: Classifier) = (y.split("/"), x.split("/")) match {
      case (_y, _x) if _y.size > _x.size => false
      case (_y, _x) =>
        (_y zip _x).map {case (a, b) => a == b || a == "*" || b == "*"}.forall(x => x)
      case _ => false
    }
  }
}