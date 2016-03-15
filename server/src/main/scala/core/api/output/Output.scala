package core.api.output

import core.eventBus.Message

trait Output extends Message

case object NavDataUpdated extends Output

