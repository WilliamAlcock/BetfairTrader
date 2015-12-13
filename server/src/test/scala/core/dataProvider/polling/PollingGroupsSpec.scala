package core.dataProvider.polling

import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.collection.immutable.HashMap

class PollingGroupsSpec extends FlatSpec with Matchers with MockFactory with BeforeAndAfterEach {

  class MockableMarketRef extends MarketRef() {
    def mockPollingGroup():PollingGroup = BEST
    override lazy val pollingGroup = mockPollingGroup()
  }

  var market1, market2, market3, market4, market5, market6: MockableMarketRef = _

  override def beforeEach() = {
    market1 = mock[MockableMarketRef]
    market2 = mock[MockableMarketRef]
    market3 = mock[MockableMarketRef]
    market4 = mock[MockableMarketRef]
    market5 = mock[MockableMarketRef]
    market6 = mock[MockableMarketRef]
  }

  "pollingGroups" should "aggregate polling groups" in {
    (market1.mockPollingGroup _).expects().returns(BEST)
    (market2.mockPollingGroup _).expects().returns(ALL)
    (market3.mockPollingGroup _).expects().returns(BEST_AND_TRADED)
    (market4.mockPollingGroup _).expects().returns(ALL_AND_TRADED)
    (market5.mockPollingGroup _).expects().returns(ALL)
    (market6.mockPollingGroup _).expects().returns(BEST_AND_TRADED)

    val pollingGroup = PollingGroups(HashMap(
      "1" -> market1,
      "2" -> market2,
      "3" -> market3,
      "4" -> market4,
      "5" -> market5,
      "6" -> market6
    ))

    pollingGroup.pollingGroups should be (HashMap(
      BEST -> Set("1"),
      ALL -> Set("2", "5"),
      BEST_AND_TRADED -> Set("3", "6"),
      ALL_AND_TRADED -> Set("4")
    ))
  }

  "pollingGroups.addSubscriber" should "call addSubscriber on the correct market and update market" in {

    val ref1 = "TestRef1"

    val pollingGroup = PollingGroups(HashMap(
      "1" -> market1,
      "2" -> market2,
      "3" -> market3,
      "4" -> market4,
      "5" -> market5,
      "6" -> market6
    ))

    val market7 = mock[MockableMarketRef]
    (market7.mockPollingGroup _).expects().returns(ALL)
    (market2.mockPollingGroup _).expects().returns(ALL)
    (market3.mockPollingGroup _).expects().returns(BEST_AND_TRADED)
    (market4.mockPollingGroup _).expects().returns(ALL_AND_TRADED)
    (market5.mockPollingGroup _).expects().returns(ALL)
    (market6.mockPollingGroup _).expects().returns(BEST_AND_TRADED)

    (market1.addSubscriber _).expects(ref1, ALL).returns(market7)

    pollingGroup.addSubscriber("1", ref1, ALL).pollingGroups should be (HashMap(
      ALL -> Set("1", "2", "5"),
      BEST_AND_TRADED -> Set("3", "6"),
      ALL_AND_TRADED -> Set("4")
    ))
  }

  "pollingGroups.removeSubscribers" should "call removeSubscriber on the correct market and update market" in {

    val ref1 = "TestRef1"

    val pollingGroup = PollingGroups(HashMap(
      "1" -> market1,
      "2" -> market2,
      "3" -> market3,
      "4" -> market4,
      "5" -> market5,
      "6" -> market6
    ))

    val market7 = mock[MockableMarketRef]
    (market7.mockPollingGroup _).expects().returns(ALL)
    (market2.mockPollingGroup _).expects().returns(ALL)
    (market3.mockPollingGroup _).expects().returns(BEST_AND_TRADED)
    (market4.mockPollingGroup _).expects().returns(ALL_AND_TRADED)
    (market5.mockPollingGroup _).expects().returns(ALL)
    (market6.mockPollingGroup _).expects().returns(BEST_AND_TRADED)

    (market1.removeSubscriber(_: String, _: PollingGroup)).expects(ref1, ALL).returns(market7)

    pollingGroup.removeSubscriber("1", ref1, ALL).pollingGroups should be (HashMap(
      ALL -> Set("1", "2", "5"),
      BEST_AND_TRADED -> Set("3", "6"),
      ALL_AND_TRADED -> Set("4")
    ))
  }

  "pollingGroups.removeSubscribers" should "call removeSubscriber on the correct market and remove market if its new group is None" in {
    val ref1 = "TestRef1"

    val pollingGroup = PollingGroups(HashMap(
      "1" -> market1,
      "2" -> market2,
      "3" -> market3,
      "4" -> market4,
      "5" -> market5,
      "6" -> market6
    ))

    val market7 = mock[MockableMarketRef]
    (market7.mockPollingGroup _).expects().returns(EMPTY)
    (market2.mockPollingGroup _).expects().returns(ALL)
    (market3.mockPollingGroup _).expects().returns(BEST_AND_TRADED)
    (market4.mockPollingGroup _).expects().returns(ALL_AND_TRADED)
    (market5.mockPollingGroup _).expects().returns(ALL)
    (market6.mockPollingGroup _).expects().returns(BEST_AND_TRADED)

    (market1.removeSubscriber(_: String, _: PollingGroup)).expects(ref1, ALL).returns(market7)

    pollingGroup.removeSubscriber("1", ref1, ALL).pollingGroups should be (HashMap(
      ALL -> Set("2", "5"),
      BEST_AND_TRADED -> Set("3", "6"),
      ALL_AND_TRADED -> Set("4")
    ))
  }

  "pollingGroups.removeSubscribers" should "call removeSubscriber on the all markets and update market when called with only an Actorref" in {

    val ref1 = "TestRef1"

    val pollingGroup = PollingGroups(HashMap(
      "1" -> market1,
      "2" -> market2,
      "3" -> market3,
      "4" -> market4,
      "5" -> market5,
      "6" -> market6
    ))

    val market7 = mock[MockableMarketRef]
    val market8 = mock[MockableMarketRef]
    val market9 = mock[MockableMarketRef]
    val market10 = mock[MockableMarketRef]
    val market11 = mock[MockableMarketRef]
    val market12 = mock[MockableMarketRef]

    (market7.mockPollingGroup _).expects().returns(EMPTY)
    (market8.mockPollingGroup _).expects().returns(EMPTY)
    (market9.mockPollingGroup _).expects().returns(BEST_AND_TRADED)
    (market10.mockPollingGroup _).expects().returns(ALL_AND_TRADED)
    (market11.mockPollingGroup _).expects().returns(EMPTY)
    (market12.mockPollingGroup _).expects().returns(BEST_AND_TRADED)

    (market1.removeSubscriber(_: String)).expects(ref1).returns(market7)
    (market2.removeSubscriber(_: String)).expects(ref1).returns(market8)
    (market3.removeSubscriber(_: String)).expects(ref1).returns(market9)
    (market4.removeSubscriber(_: String)).expects(ref1).returns(market10)
    (market5.removeSubscriber(_: String)).expects(ref1).returns(market11)
    (market6.removeSubscriber(_: String)).expects(ref1).returns(market12)

    pollingGroup.removeSubscriber(ref1).pollingGroups should be (HashMap(
      BEST_AND_TRADED -> Set("3", "6"),
      ALL_AND_TRADED -> Set("4")
    ))
  }

  "pollingGroups.removeAllSubscribers" should "return an empty object" in {
    val pollingGroup = PollingGroups(HashMap(
      "1" -> market1,
      "2" -> market2,
      "3" -> market3,
      "4" -> market4,
      "5" -> market5,
      "6" -> market6
    ))

    pollingGroup.removeAllSubscribers().pollingGroups should be (HashMap.empty[PollingGroup, String])
  }
}