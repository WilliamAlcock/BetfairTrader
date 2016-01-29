package core.dataStore.csvReader

import scala.collection.immutable.HashMap
import scala.io.BufferedSource
import scala.io.Source._

trait CSVReader {
  def read(s: BufferedSource): HashMap[String, EventData]
}

object FracSoftCSVReader extends CSVReader {
  override def read(s: BufferedSource): HashMap[String, EventData] = {
    s.getLines().foldLeft(HashMap.empty[String, EventData])((data, x) => {
      val csv = CSVData.fromCSVString(x)
      data + (csv.eventId -> data.getOrElse(csv.eventId, EventData()).addCSV(csv))
    })
  }
}

object CSVReader extends App {
  val data = FracSoftCSVReader.read(fromFile("/Users/Alcock/Desktop/horse_racing_example.csv"))

  val testEvent = data.keys.head
  val testMarket = data(testEvent).markets.keys.tail.head
//  data(testEvent).markets(testMarket).runners.foreach(println(_))
//  data(testEvent).markets(testMarket).printSortedData()
//  println(data(testEvent).markets(testMarket).getMarketBookUpdates().head) //foreach(println(_))
//
//  println(data(data.keys.head).markets.keys.head)
//
//  for each market in each event build a series of marketBook
}

