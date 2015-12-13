package core.dataProvider.polling

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

class MarketRefSpec extends FlatSpec with Matchers {

  val ref1 = "TestRef1"
  val ref2 = "TestRef2"

  // Testing add subscribers

  val addSubscribers = Table(
    ("subscribers",                                   "pollingGroup" ),

    (Seq(BEST),                                       Seq(BEST)),
    (Seq(ALL),                                        Seq(ALL)),
    (Seq(BEST, ALL),                                  Seq(BEST, ALL)),
    (Seq(BEST_AND_TRADED),                            Seq(BEST_AND_TRADED)),
    (Seq(BEST, BEST_AND_TRADED),                      Seq(BEST, BEST_AND_TRADED)),
    (Seq(ALL, BEST_AND_TRADED),                       Seq(ALL, ALL_AND_TRADED)),
    (Seq(BEST, ALL, BEST_AND_TRADED),                 Seq(BEST, ALL, ALL_AND_TRADED)),
    (Seq(BEST, ALL, BEST_AND_TRADED, ALL_AND_TRADED), Seq(BEST, ALL, ALL_AND_TRADED, ALL_AND_TRADED))
  )

  "marketRef" should "add subscribers and recalculate pollingGroup" in {
    forAll (addSubscribers) { (subscribers: Seq[PollingGroup], pollingGroups: Seq[PollingGroup]) =>
      val data = subscribers zip pollingGroups

      var market = new MarketRef

      // Adding subscribers and asserting group
      data.foreach (x => {
        market = market.addSubscriber(ref1, x._1)
        market.pollingGroup should be (x._2)
      })
    }
  }

  // Testing remove subscribers

  val removeSubscribers = Table(
    ("subscribers",                                   "subscriberToRemove",                             "pollingGroups" ),

    (Seq(BEST),                                       Seq(BEST),                                        Seq(EMPTY)),
    (Seq(ALL),                                        Seq(ALL),                                         Seq(EMPTY)),
    (Seq(BEST, ALL),                                  Seq(BEST, ALL),                                   Seq(ALL, EMPTY)),
    (Seq(BEST, BEST_AND_TRADED),                      Seq(BEST, BEST_AND_TRADED),                       Seq(BEST_AND_TRADED, EMPTY)),
    (Seq(ALL, BEST_AND_TRADED),                       Seq(ALL, BEST_AND_TRADED),                        Seq(BEST_AND_TRADED, EMPTY)),
    (Seq(BEST, ALL, BEST_AND_TRADED),                 Seq(ALL, BEST_AND_TRADED),                        Seq(BEST_AND_TRADED, BEST)),
    (Seq(BEST, ALL, BEST_AND_TRADED, ALL_AND_TRADED), Seq(ALL_AND_TRADED, ALL, BEST_AND_TRADED),        Seq(ALL_AND_TRADED, BEST_AND_TRADED, BEST))
  )

  "marketRef" should "remove subscribers and recalculate pollingGroup" in {
    forAll(removeSubscribers) { (subscribers: Seq[PollingGroup], subscribersToRemove: Seq[PollingGroup], pollingGroups: Seq[PollingGroup]) =>
      val data = subscribersToRemove zip pollingGroups

      var market = new MarketRef

      // Adding initial subscribers
      subscribers.foreach(x =>
        market = market.addSubscriber(ref1, x)
      )

      // Removing subscribers and asserting group
      data.foreach(x => {
        market = market.removeSubscriber(ref1, x._1)
        market.pollingGroup should be(x._2)
      })
    }
  }

  // Testing remove all subscriptions for an actor ref
  val removeAllSubscribers = Table(
    ("subscribers",                                   "refToRemove",      "pollingGroup" ),

    (List(
      Subscription(ref1, BEST),
      Subscription(ref2, ALL),
      Subscription(ref2, BEST_AND_TRADED)),           ref2,               BEST),
    (List(
      Subscription(ref1, BEST)),                      ref2,               BEST),
    (List(
      Subscription(ref1, BEST)),                      ref1,               EMPTY)
  )

  "marketRef" should "remove all subscribers and recalculate pollingGroup" in {
    forAll(removeAllSubscribers) { (subscriptions: List[Subscription], refToRemove: String, pollingGroup: PollingGroup) =>

      var market = new MarketRef(subscribers = subscriptions)

      market = market.removeSubscriber(refToRemove)

      market.pollingGroup should be(pollingGroup)
    }
  }
}