package core.dataStore.dbWriter

import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.ExecutionContext.Implicits.global

trait DBConnector {
  // TODO get these values from config
  val driver = new MongoDriver()
  val connection = driver.connection(List("localhost:27017"))
  val db = connection("BetfairTrader")
  sys.addShutdownHook(connection.close())
  sys.addShutdownHook(driver.close())

  def getCollection(name: String): BSONCollection = db.collection[BSONCollection](name)
}


