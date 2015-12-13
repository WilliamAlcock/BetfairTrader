package service

import core.dataProvider.polling._
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

class MarketSpec extends FlatSpec with Matchers with MockFactory {

//  val ref = mock[ActorRef]

  val addSubscribers = Table(
    ("subscribers",                                   "pollingGroup" ),

    (Seq(BEST),                                       Seq(BEST)),
    (Seq(ALL),                                        Seq(ALL)),
    (Seq(BEST, ALL),                                  Seq(BEST, ALL)),
    (Seq(BEST_AND_TRADED),                            Seq(BEST_AND_TRADED)),
    (Seq(BEST, BEST_AND_TRADED),                      Seq(BEST, BEST_AND_TRADED)),
    (Seq(ALL, BEST_AND_TRADED),                       Seq(ALL, ALL_AND_TRADED)),
    (Seq(BEST, ALL, BEST_AND_TRADED),                 Seq(BEST, ALL, ALL_AND_TRADED)),
    (Seq(BEST, ALL, BEST_AND_TRADED, ALL_AND_TRADED), Seq(BEST, ALL, BEST_AND_TRADED, ALL_AND_TRADED)),
  )

  forAll (addSubscribers) { (subscribers: Seq[PollingGroup], pollingGroups: Seq[PollingGroup]) =>
    val data = subscribers zip pollingGroups

    val market = new Market

    data.foreach (x => {
      market.addSubscriber(null, x._1)
      market.getPollingGroup() should be (x._2)
    })
  }
}