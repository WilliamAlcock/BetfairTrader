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
    override def isSubclass(x: Classifier, y: Classifier) = {
      val xClasses = x.split("/")
      val yClasses = y.split("/")
      xClasses.size == yClasses.size &&
      (xClasses zip yClasses).map {case (a, b) => a == b || a == "*" || b == "*"}.foldLeft(true)((a, b) => a && b)
    }
  }
}