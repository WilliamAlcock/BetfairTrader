package core

import org.scalatest.prop.Tables.Table
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._
import server.Configuration
import scala.concurrent.duration._
import scala.language.postfixOps

class ConfigurationSpec extends FlatSpec with Matchers {

  val marketId = "TEST_MARKET_ID"
  val strategyId = "TEST_STRATEGY_ID"

  val testConfig = Configuration(
    appKey                    = "TEST_APP_KEY",
    username                  = "TEST_USERNAME",
    password                  = "TEST_PASSWORD",
    apiUrl                    = "TEST_API_URL",
    isoUrl                    = "TEST_ISO_URL",
    navUrl                    = "TEST_NAV_URL",
    orderManagerUpdateInterval = 1 second,
    systemAlertsChannel       = "TEST_SYSTEM_ALERTS",
    marketUpdateChannel       = "TEST_MARKET_UPDATES",
    orderUpdateChannel        = "TEST_ORDER_UPDATES",
    autoTraderChannel         = "TEST_AUTO_TRADER_UPDATES",
    navDataInstructions       = "TEST_NAV_DATA_INSTRUCTIONS",
    dataModelInstructions     = "TEST_DATA_MODEL_INSTRUCTIONS",
    orderManagerInstructions  = "TEST_ORDER_MANAGER_INSTRUCTIONS",
    dataProviderInstructions  = "TEST_DATA_PROVIDER_INSTRUCTIONS"
  )

  val getOrderUpdateChannel_Cases = Table(
    ("marketId",          "selectionId",  "handicap", "output"),

    (Some(marketId),      Some(1L),       Some(1.0),  "TEST_ORDER_UPDATES/TEST_MARKET_ID/1/1.0"),
    (None,                Some(1L),       Some(1.0),  "TEST_ORDER_UPDATES/*/1/1.0"),
    (Some(marketId),      None,           Some(1.0),  "TEST_ORDER_UPDATES/TEST_MARKET_ID/*/1.0"),
    (Some(marketId),      Some(1L),       None,       "TEST_ORDER_UPDATES/TEST_MARKET_ID/1"),
    (None,                None,           Some(1.0),  "TEST_ORDER_UPDATES/*/*/1.0"),
    (Some(marketId),      None,           None,       "TEST_ORDER_UPDATES/TEST_MARKET_ID"),
    (None,                Some(1L),       None,       "TEST_ORDER_UPDATES/*/1"),
    (None,                None,           None,       "TEST_ORDER_UPDATES")
  )

  forAll(getOrderUpdateChannel_Cases) {(marketId: Option[String], selectionId: Option[Long], handicap: Option[Double], output: String) =>
    "Configuration.getOrderUpdateChannel given " + marketId + " " + selectionId + " " + handicap should "return " + output in {
      testConfig.getOrderUpdateChannel(marketId, selectionId, handicap) should be (output)
    }
  }

  val getMarketUpdateChannel_Cases = Table(
    ("marketId",          "output"),

    (Some(marketId),      "TEST_MARKET_UPDATES/TEST_MARKET_ID"),
    (None,                "TEST_MARKET_UPDATES")
  )

  forAll(getMarketUpdateChannel_Cases) {(marketId: Option[String], output: String) =>
    "Configuration.getMarketUpdateChannel given " + marketId should "return " + output in {
      testConfig.getMarketUpdateChannel(marketId) should be (output)
    }
  }

  val getAutoTraderUpdateChannel_Cases = Table(
    ("strategyId",          "output"),

    (Some(strategyId),      "TEST_AUTO_TRADER_UPDATES/TEST_STRATEGY_ID"),
    (None,                  "TEST_AUTO_TRADER_UPDATES")
  )

  forAll(getAutoTraderUpdateChannel_Cases) {(strategyId: Option[String], output: String) =>
    "Configuration.getAutoTraderUpdates given " + strategyId should "return " + output in {
      testConfig.getAutoTraderUpdateChannel(strategyId) should be (output)
    }
  }
}

