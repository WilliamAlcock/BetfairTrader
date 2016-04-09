import csvReader.CSVReader

case class Config(mode: String = "",
                  file: String = "",
                  db: String = "",
                  interval: Long = 0L,
                  trainingStr: String = "",
                  testingStr: String = "",
                  inplay: Boolean = false,
                  minsBefore: Option[Int] = None,
                  numberOfTrees: Int = 0,
                  leafSize: Int = 0,
                  features: Int = 0)

object Main {
  val parser = new scopt.OptionParser[Config]("scopt") {
    head("Betfair Trader DataUtils", "0.1")

    cmd("readFile") action { (_, c) =>
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
      arg[String]("<new db postfix>") required() action { (x, c) => c.copy(file = x) } text "output database postfix"
    )

    cmd("buildDataSet") action { (_, c) =>
      c.copy(mode = "buildDataSet")
    } text "build training and test data from the contents of a db" children(
      opt[Unit]("<inplay>") abbr("ip") action { (x, c) => c.copy(inplay = false)} text "use inplay intervals",
      arg[String]("<db name>") required() action { (x, c) => c.copy(db = x) } text "database name",
      arg[String]("<training set>") required() action { (x, c) => c.copy(trainingStr = x) } text "training collections prefix",
      arg[String]("<testing set>") required() action { (x, c) => c.copy(testingStr = x) } text "testing collections prefix",
      arg[String]("<output postfix>") required() action { (x, c) => c.copy(file = x) } text "output collections, postfix",
      arg[Int]("<minsBefore>") optional() action { (x, c) => c.copy(minsBefore = Some(x)) } text "minutes before race to take intervals"
    )

    cmd("crossValidateClassifier") action { (_, c) =>
      c.copy(mode = "crossValidateClassifier")
    } text "trains and tests a classifier using crossValidation on a dataset" children(
      arg[String]("<db name>") required() action { (x, c) => c.copy(db = x) } text "database name",
      arg[String]("<training set>") required() action { (x, c) => c.copy(file = x) } text "training set collection name",
      arg[Int]("<leaf size>") required() action { (x, c) => c.copy(leafSize= x) } text "final leaf size",
      arg[Int]("<# features to split>") required() action { (x, c) => c.copy(features = x) } text "number of features to split on",
      arg[Int]("<# trees>") required() action { (x, c) => c.copy(numberOfTrees = x) } text "number of trees in forest"
    )

    cmd("trainAndTestClassifier") action { (_, c) =>
      c.copy(mode = "trainAndTestClassifier")
    } text "trains and tests a classifier using crossValidation on a dataset" children(
      arg[String]("<db name>") required() action { (x, c) => c.copy(db = x) } text "database name",
      arg[String]("<training set>") required() action { (x, c) => c.copy(file = x) } text "training set collection name",
      arg[String]("<testing set>") required() action { (x, c) => c.copy(testingStr = x) } text "testing set collection name",
      arg[Int]("<leaf size>") required() action { (x, c) => c.copy(leafSize= x) } text "final leaf size",
      arg[Int]("<# features to split>") required() action { (x, c) => c.copy(features = x) } text "number of features to split on",
      arg[Int]("<# trees>") required() action { (x, c) => c.copy(numberOfTrees = x) } text "number of trees in forest"
      )
  }

  def main(args: Array[String]) = {
    parser.parse(args, Config()) match {
      case Some(config) => config.mode match {
        case "readFile" => new CSVReader().readFile(config.file)
        case "buildIndexes" => new CSVReader().buildIndexes(config.db)
        case "buildCatalogues" => new CSVReader().buildCatalogues(config.db)
        case "writeIndicators" => new CSVReader().writeIndicators(config.db, config.interval, config.file)
        case "buildDataSet" => new CSVReader().buildTrainingAndTestSets(config.db, config.trainingStr, config.testingStr, config.file, config.inplay, config.minsBefore)
        case "crossValidateClassifier" => new CSVReader().crossValidateClassifier(config.db, config.file, config.leafSize, config.features, config.numberOfTrees)
        case "trainAndTestClassifier" => new CSVReader().trainAndTestClassifier(config.db, config.file, config.testingStr, config.leafSize, config.features, config.numberOfTrees)
        case _ => // should never reach this point
      }
      case None =>
      // arguments are bad, error message will have been displayed
    }
    // TODO find out why application does not exit at the point
    println("FINISHED !")
  }
}