package dbIO

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

trait DBConnector {
  import scala.concurrent.ExecutionContext.Implicits.global

  // TODO get these values from config
  val driver: MongoDriver = new MongoDriver()
  val connection: MongoConnection = driver.connection(List("localhost:27017"))

  def getDB(name: String): DefaultDB = connection(name)

  def getCollection(db: DefaultDB, name: String): BSONCollection = db.collection[BSONCollection](name)
}

object DBConnector extends DBConnector {
  sys.addShutdownHook(connection.close())
  sys.addShutdownHook(driver.close())
}