package core.api.output

import core.dataModel.navData.NavData
import domain.ListCompetitionsContainer

case class NavigationDataUpdate(data: NavData, competitions: ListCompetitionsContainer) extends Output
