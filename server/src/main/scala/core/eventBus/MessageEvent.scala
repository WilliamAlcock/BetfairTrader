package core.eventBus

case class MessageEvent(channel: String, message: Message)
