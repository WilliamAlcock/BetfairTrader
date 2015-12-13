package core.api.output

import domain.EventResult

case class EventUpdate(data: EventResult) extends Output
