package core.dataStore

import domain.MarketBook
import org.joda.time.DateTime
import play.api.libs.json.Json

case class Entry(timestamp: DateTime, marketBook: MarketBook)

object Entry {
  implicit def entryFormat = Json.format[Entry]
}


