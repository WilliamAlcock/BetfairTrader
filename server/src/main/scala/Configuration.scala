package server

case class Configuration(appKey: String,
                         username: String,
                         password: String,
                         apiUrl: String,
                         isoUrl: String,
                         navUrl: String,
                         systemAlertsChannel: String        = "systemAlerts",
                         marketUpdateChannel: String        = "marketUpdates",
                         navDataInstructions: String        = "navDataInstructions",
                         dataModelInstructions: String      = "dataModelInstructions",
                         orderManagerInstructions: String   = "orderManagerInstructions",
                         dataProviderInstructions: String   = "dataProviderInstructions") {

  def getPublishChannel(ids: Seq[String]): String = ids.foldLeft[String](marketUpdateChannel)((channel, id) => channel + "/" + id)
}