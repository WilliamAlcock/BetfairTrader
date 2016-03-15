package core.navData

import org.joda.time.DateTime
import play.api.libs.json.Json

case class Group(children: List[NavData],
                 override val id: String,
                 override val name: String) extends NavData {
  override def getType: String = "GROUP"
  override lazy val hasChildren: Boolean = !children.isEmpty
  override lazy val numberOfMarkets: Int = children.foldLeft[Int](0)((acc: Int, x: NavData) => acc + x.numberOfMarkets)
  override lazy val countryCode: String = children.foldLeft[String](null)(
    (acc: String, x: NavData) => if (acc == null || acc == x.countryCode) x.countryCode else acc)
  override lazy val startTime: DateTime = children.foldLeft[DateTime](null)((acc: DateTime, x: NavData) =>
    if (acc == null || x.startTime.getMillis() < acc.getMillis()) x.startTime else acc
  )
  override val exchangeId: String = children.foldLeft[String](null)((acc: String, x: NavData) =>
    if (acc == null) x.exchangeId else if (acc == "Multiple" || acc == x.exchangeId) acc else "Multiple"
  )
}

object Group {
  implicit val formatGroup = Json.format[Group]
}
