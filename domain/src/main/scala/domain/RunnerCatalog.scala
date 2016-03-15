package domain

import play.api.libs.json.{JsString, JsNumber, Writes, Json}

case class RunnerCatalog(selectionId: Long,
                         runnerName: String,
                         handicap: Double,
                         sortPriority: Option[Int] = None,
                         metadata: Option[Map[String, String]] = None,
                         totalMatched: Option[Double] = None) {
  val uniqueId = Runner.getUniqueId(selectionId, handicap)
}

object RunnerCatalog {
  implicit val readsRunnerCatalog = Json.reads[RunnerCatalog]
  implicit val writesRunnerCatalog: Writes[RunnerCatalog] = {
    new Writes[RunnerCatalog] {
      def writes(r: RunnerCatalog) = Json.obj(
        "selectionId" -> JsNumber(r.selectionId),
        "runnerName" -> JsString(r.runnerName),
        "handicap" -> JsNumber(r.handicap),
        "sortPriority" -> Json.toJson(r.sortPriority),
        "metadata" -> Json.toJson(r.metadata),
        "totalMatched" -> Json.toJson(r.totalMatched),
        "uniqueId" -> JsString(r.uniqueId)
      )
    }
  }
}