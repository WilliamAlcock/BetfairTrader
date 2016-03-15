package domain

import play.api.libs.json.Json

case class ListCompetitionsContainer(result: List[CompetitionResult])

object ListCompetitionsContainer {
  implicit val formatCompetitionResultContainer = Json.format[ListCompetitionsContainer]
}