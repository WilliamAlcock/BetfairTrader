package domain

import org.joda.time.DateTime
import play.api.libs.json._

case class Runner(selectionId: Long,
                  handicap: Double,
                  status: String,
                  adjustmentFactor: Option[Double] = None,
                  lastPriceTraded: Option[Double] = None,
                  totalMatched: Option[Double] = None,
                  removalDate: Option[DateTime] = None,
                  sp: Option[StartingPrices] = None,
                  ex: Option[ExchangePrices] = None,
                  orders: Option[Set[Order]] = None,
                  matches: Option[Set[Match]] = None) {

  lazy val uniqueId: String = Runner.getUniqueId(selectionId, handicap)
  lazy val backPrice: Option[Double] = Runner.getBackPrice(ex)
  lazy val layPrice: Option[Double] = Runner.getLayPrice(ex)
  lazy val position: Position = Position.fromOrders(orders)
  lazy val hedgeStake: Double = Runner.getHedgeStake(position.backReturn - position.layLiability, layPrice, backPrice)
  lazy val hedge: Double = Runner.getHedge(hedgeStake, position.sumBacked, position.sumLaid)

  def getWeightOfMoney: Double = ex match {
    case Some(x) =>
      val backSize = x.availableToBack.map(_.size).sum
      val totalSize = backSize + x.availableToLay.map(_.size).sum
      if (totalSize == 0.0) totalSize else backSize / totalSize
    case None => 0.0
  }
}

object Runner {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val dateTimeReads = Reads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = Writes.optionWithNull(Writes.jodaDateWrites(dateFormat))
  implicit val readsRunner = Json.reads[Runner]
  implicit val writesRunner: Writes[Runner] = new Writes[Runner] {
    def writes(r: Runner) = Json.obj(
      "selectionId" -> JsNumber(r.selectionId),
      "handicap" -> JsNumber(r.handicap),
      "status" -> JsString(r.status),
      "adjustmentFactor" -> Json.toJson(r.adjustmentFactor),
      "lastPriceTraded" -> Json.toJson(r.lastPriceTraded),
      "totalMatched" -> Json.toJson(r.totalMatched),
      "removalDate" -> Json.toJson(r.removalDate),
      "sp" -> Json.toJson(r.sp),
      "ex" -> Json.toJson(r.ex),
      "orders" -> Json.toJson(r.orders),
      "matches" -> Json.toJson(r.matches),
      "uniqueId" -> JsString(r.uniqueId),
      "backPrice" -> Json.toJson(r.backPrice),
      "layPrice" -> Json.toJson(r.layPrice),
      "position" -> Json.toJson(r.position),
      "hedgeStake" -> Json.toJson(r.hedgeStake),
      "hedge" -> Json.toJson(r.hedge)
    )
  }

  def getHedge(hedgeStake: Double, sumBacked: Double, sumLaid: Double): Double = hedgeStake match {
    case x if x != 0 => BigDecimal(hedgeStake - (sumBacked - sumLaid)).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
    case x => 0.0
  }

  def getHedgeStake(excessReturn: Double, layPrice: Option[Double], backPrice: Option[Double]): Double = excessReturn match {
    case x if x > 0 && layPrice.isDefined && layPrice.get != 0.0 =>
      BigDecimal(x / layPrice.get).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
    case x if x < 0 && backPrice.isDefined && backPrice.get != 0.0 =>
      BigDecimal(x / backPrice.get).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
    case x => 0.0
  }

  def getBackPrice(ex: Option[ExchangePrices]): Option[Double] = ex match {
    case Some(x) if x.availableToBack.nonEmpty => Some(x.availableToBack.head.price)
    case _ => None
  }

  def getLayPrice(ex: Option[ExchangePrices]): Option[Double]  = ex match {
    case Some(x) if x.availableToLay.nonEmpty => Some(x.availableToLay.head.price)
    case _ => None
  }

  def getUniqueId(selectionId: Long, handicap: Double): String = selectionId.toString + "-" + handicap.toString
}