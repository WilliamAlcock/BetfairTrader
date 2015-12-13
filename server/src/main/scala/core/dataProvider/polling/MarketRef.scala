package core.dataProvider.polling

import domain.{PriceData, PriceProjection}
import play.api.libs.json._

trait PollingGroup {
  def getPriceProjection(): PriceProjection
  def +(that: PollingGroup): PollingGroup
  val maxMarkets: Int
}

object PollingGroup {
  implicit val readsPollingGroup = {
    new Reads[PollingGroup] {
      def reads(json: JsValue) = JsSuccess((json \ "group").getOrElse(JsString("")) match {
        case JsString("BEST")             => BEST
        case JsString("ALL")              => ALL
        case JsString("BEST_AND_TRADED")  => BEST_AND_TRADED
        case JsString("ALL_AND_TRADED")   => ALL_AND_TRADED
        case _                            => EMPTY
      })
    }
  }

  implicit val writesPollingGroup = {
    new Writes[PollingGroup] {
      def writes(p: PollingGroup) = p match {
        case BEST             => Json.obj("group" -> JsString("BEST"))
        case ALL              => Json.obj("group" -> JsString("ALL"))
        case BEST_AND_TRADED  => Json.obj("group" -> JsString("BEST_AND_TRADED"))
        case ALL_AND_TRADED   => Json.obj("group" -> JsString("ALL_AND_TRADED"))
        case _                => Json.obj("group" -> JsString(""))
      }
    }
  }
}

case object EMPTY extends PollingGroup {
  override def getPriceProjection(): PriceProjection = PriceProjection(priceData = Set.empty)
  override def +(that: PollingGroup): PollingGroup = that
  val maxMarkets = 0
}

case object BEST extends PollingGroup {
  override def getPriceProjection(): PriceProjection =
    PriceProjection(priceData = Set(PriceData.EX_BEST_OFFERS), virtualise = Some(true))
  override def +(that: PollingGroup): PollingGroup = {
    that match {
      case BEST             => BEST
      case ALL              => ALL
      case BEST_AND_TRADED  => BEST_AND_TRADED
      case ALL_AND_TRADED   => ALL_AND_TRADED
    }
  }
  override val maxMarkets = 200/5
}
case object ALL extends PollingGroup {
  override def getPriceProjection(): PriceProjection =
    PriceProjection(priceData = Set(PriceData.EX_ALL_OFFERS), virtualise = Some(true))
  override def +(that: PollingGroup): PollingGroup = {
    that match {
      case BEST_AND_TRADED  => ALL_AND_TRADED
      case ALL_AND_TRADED   => ALL_AND_TRADED
      case _                => ALL
    }
  }
  override val maxMarkets = 200/17
}
case object BEST_AND_TRADED extends PollingGroup {
  override def getPriceProjection(): PriceProjection =
    PriceProjection(priceData = Set(PriceData.EX_BEST_OFFERS, PriceData.EX_TRADED), virtualise = Some(true))
  override def +(that: PollingGroup): PollingGroup = {
    that match {
      case ALL              => ALL_AND_TRADED
      case ALL_AND_TRADED   => ALL_AND_TRADED
      case _                => BEST_AND_TRADED
    }
  }
  override val maxMarkets = 200/22
}
case object ALL_AND_TRADED extends PollingGroup {
  override def getPriceProjection(): PriceProjection =
    PriceProjection(priceData = Set(PriceData.EX_ALL_OFFERS, PriceData.EX_TRADED), virtualise = Some(true))
  override def +(that: PollingGroup): PollingGroup = ALL_AND_TRADED
  override val maxMarkets = 200/34
}

sealed case class Subscription(ref: String, pollingGroup: PollingGroup)

case class MarketRef(subscribers: List[Subscription] = List.empty) {

  def addSubscriber(ref: String, pollingGroup: PollingGroup): MarketRef = {
    this.copy(subscribers = Subscription(ref, pollingGroup) :: this.subscribers)
  }

  def removeSubscriber(ref: String, pollingGroup: PollingGroup): MarketRef = {
    subscribers.indexWhere((x: Subscription) => x.ref == ref && x.pollingGroup == pollingGroup) match {
      case x if x < 0 => this
      case x =>
        val (a, b) = subscribers.splitAt(x)
        this.copy(subscribers = a ++ b.tail)
    }
  }

  def removeSubscriber(ref: String): MarketRef = {
    this.copy(subscribers = subscribers.filter((x: Subscription) => x.ref != ref))
  }

  lazy val pollingGroup: PollingGroup = this.subscribers.foldLeft[PollingGroup](EMPTY)((acc, x) => acc + x.pollingGroup)
}

