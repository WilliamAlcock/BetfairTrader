package core.dataModel.navData

import org.joda.time.DateTime

case class EventType(children: List[NavData],
                     override val id: String,
                     override val name: String) extends NavData {
  override def getType: String = "EVENT_TYPE"
  override lazy val hasChildren: Boolean = !children.isEmpty
  override lazy val numberOfMarkets: Int = children.foldLeft[Int](0)((acc: Int, x: NavData) => acc + x.numberOfMarkets)
  override lazy val countryCode: String = children.foldLeft[String](null)(
    (acc: String, x: NavData) => if (acc == null || acc == x.countryCode) x.countryCode else acc)
  override lazy val hasGroupChildren: Boolean = children.foldLeft[Boolean](false)((acc: Boolean, x: NavData) => acc || x.getType == "GROUP" )
  override lazy val hasGroupGrandChildren: Boolean = children.foldLeft[Boolean](false)((acc: Boolean, x: NavData) => acc || x.hasGroupChildren)
  override lazy val startTime: DateTime = children.foldLeft[DateTime](null)((acc: DateTime, x: NavData) =>
    if (acc == null || x.startTime.getMillis() < acc.getMillis()) x.startTime else acc
  )
}