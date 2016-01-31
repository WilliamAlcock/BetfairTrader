package core.dataStore

import play.api.libs.json.Json

case class OID($oid: String)

object OID {
  implicit val oidFormat = Json.format[OID]
}

