package randomForest

import play.api.libs.json.Json

case class Instance(features: List[Double], label: String)

object Instance {
  implicit val formatInstance = Json.format[Instance]
}