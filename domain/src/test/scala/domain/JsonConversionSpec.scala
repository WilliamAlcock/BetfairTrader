package domain

/**
 * Created by Alcock on 11/11/2015.
 */

import java.util.Date

import org.joda.time.DateTime
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen, Properties}
import play.api.libs.json.Json

object JsonConversionSpec extends Properties("") {

  val genDateTime: Gen[DateTime] = for {
      d <- Arbitrary.arbitrary[Date]
  } yield new DateTime(d)

  val genCancelExecutionReport: Gen[CancelExecutionReport] = for {
    status <- Gen.oneOf(ExecutionReportStatus.values.toSeq)
    marketId <- Arbitrary.arbitrary[String]
    errorCode <- Gen.option(Gen.oneOf(ExecutionReportErrorCode.values.toSeq))
    instructionReports <- genCancelInstructionReport
    customerRef <- Gen.option(Arbitrary.arbitrary[String])
  } yield CancelExecutionReport(status, marketId, errorCode, Set(instructionReports), customerRef)

  property("CancelExecutionReport") = forAll(genCancelExecutionReport) { (a: CancelExecutionReport) =>
    Json.toJson(a).validate[CancelExecutionReport].get equals a
  }

  val genCancelExecutionReportContainer: Gen[CancelExecutionReportContainer] = for {
    result <- genCancelExecutionReport
  } yield CancelExecutionReportContainer(result)

  property("CancelExecutionReportContainer") = forAll(genCancelExecutionReportContainer) { (a: CancelExecutionReportContainer) =>
    Json.toJson(a).validate[CancelExecutionReportContainer].get equals a
  }

  val genCancelInstruction: Gen[CancelInstruction] = for {
    betId <- Arbitrary.arbitrary[String]
    sizeReduction <- Gen.option(Arbitrary.arbitrary[Double])
  } yield CancelInstruction(betId, sizeReduction)

  property("CancelInstruction") = forAll(genCancelInstruction) { (a: CancelInstruction) =>
    Json.toJson(a).validate[CancelInstruction].get equals a
  }

  val genCancelInstructionReport: Gen[CancelInstructionReport] = for {
    status <- Gen.oneOf(InstructionReportStatus.values.toSeq)
    errorCode <- Gen.option(Gen.oneOf(InstructionReportErrorCode.values.toSeq))
    instruction <- genCancelInstruction
    sizeCancelled <- Gen.option(Arbitrary.arbitrary[Double])
    cancelledDate <- Gen.option(genDateTime)
  } yield CancelInstructionReport(status, errorCode, instruction, sizeCancelled, cancelledDate)

  property("CancelInstructionReport") = forAll(genCancelInstructionReport) { (a: CancelInstructionReport) =>
    Json.toJson(a).validate[CancelInstructionReport].get equals a
  }

  val genClearedOrderSummary: Gen[ClearedOrderSummary] = for {
    eventTypeId <- Arbitrary.arbitrary[String]
    eventId <- Arbitrary.arbitrary[String]
    marketId <- Arbitrary.arbitrary[String]
    selectionId <- Arbitrary.arbitrary[Long]
    handicap <- Arbitrary.arbitrary[Double]
    betId <- Arbitrary.arbitrary[String]
    placeDate <- genDateTime
    persistenceType <- Gen.oneOf(PersistenceType.values.toSeq)
    orderType <- Gen.oneOf(OrderType.values.toSeq)
    side <- Gen.oneOf(Side.values.toSeq)
    itemDescription <- genItemDescription
    betOutcome <- Arbitrary.arbitrary[String]
    priceRequested <- Arbitrary.arbitrary[Double]
    settledDate <- genDateTime
    lastMatchedDate <- genDateTime
    betCount <- Arbitrary.arbitrary[Int]
    commission <- Arbitrary.arbitrary[Double]
    priceMatched <- Arbitrary.arbitrary[Double]
    priceReduced <- Arbitrary.arbitrary[Boolean]
    sizeSettled <- Arbitrary.arbitrary[Double]
    profit <- Arbitrary.arbitrary[Double]
    sizeCancelled <- Arbitrary.arbitrary[Double]
  } yield ClearedOrderSummary(eventTypeId, eventId , marketId , selectionId , handicap , betId , placeDate , persistenceType ,
      orderType , side , itemDescription , betOutcome , priceRequested , settledDate , lastMatchedDate , betCount , commission ,
      priceMatched , priceReduced , sizeSettled , profit , sizeCancelled)

  property("ClearedOrderSummary") = forAll(genClearedOrderSummary) { (a: ClearedOrderSummary) =>
    Json.toJson(a).validate[ClearedOrderSummary].get equals a
  }

  val genClearedOrderSummaryReport: Gen[ClearedOrderSummaryReport] = for {
    currentOrders <- genClearedOrderSummary
    moreAvailable <- Arbitrary.arbitrary[Boolean]
  } yield ClearedOrderSummaryReport(Set(currentOrders), moreAvailable)

  property("ClearedOrderSummaryReport") = forAll(genClearedOrderSummaryReport) { (a: ClearedOrderSummaryReport) =>
    Json.toJson(a).validate[ClearedOrderSummaryReport].get equals a
  }

  val genCompetition: Gen[Competition] = for {
    id <- Arbitrary.arbitrary[String]
    name <- Arbitrary.arbitrary[String]
  } yield Competition(id, name)

  property("Competition") = forAll(genCompetition) { (a: Competition) =>
    Json.toJson(a).validate[Competition].get equals a
  }

  val genCompetitionResult: Gen[CompetitionResult] = for {
    competition <- genCompetition
    marketCount <- Arbitrary.arbitrary[Int]
    competitionRegion <- Arbitrary.arbitrary[String]
  } yield CompetitionResult(competition, marketCount, competitionRegion)

  property("CompetitionResult") = forAll(genCompetitionResult) { (a: CompetitionResult) =>
    Json.toJson(a).validate[CompetitionResult].get equals a
  }

  val genCountryCodeResult: Gen[CountryCodeResult] = for {
    countryCode <- Arbitrary.arbitrary[String]
    marketCount <- Arbitrary.arbitrary[Int]
  } yield CountryCodeResult(countryCode, marketCount)

  property("CountryCodeResult") = forAll(genCountryCodeResult) { (a: CountryCodeResult) =>
    Json.toJson(a).validate[CountryCodeResult].get equals a
  }

  val genCurrentOrderSummary: Gen[CurrentOrderSummary] = for {
    betId <- Arbitrary.arbitrary[String]
    marketId <- Arbitrary.arbitrary[String]
    selectionId <- Arbitrary.arbitrary[Long]
    handicap <- Arbitrary.arbitrary[Double]
    priceSize <- genPriceSize
    bspLiability <- Arbitrary.arbitrary[Double]
    side <- Gen.oneOf(Side.values.toSeq)
    status <- Gen.oneOf(OrderStatus.values.toSeq)
    persistenceType <- Gen.oneOf(PersistenceType.values.toSeq)
    orderType <- Gen.oneOf(OrderType.values.toSeq)
    placedDate <- genDateTime
    matchedDate <- Gen.option(genDateTime)
    averagePriceMatched <- Arbitrary.arbitrary[Double]
    sizeMatched <- Arbitrary.arbitrary[Double]
    sizeRemaining <- Arbitrary.arbitrary[Double]
    sizeLapsed <- Arbitrary.arbitrary[Double]
    sizeCancelled <- Arbitrary.arbitrary[Double]
    sizeVoided <- Arbitrary.arbitrary[Double]
    regulatorCode <- Arbitrary.arbitrary[String]
  } yield CurrentOrderSummary(betId, marketId, selectionId, handicap, priceSize, bspLiability, side, status, persistenceType,
      orderType, placedDate, matchedDate, averagePriceMatched, sizeMatched, sizeRemaining, sizeLapsed, sizeCancelled, sizeVoided, regulatorCode)

  property("CurrentOrderSummary") = forAll(genCurrentOrderSummary) { (a: CurrentOrderSummary) =>
    Json.toJson(a).validate[CurrentOrderSummary].get equals a
  }

  val genCurrentOrderSummaryReport: Gen[CurrentOrderSummaryReport] = for {
    currentOrders <- genCurrentOrderSummary
    moreAvailable <- Arbitrary.arbitrary[Boolean]
  } yield CurrentOrderSummaryReport(Set(currentOrders), moreAvailable)

  property("CurrentOrderSummaryReport") = forAll(genCurrentOrderSummaryReport) { (a: CurrentOrderSummaryReport) =>
    Json.toJson(a).validate[CurrentOrderSummaryReport].get equals a
  }

  val genEvent: Gen[Event] = for {
    id <- Arbitrary.arbitrary[String]
    name <- Arbitrary.arbitrary[String]
    countryCode <- Gen.option(Arbitrary.arbitrary[String])
    timezone <- Arbitrary.arbitrary[String]
    venue <- Gen.option(Arbitrary.arbitrary[String])
    openDate <- genDateTime
  } yield Event(id, name, countryCode, timezone, venue, openDate)

  property("Event") = forAll(genEvent) { (a: Event) =>
    Json.toJson(a).validate[Event].get equals a
  }

  val genEventResult: Gen[EventResult] = for {
    event <- genEvent
    marketCount <- Arbitrary.arbitrary[Int]
  } yield EventResult(event, marketCount)

  property("EventResult") = forAll(genEventResult) { (a: EventResult) =>
    Json.toJson(a).validate[EventResult].get equals a
  }

  val genEventType: Gen[EventType] = for {
    id <- Arbitrary.arbitrary[String]
    name <- Arbitrary.arbitrary[String]
  } yield EventType(id, name)

  property("EventType") = forAll(genEventType) { (a: EventType) =>
    Json.toJson(a).validate[EventType].get equals a
  }

  val genEventTypeResult: Gen[EventTypeResult] = for {
    eventType <- genEventType
    marketCount <- Arbitrary.arbitrary[Int]
  } yield EventTypeResult(eventType, marketCount)

  property("EventTypeResult") = forAll(genEventTypeResult) { (a: EventTypeResult) =>
    Json.toJson(a).validate[EventTypeResult].get equals a
  }

  val genPriceSize: Gen[PriceSize] = for {
    price <- Arbitrary.arbitrary[Double]
    size <- Arbitrary.arbitrary[Double]
  } yield PriceSize(price, size)

  property("PriceSize") = forAll(genPriceSize) { (a: PriceSize) =>
    Json.toJson(a).validate[PriceSize].get equals a
  }

  val genExchangePrices: Gen[ExchangePrices] = for {
    availableToBack <- genPriceSize
    availableToLay <- genPriceSize
    tradedVolume <- genPriceSize
  } yield ExchangePrices(List(availableToBack), List(availableToBack), List(tradedVolume))

  property("ExchangePrices") = forAll(genExchangePrices) { (a: ExchangePrices) =>
    Json.toJson(a).validate[ExchangePrices].get equals a
  }

  val genItemDescription: Gen[ItemDescription] = for {
    eventTypeDesc <- Arbitrary.arbitrary[String]
    eventDesc <- Arbitrary.arbitrary[String]
    marketDesc <- Arbitrary.arbitrary[String]
    marketType <- Arbitrary.arbitrary[String]
    marketStartTime <- genDateTime
    runnerDesc <- Arbitrary.arbitrary[String]
    numberOfWinners <- Arbitrary.arbitrary[Int]
    eachWayDivisor <- Arbitrary.arbitrary[Double]
  } yield ItemDescription(eventTypeDesc, eventDesc, marketDesc, marketType, marketStartTime, runnerDesc, numberOfWinners, eachWayDivisor)

  property("ItemDescription") = forAll(genItemDescription) { (a: ItemDescription) =>
    Json.toJson(a).validate[ItemDescription].get equals a
  }

  val genLimitOnCloseOrder: Gen[LimitOnCloseOrder] = for {
    liability <- Arbitrary.arbitrary[Double]
    price <- Arbitrary.arbitrary[Double]
  } yield LimitOnCloseOrder(liability, price)

  property("LimitOnCloseOrder") = forAll(genLimitOnCloseOrder) { (a: LimitOnCloseOrder) =>
    Json.toJson(a).validate[LimitOnCloseOrder].get equals a
  }

  val genLimitOrder: Gen[LimitOrder] = for {
    size <- Arbitrary.arbitrary[Double]
    price <- Arbitrary.arbitrary[Double]
    persistenceType <- Gen.oneOf(PersistenceType.values.toSeq)
  } yield LimitOrder(size, price, persistenceType)

  property("LimitOrder") = forAll(genLimitOrder) { (a: LimitOrder) =>
    Json.toJson(a).validate[LimitOrder].get equals a
  }


  val genListClearedOrdersContainer: Gen[ListClearedOrdersContainer] = for {
    result <- genClearedOrderSummaryReport
  } yield ListClearedOrdersContainer(result)

  property("ListClearedOrdersContainer") = forAll(genListClearedOrdersContainer) { (a: ListClearedOrdersContainer) =>
    Json.toJson(a).validate[ListClearedOrdersContainer].get equals a
  }

  val genListCompetitionsContainer: Gen[ListCompetitionsContainer] = for {
    result <- Gen.listOf(genCompetitionResult)
  } yield ListCompetitionsContainer(result)

  property("ListCompetitionsContainer") = forAll(genListCompetitionsContainer) { (a: ListCompetitionsContainer) =>
    Json.toJson(a).validate[ListCompetitionsContainer].get equals a
  }

  val genListCountriesContainer: Gen[ListCountriesContainer] = for {
    result <- Gen.listOf(genCountryCodeResult)
  } yield ListCountriesContainer(result)

  property("ListCountriesContainer") = forAll(genListCountriesContainer) { (a: ListCountriesContainer) =>
    Json.toJson(a).validate[ListCountriesContainer].get equals a
  }

  val genListCurrentOrdersContainer: Gen[ListCurrentOrdersContainer] = for {
    result <- genCurrentOrderSummaryReport
  } yield ListCurrentOrdersContainer(result)

  property("ListCurrentOrdersContainer") = forAll(genListCurrentOrdersContainer) { (a: ListCurrentOrdersContainer) =>
    Json.toJson(a).validate[ListCurrentOrdersContainer].get equals a
  }

  val genListEventResultContainer: Gen[ListEventResultContainer] = for {
    result <- Gen.listOf(genEventResult)
  } yield ListEventResultContainer(result)

  property("ListEventResultContainer") = forAll(genListEventResultContainer) { (a: ListEventResultContainer) =>
    Json.toJson(a).validate[ListEventResultContainer].get equals a
  }

  val genListEventTypeResultContainer: Gen[ListEventTypeResultContainer] = for {
    result <- Gen.listOf(genEventTypeResult)
  } yield ListEventTypeResultContainer(result)

  property("ListEventTypeResultContainer") = forAll(genListEventTypeResultContainer) { (a: ListEventTypeResultContainer) =>
    Json.toJson(a).validate[ListEventTypeResultContainer].get equals a
  }

  val genOrder: Gen[Order] = for {
    betId <- Arbitrary.arbitrary[String]
    orderType <- Gen.oneOf(OrderType.values.toSeq)
    status <- Gen.oneOf(OrderStatus.values.toSeq)
    persistenceType <- Gen.oneOf(PersistenceType.values.toSeq)
    side <- Gen.oneOf(Side.values.toSeq)
    price <- Arbitrary.arbitrary[Double]
    size <- Arbitrary.arbitrary[Double]
    bspLiability <- Arbitrary.arbitrary[Double]
    placedDate <- genDateTime
    avgPriceMatched <- Arbitrary.arbitrary[Double]
    sizeMatched <- Arbitrary.arbitrary[Double]
    sizeRemaining <- Arbitrary.arbitrary[Double]
    sizeLapsed <- Arbitrary.arbitrary[Double]
    sizeCancelled <- Arbitrary.arbitrary[Double]
    sizeVoided <- Arbitrary.arbitrary[Double]
  } yield Order(betId, orderType, status, persistenceType, side , price, size, bspLiability, placedDate, avgPriceMatched,
      sizeMatched, sizeRemaining, sizeLapsed, sizeCancelled, sizeVoided)

  property("Order") = forAll(genOrder) { (a: Order) =>
    Json.toJson(a).validate[Order].get equals a
  }

  val genStartingPrices: Gen[StartingPrices] = for {
    nearPrice <- Arbitrary.arbitrary[Double]
    farPrice <- Arbitrary.arbitrary[Double]
    backStakeTaken <- genPriceSize
    layLiabilityTaken <- genPriceSize
    actualSP <- Arbitrary.arbitrary[Double]
  } yield StartingPrices(nearPrice, farPrice, Set(backStakeTaken), Set(layLiabilityTaken), actualSP)

  property("StartingPrices") = forAll(genStartingPrices) { (a: StartingPrices) =>
    Json.toJson(a).validate[StartingPrices].get equals a
  }

  val genMatch: Gen[Match] = for {
    betId <- Gen.option(Arbitrary.arbitrary[String])
    matchId <- Gen.option(Arbitrary.arbitrary[String])
    side <- Gen.oneOf(Side.values.toSeq)
    price <- Arbitrary.arbitrary[Double]
    size <- Arbitrary.arbitrary[Double]
    matchDate <- Gen.option(genDateTime)
  } yield Match(betId, matchId, side, price, size, matchDate)

  property("Match") = forAll(genMatch) { (a: Match) =>
    val jsVal = Json.toJson(a)
    val newObj  = jsVal.validate[Match].get

    newObj equals a
  }

  val genRunner: Gen[Runner] = for {
    selectionId <- Arbitrary.arbitrary[Long]
    handicap <- Arbitrary.arbitrary[Double]
    status <- Arbitrary.arbitrary[String]
    adjustmentFactor <- Gen.option(Arbitrary.arbitrary[Double])
    lastPriceTraded <- Gen.option(Arbitrary.arbitrary[Double])
    totalMatched <- Gen.option(Arbitrary.arbitrary[Double])
    removalDate <- Gen.option(genDateTime)
    sp <- Gen.option(genStartingPrices)
    ex <- Gen.option(genExchangePrices)
    order <- genOrder
    orders <- Gen.option(Set(order))
    _match <- genMatch
    matches <- Gen.option(Set(_match))
  } yield Runner(selectionId, handicap, status, adjustmentFactor, lastPriceTraded, totalMatched, removalDate, sp, ex, orders, matches)

//  property("Runner") = forAll(genRunner) { (a: Runner) =>
//    Json.toJson(a).validate[Runner].get equals a
//  }

  val genMarketBook: Gen[MarketBook] = for {
    marketId <- Arbitrary.arbitrary[String]
    isMarketDataDelayed <- Arbitrary.arbitrary[Boolean]
    status <- Arbitrary.arbitrary[String]
    betDelay <- Arbitrary.arbitrary[Int]
    bspReconciled <- Arbitrary.arbitrary[Boolean]
    complete <- Arbitrary.arbitrary[Boolean]
    inplay <- Arbitrary.arbitrary[Boolean]
    numberOfWinners <- Arbitrary.arbitrary[Int]
    numberOfRunners <- Arbitrary.arbitrary[Int]
    numberOfActiveRunners <- Arbitrary.arbitrary[Int]
    lastMatchTime <- Gen.option(genDateTime)
    totalMatched <- Arbitrary.arbitrary[Double]
    totalAvailable <- Arbitrary.arbitrary[Double]
    crossMatching <- Arbitrary.arbitrary[Boolean]
    runnersVoidable <- Arbitrary.arbitrary[Boolean]
    version <- Arbitrary.arbitrary[Long]
    runners <- genRunner
  } yield MarketBook(marketId, isMarketDataDelayed, status, betDelay, bspReconciled, complete, inplay, numberOfWinners,
      numberOfRunners, numberOfActiveRunners, lastMatchTime, totalMatched, totalAvailable, crossMatching, runnersVoidable,
      version, Set(runners))

//  property("MarketBook") = forAll(genMarketBook) { (a: MarketBook) =>
//    Json.toJson(a).validate[MarketBook].get equals a
//  }

  val genListMarketBookContainer: Gen[ListMarketBookContainer] = for {
    result <- Gen.listOf(genMarketBook)
  } yield ListMarketBookContainer(result)

//  property("ListMarketBookContainer") = forAll(genListMarketBookContainer) { (a: ListMarketBookContainer) =>
//    Json.toJson(a).validate[ListMarketBookContainer].get equals a
//  }

  val genMarketDescription: Gen[MarketDescription] = for {
    persistenceEnabled <- Arbitrary.arbitrary[Boolean]
    bspMarket <- Arbitrary.arbitrary[Boolean]
    marketTime <- Gen.option(genDateTime)
    suspendTime <- Gen.option(genDateTime)
    settleTime <- Gen.option(genDateTime)
    bettingType <- Arbitrary.arbitrary[String]
    turnInPlayEnabled <- Arbitrary.arbitrary[Boolean]
    marketType <- Arbitrary.arbitrary[String]
    regulator <- Arbitrary.arbitrary[String]
    marketBaseRate <- Arbitrary.arbitrary[Double]
    discountAllowed <- Arbitrary.arbitrary[Boolean]
    wallet <- Arbitrary.arbitrary[String]
    rules <- Arbitrary.arbitrary[String]
    rulesHasDate <- Arbitrary.arbitrary[Boolean]
    clarifications <- Gen.option(Arbitrary.arbitrary[String])
  } yield MarketDescription(persistenceEnabled, bspMarket, marketTime, suspendTime, settleTime, bettingType, turnInPlayEnabled,
      marketType, regulator, marketBaseRate, discountAllowed, wallet, rules, rulesHasDate, clarifications)

  property("MarketDescription") = forAll(genMarketDescription) { (a: MarketDescription) =>
    Json.toJson(a).validate[MarketDescription].get equals a
  }

  val genRunnerCatalog: Gen[RunnerCatalog] = for {
    selectionId <- Arbitrary.arbitrary[Long]
    runnerName <- Arbitrary.arbitrary[String]
    handicap <- Arbitrary.arbitrary[Double]
    sortPriority <- Gen.option(Arbitrary.arbitrary[Int])
    metaKey <- Arbitrary.arbitrary[String]
    metaVal <- Arbitrary.arbitrary[String]
    metaData <- Gen.option(Map[String, String](metaKey -> metaVal))
  } yield RunnerCatalog(selectionId, runnerName, handicap, sortPriority, metaData)

  property("RunnerCatalog") = forAll(genRunnerCatalog) { (a: RunnerCatalog) =>
    Json.toJson(a).validate[RunnerCatalog].get equals a
  }

  val genMarketCatalogue: Gen[MarketCatalogue] = for {
    marketId <- Arbitrary.arbitrary[String]
    marketName <- Arbitrary.arbitrary[String]
    marketStartTime <- Gen.option(genDateTime)
    description <- Gen.option(genMarketDescription)
    totalMatched <- Arbitrary.arbitrary[Double]
    runners <- Gen.option(Gen.listOf(genRunnerCatalog))
    eventType <- Gen.option(genEventType)
    competition <- Gen.option(genCompetition)
    event <- genEvent
  } yield MarketCatalogue(marketId, marketName, marketStartTime, description, totalMatched, runners, eventType, competition, event)

  property("MarketCatalogue") = forAll(genMarketCatalogue) { (a: MarketCatalogue) =>
    Json.toJson(a).validate[MarketCatalogue].get equals a
  }

  val genListMarketCatalogueContainer: Gen[ListMarketCatalogueContainer] = for {
    result <- Gen.listOf(genMarketCatalogue)
  } yield ListMarketCatalogueContainer(result)

  property("ListMarketCatalogueContainer") = forAll(genListMarketCatalogueContainer) { (a: ListMarketCatalogueContainer) =>
    Json.toJson(a).validate[ListMarketCatalogueContainer].get equals a
  }

  val genLoginResponse: Gen[LoginResponse] = for {
    token <- Arbitrary.arbitrary[String]
    product <- Arbitrary.arbitrary[String]
    status <- Arbitrary.arbitrary[String]
    error <- Arbitrary.arbitrary[String]
  } yield LoginResponse(token, product, status, error)

  property("LoginResponse") = forAll(genLoginResponse) { (a: LoginResponse) =>
    Json.toJson(a).validate[LoginResponse].get equals a
  }

  val genLogoutResponse: Gen[LogoutResponse] = for {
    token <- Arbitrary.arbitrary[String]
    product <- Arbitrary.arbitrary[String]
    status <- Arbitrary.arbitrary[String]
    error <- Arbitrary.arbitrary[String]
  } yield LogoutResponse(token, product, status, error)

  property("LogoutResponse") = forAll(genLogoutResponse) { (a: LogoutResponse) =>
    Json.toJson(a).validate[LogoutResponse].get equals a
  }

  val genMarketFilter: Gen[MarketFilter] = for {
    textQuery <- Gen.option(Arbitrary.arbitrary[String])
    exchangeIds <- Arbitrary.arbitrary[String]
    eventTypeIds <- Arbitrary.arbitrary[String]
    marketIds <- Arbitrary.arbitrary[String]
    inPlayOnly <- Gen.option(Arbitrary.arbitrary[Boolean])
    eventIds <- Arbitrary.arbitrary[String]
    competitionIds <- Arbitrary.arbitrary[String]
    venues <- Arbitrary.arbitrary[String]
    bspOnly <- Gen.option(Arbitrary.arbitrary[Boolean])
    turnInPlayEnabled <- Gen.option(Arbitrary.arbitrary[Boolean])
    marketBettingTypes <- Gen.oneOf(MarketBettingType.values.toSeq)
    marketCountries <- Arbitrary.arbitrary[String]
    marketTypeCodes <- Arbitrary.arbitrary[String]
    marketStartTime <- Gen.option(genTimeRange)
    withOrders <- Gen.oneOf(OrderStatus.values.toSeq)
  } yield MarketFilter(textQuery, Set(exchangeIds), Set(eventTypeIds), Set(marketIds), inPlayOnly, Set(eventIds),
      Set(competitionIds), Set(venues), bspOnly, turnInPlayEnabled, Set(marketBettingTypes), Set(marketCountries),
      Set(marketTypeCodes), marketStartTime, Set(withOrders))

  property("MarketFilter") = forAll(genMarketFilter) { (a: MarketFilter) =>
    Json.toJson(a).validate[MarketFilter].get equals a
  }

  val genMarketOnCloseOrder: Gen[MarketOnCloseOrder] = for {
    liability <- Arbitrary.arbitrary[Double]
  } yield MarketOnCloseOrder(liability)

  property("MarketOnCloseOrder") = forAll(genMarketOnCloseOrder) { (a: MarketOnCloseOrder) =>
    Json.toJson(a).validate[MarketOnCloseOrder].get equals a
  }

  val genRunnerProfitAndLoss: Gen[RunnerProfitAndLoss] = for {
    selectedId <- Arbitrary.arbitrary[Long]
    ifWin <- Arbitrary.arbitrary[Double]
    ifLose <- Arbitrary.arbitrary[Double]
    ifPlace <- Arbitrary.arbitrary[Double]
  } yield RunnerProfitAndLoss(selectedId, ifWin, ifLose, ifPlace)

  property("RunnerProfitAndLoss") = forAll(genRunnerProfitAndLoss) { (a: RunnerProfitAndLoss) =>
    Json.toJson(a).validate[RunnerProfitAndLoss].get equals a
  }

  val genMarketProfitAndLoss: Gen[MarketProfitAndLoss] = for {
    marketId <- Arbitrary.arbitrary[String]
    commissionApplied <- Arbitrary.arbitrary[Double]
    profitAndLoss <- genRunnerProfitAndLoss
  } yield MarketProfitAndLoss(marketId, commissionApplied, Set(profitAndLoss))

  property("MarketProfitAndLoss") = forAll(genMarketProfitAndLoss) { (a: MarketProfitAndLoss) =>
    Json.toJson(a).validate[MarketProfitAndLoss].get equals a
  }

  val genMarketProfitAndLossContainer: Gen[MarketProfitAndLossContainer] = for {
    result <- Gen.listOf(genMarketProfitAndLoss)
  } yield MarketProfitAndLossContainer(result)

  property("MarketProfitAndLossContainer") = forAll(genMarketProfitAndLossContainer) { (a: MarketProfitAndLossContainer) =>
    Json.toJson(a).validate[MarketProfitAndLossContainer].get equals a
  }

  val genMarketTypeResult: Gen[MarketTypeResult] = for {
    marketType <- Arbitrary.arbitrary[String]
    marketCount <- Arbitrary.arbitrary[Int]
  } yield MarketTypeResult(marketType, marketCount)

  property("MarketTypeResult") = forAll(genMarketTypeResult) { (a: MarketTypeResult) =>
    Json.toJson(a).validate[MarketTypeResult].get equals a
  }

  val genMarketTypeResultContainer: Gen[MarketTypeResultContainer] = for {
    result <- Gen.listOf(genMarketTypeResult)
  } yield MarketTypeResultContainer(result)

  property("MarketTypeResultContainer") = forAll(genMarketTypeResultContainer) { (a: MarketTypeResultContainer) =>
    Json.toJson(a).validate[MarketTypeResultContainer].get equals a
  }

  val genPlaceExecutionReport: Gen[PlaceExecutionReport] = for {
    status <- Gen.oneOf(ExecutionReportStatus.values.toSeq)
    marketId <- Arbitrary.arbitrary[String]
    errorCode <- Gen.option(Gen.oneOf(ExecutionReportErrorCode.values.toSeq))
    instructionReport <- genPlaceInstructionReport
    customerRef <- Gen.option(Arbitrary.arbitrary[String])
  } yield PlaceExecutionReport(status, marketId, errorCode, Set(instructionReport), customerRef)

  property("PlaceExecutionReport") = forAll(genPlaceExecutionReport) { (a: PlaceExecutionReport) =>
    Json.toJson(a).validate[PlaceExecutionReport].get equals a
  }

  val genPlaceExecutionReportContainer: Gen[PlaceExecutionReportContainer] = for {
    result <- genPlaceExecutionReport
  } yield PlaceExecutionReportContainer(result)

  property("PlaceExecutionReportContainer") = forAll(genPlaceExecutionReportContainer) { (a: PlaceExecutionReportContainer) =>
    Json.toJson(a).validate[PlaceExecutionReportContainer].get equals a
  }

  val genPlaceInstruction: Gen[PlaceInstruction] = for {
    orderType <- Gen.oneOf(OrderType.values.toSeq)
    selectionId <- Arbitrary.arbitrary[Long]
    handicap <- Arbitrary.arbitrary[Double]
    side <- Gen.oneOf(Side.values.toSeq)
    limitOrder <- Gen.option(genLimitOrder)
    limitOnCloseOrder <- Gen.option(genLimitOnCloseOrder)
    marketOnCloseOrder <- Gen.option(genMarketOnCloseOrder)
  } yield PlaceInstruction(orderType, selectionId, handicap, side, limitOrder, limitOnCloseOrder, marketOnCloseOrder)

  property("PlaceInstruction") = forAll(genPlaceInstruction) { (a: PlaceInstruction) =>
    Json.toJson(a).validate[PlaceInstruction].get equals a
  }

  val genPlaceInstructionReport: Gen[PlaceInstructionReport] = for {
    status <- Gen.oneOf(InstructionReportStatus.values.toSeq)
    errorCode <- Gen.option(Gen.oneOf(InstructionReportErrorCode.values.toSeq))
    instruction <- genPlaceInstruction
    betId <- Gen.option(Arbitrary.arbitrary[String])
    placedDate <- Gen.option(genDateTime)
    averagePriceMatched <- Gen.option(Arbitrary.arbitrary[Double])
    sizeMatched <- Gen.option(Arbitrary.arbitrary[Double])
  } yield PlaceInstructionReport(status, errorCode, instruction, betId, placedDate, averagePriceMatched, sizeMatched)

  property("PlaceInstructionReport") = forAll(genPlaceInstructionReport) { (a: PlaceInstructionReport) =>
    Json.toJson(a).validate[PlaceInstructionReport].get equals a
  }

  val genPriceProjections: Gen[PriceProjection] = for {
    priceData <- Gen.oneOf(PriceData.values.toSeq)
  } yield PriceProjection(Set(priceData))

  property("PriceProjection") = forAll(genPriceProjections) { (a: PriceProjection) =>
    Json.toJson(a).validate[PriceProjection].get equals a
  }

  val genReplaceExecutionReport: Gen[ReplaceExecutionReport] = for {
    customerRef <- Gen.option(Arbitrary.arbitrary[String])
    status <- Gen.oneOf(ExecutionReportStatus.values.toSeq)
    errorCode <- Gen.option(Gen.oneOf(ExecutionReportErrorCode.values.toSeq))
    marketId <- Arbitrary.arbitrary[String]
    instructionReports <- genReplaceInstructionReport
  } yield ReplaceExecutionReport(customerRef, status, errorCode, marketId, Set(instructionReports))

  property("ReplaceExecutionReport") = forAll(genReplaceExecutionReport) { (a: ReplaceExecutionReport) =>
    Json.toJson(a).validate[ReplaceExecutionReport].get equals a
  }

  val genReplaceExecutionReportContainer: Gen[ReplaceExecutionReportContainer] = for {
    result <- genReplaceExecutionReport
  } yield ReplaceExecutionReportContainer(result)

  property("ReplaceExecutionReportContainer") = forAll(genReplaceExecutionReportContainer) { (a: ReplaceExecutionReportContainer) =>
    Json.toJson(a).validate[ReplaceExecutionReportContainer].get equals a
  }

  val genReplaceInstruction: Gen[ReplaceInstruction] = for {
    betId <- Arbitrary.arbitrary[String]
    newPrice <- Arbitrary.arbitrary[Double]
  } yield ReplaceInstruction(betId, newPrice)

  property("ReplaceInstruction") = forAll(genReplaceInstruction) { (a: ReplaceInstruction) =>
    Json.toJson(a).validate[ReplaceInstruction].get equals a
  }

  val genReplaceInstructionReport: Gen[ReplaceInstructionReport] = for {
    status <- Gen.oneOf(InstructionReportStatus.values.toSeq)
    errorCode <- Gen.option(Gen.oneOf(InstructionReportErrorCode.values.toSeq))
    cancelInstructionReport <- Gen.option(genCancelInstructionReport)
    placeInstructionReport <- Gen.option(genPlaceInstructionReport)
  } yield ReplaceInstructionReport(status, errorCode, cancelInstructionReport, placeInstructionReport)

  property("ReplaceInstructionReport") = forAll(genReplaceInstructionReport) { (a: ReplaceInstructionReport) =>
    Json.toJson(a).validate[ReplaceInstructionReport].get equals a
  }

  val genTimeRange: Gen[TimeRange] = for {
    from <- Gen.option(genDateTime)
    to <- Gen.option(genDateTime)
  } yield TimeRange(from, to)

  property("TimeRange") = forAll(genTimeRange) { (a: TimeRange) =>
    Json.toJson(a).validate[TimeRange].get equals a
  }

  val genTimeRangeResult: Gen[TimeRangeResult] = for {
    timeRange <- genTimeRange
    marketCount <- Arbitrary.arbitrary[Int]
  } yield TimeRangeResult(timeRange, marketCount)

  property("TimeRangeResult") = forAll(genTimeRangeResult) { (a: TimeRangeResult) =>
    Json.toJson(a).validate[TimeRangeResult].get equals a
  }

  val genTimeRangeResultContainer: Gen[TimeRangeResultContainer] = for {
    result <- Gen.listOf(genTimeRangeResult)
  } yield TimeRangeResultContainer(result)

  property("TimeRangeResultContainer") = forAll(genTimeRangeResultContainer) { (a: TimeRangeResultContainer) =>
    Json.toJson(a).validate[TimeRangeResultContainer].get equals a
  }

  val genUpdateExecutionReport: Gen[UpdateExecutionReport] = for {
    status <- Gen.oneOf(ExecutionReportStatus.values.toSeq)
    marketId <- Arbitrary.arbitrary[String]
    errorCode <- Gen.option(Gen.oneOf(ExecutionReportErrorCode.values.toSeq))
    instructionReports <- genUpdateInstructionReport
    customerRef <- Gen.option(Arbitrary.arbitrary[String])
  } yield UpdateExecutionReport(status, marketId, errorCode, Set(instructionReports), customerRef)

  property("UpdateExecutionReport") = forAll(genUpdateExecutionReport) { (a: UpdateExecutionReport) =>
    Json.toJson(a).validate[UpdateExecutionReport].get equals a
  }

  val genUpdateExecutionReportContainer: Gen[UpdateExecutionReportContainer] = for {
    result <- genUpdateExecutionReport
  } yield UpdateExecutionReportContainer(result)

  property("UpdateExecutionReportContainer") = forAll(genUpdateExecutionReportContainer) { (a: UpdateExecutionReportContainer) =>
    Json.toJson(a).validate[UpdateExecutionReportContainer].get equals a
  }

  val genUpdateInstruction: Gen[UpdateInstruction] = for {
    betId <- Arbitrary.arbitrary[String]
    newPersistenceType <- Gen.oneOf(PersistenceType.values.toSeq)
  } yield UpdateInstruction(betId, newPersistenceType)

  property("UpdateInstruction") = forAll(genUpdateInstruction) { (a: UpdateInstruction) =>
    Json.toJson(a).validate[UpdateInstruction].get equals a
  }

  val genUpdateInstructionReport: Gen[UpdateInstructionReport] = for {
    status <- Gen.oneOf(InstructionReportStatus.values.toSeq)
    errorCode <- Gen.option(Gen.oneOf(InstructionReportErrorCode.values.toSeq))
    instruction <- genUpdateInstruction
  } yield UpdateInstructionReport(status, errorCode, instruction)

  property("UpdateInstructionReport") = forAll(genUpdateInstructionReport) { (a: UpdateInstructionReport) =>
    Json.toJson(a).validate[UpdateInstructionReport].get equals a
  }

  val genVenueResult: Gen[VenueResult] = for {
    venue <- Arbitrary.arbitrary[String]
    marketCount <- Arbitrary.arbitrary[Int]
  } yield VenueResult(venue, marketCount)

  property("VenueResult") = forAll(genVenueResult) { (a: VenueResult) =>
    Json.toJson(a).validate[VenueResult].get equals a
  }

  val genVenueResultContainer: Gen[VenueResultContainer] = for {
    result <- Gen.listOf(genVenueResult)
  } yield VenueResultContainer(result)

  property("VenueResultContainer") = forAll(genVenueResultContainer) { (a: VenueResultContainer) =>
    Json.toJson(a).validate[VenueResultContainer].get equals a
  }
}