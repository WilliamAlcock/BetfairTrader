package server

case class Configuration(appKey: String,
                         username: String,
                         password: String,
                         apiUrl: String,
                         isoUrl: String,
                         navUrl: String,
                         marketUpdateChannel: String        = "marketUpdates",
                         dataModelInstructions: String      = "dataModelInstructions",
                         orderManagerInstructions: String   = "orderManagerInstructions",
                         dataProviderInstructions: String   = "dataProviderInstructions") {

  def getPublishChannel(ids: Seq[String]): String = ids.foldLeft[String](marketUpdateChannel)((channel, id) => channel + "/" + id)
}