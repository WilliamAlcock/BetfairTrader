import dbIO.DataUtils

case class Config(mode: String = "",
                  file: String = "",
                  db: String = "",
                  interval: Long = 0L,
                  inputName: String = "",
                  outputName: String = "",
                  inplay: Boolean = false,
                  minsBefore: Option[Int] = None,
                  numberOfTrees: Int = 0,
                  leafSize: Int = 0,
                  features: Int = 0,
                  numIndicators: Int = 0)

object Main extends DataUtils {
  val parser = new scopt.OptionParser[Config]("scopt") {
    head("Betfair Trader DataUtils", "0.1")

    cmd("readFileToDB") action { (_, c) =>
      c.copy(mode = "readFile")
    } text "read a csv file and save to the database." children(
      arg[String]("<file>") required() action { (x, c) => c.copy(file = x) } text "file to read"
    )

    cmd("buildIndexes") action { (_, c) =>
      c.copy(mode = "buildIndexes")
    } text "index markets on selectionId and timestamp" children(
      arg[String]("<db name>") required() action { (x, c) => c.copy(db = x) } text "database name"
    )

    cmd("buildCatalogues") action { (_, c) =>
      c.copy(mode = "buildCatalogues")
    } text "build market catalogues for the given database" children(
      arg[String]("<db name>") required() action { (x, c) => c.copy(db = x) } text "database name"
    )

    cmd("writeIndicators") action { (_, c) =>
      c.copy(mode = "writeIndicators")
    } text "build intervals from tick data" children(
      arg[String]("<db name>") required() action { (x, c) => c.copy(db = x) } text "database name",
      arg[Long]("<interval>") required() action { (x, c) => c.copy(interval = x) } text "interval length in milliseconds",
      arg[String]("<new db postfix>") required() action { (x, c) => c.copy(outputName = x) } text "output database postfix"
    )

    cmd("buildDataSet") action { (_, c) =>
      c.copy(mode = "buildDataSet")
    } text "build training and test data from the contents of a db" children(
      opt[Unit]("<inplay>") abbr("ip") action { (x, c) => c.copy(inplay = false)} text "use inplay intervals",
      arg[String]("<db name>") required() action { (x, c) => c.copy(db = x) } text "database name",
      arg[String]("<input collections prefix>") required() action { (x, c) => c.copy(inputName = x) } text "collections prefix",
      arg[String]("<output name>") required() action { (x, c) => c.copy(outputName = x) } text "output collections name",
      arg[Int]("<minsBefore>") optional() action { (x, c) => c.copy(minsBefore = Some(x)) } text "minutes before race to take intervals"
    )

    cmd("trainClassifier") action { (_, c) =>
      c.copy(mode = "trainClassifier")
    } text "trains a classifier using the given dataset and saves the resulting model" children(
      arg[String]("<db name>") required() action { (x, c) => c.copy(db = x) } text "database name",
      arg[String]("<training set>") required() action { (x, c) => c.copy(inputName = x) } text "training set collection name",
      arg[String]("<modelName>") required() action { (x, c) => c.copy(outputName = x) } text "The name to call the resulting model",
      arg[Int]("<leaf size>") required() action { (x, c) => c.copy(leafSize= x) } text "final leaf size",
      arg[Int]("<# features to split>") required() action { (x, c) => c.copy(features = x) } text "number of features to split on",
      arg[Int]("<# trees>") required() action { (x, c) => c.copy(numberOfTrees = x) } text "number of trees in forest"
    )

    cmd("testClassifier") action { (_, c) =>
      c.copy(mode = "testClassifier")
    } text "tests a classifier using the given dataset" children(
      arg[String]("<db name>") required() action { (x, c) => c.copy(db = x) } text "database name",
      arg[String]("<testing set>") required() action { (x, c) => c.copy(inputName = x) } text "training set collection name",
      arg[String]("<modelName>") required() action { (x, c) => c.copy(outputName = x) } text "The name to call the resulting model"
    )

    cmd("testClassifierPnL") action { (_, c) =>
      c.copy(mode = "testClassifierPnL")
    } text "tests a classifier using the given dataset" children(
      opt[Unit]("<inplay>") abbr("ip") action { (x, c) => c.copy(inplay = false)} text "use inplay intervals",
      arg[String]("<db name>") required() action { (x, c) => c.copy(db = x) } text "database name",
      arg[String]("<testing set>") required() action { (x, c) => c.copy(inputName = x) } text "training set collection name",
      arg[String]("<modelName>") required() action { (x, c) => c.copy(outputName = x) } text "The name to call the resulting model",
      arg[Int]("<minsBefore>") optional() action { (x, c) => c.copy(minsBefore = Some(x)) } text "minutes before race to take intervals",
      arg[Int]("<numIndicators>") optional() action { (x, c) => c.copy(numIndicators = x) } text "number of consecutive indicators"
    )
  }

  def main(args: Array[String]) = {
    parser.parse(args, Config()) match {
      case Some(config) => config.mode match {
        case "readFile" => readFileToDB(config.file)
        case "buildIndexes" => buildIndexes(config.db)
        case "buildCatalogues" => buildCatalogues(config.db)
        case "writeIndicators" => writeIntervals(config.db, config.interval, config.outputName)
        case "buildDataSet" => buildDataSet(config.db, config.inputName, config.outputName, config.inplay, config.minsBefore)
        case "trainClassifier" => trainClassifier(config.db, config.inputName, config.outputName, config.leafSize, config.features, config.numberOfTrees)
        case "testClassifier" => testClassifier(config.db, config.inputName, config.outputName)
        case "testClassifierPnL" => testClassifierPnL(config.db, config.inputName, config.outputName, config.inplay, config.minsBefore, config.numIndicators)
        case _ => // should never reach this point
      }
      case None =>
        println("No Matching Arguments")

      // arguments are bad, error message will have been displayed
    }
    // TODO find out why application does not exit at the point
    println("FINISHED !")
  }
}