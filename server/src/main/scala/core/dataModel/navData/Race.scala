package core.dataModel.navData

import org.joda.time.DateTime

case class Race(children: List[NavData],
                override val id: String,
                override val name: String,
                startTime: DateTime,
                venue: String,
                raceNumber: String,
                countryCode: String) extends NavData {
  override def getType: String = "RACE"
  override lazy val hasChildren: Boolean = !children.isEmpty
  override lazy val numberOfMarkets: Int = children.foldLeft[Int](0)((acc: Int, x: NavData) => acc + x.numberOfMarkets)
  override lazy val hasGroupChildren: Boolean = false
  override lazy val hasGroupGrandChildren: Boolean = false
  override val exchangeId: String = children.foldLeft[String](null)((acc: String, x: NavData) =>
    if (acc == null)
      x.exchangeId
    else if
      (acc == "Multiple" || acc == x.exchangeId) acc
    else "Multiple"
  )
}

