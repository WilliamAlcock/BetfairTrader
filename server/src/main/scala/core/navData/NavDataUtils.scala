package core.navData

trait NavDataUtils {
  def getEventType(navData: NavData, eventTypeId: String): Option[EventType] = navData match {
    case x: Group => x.children.find(getEventType(_, eventTypeId).isDefined).map(_.asInstanceOf[EventType])
    case x: EventType if x.id == eventTypeId => Some(x)
    case _ => None
  }

  def restrictToExchange(navData: NavData, exchangeIds: Set[String]): NavData = navData match {
    case x: EventType => x.copy(children = x.children.filter(x => exchangeIds.contains(x.exchangeId)).map(restrictToExchange(_, exchangeIds)))
    case x: Group     => x.copy(children = x.children.filter(x => exchangeIds.contains(x.exchangeId)).map(restrictToExchange(_, exchangeIds)))
    case x: Event     => x.copy(children = x.children.filter(x => exchangeIds.contains(x.exchangeId)).map(restrictToExchange(_, exchangeIds)))
    case x: Race      => x.copy(children = x.children.filter(x => exchangeIds.contains(x.exchangeId)).map(restrictToExchange(_, exchangeIds)))
    case x: Market    => x
  }
}
