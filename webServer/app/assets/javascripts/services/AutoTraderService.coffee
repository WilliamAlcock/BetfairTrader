class AutoTraderService

  constructor: (@$log) ->

    @strategies = {}

  isStrategyRunning: (marketId, selectionId, handicap) =>
    @strategies[marketId]? && @strategies[marketId][selectionId]? && @strategies[marketId][selectionId][handicap]? && @strategies[marketId][selectionId][handicap].running

  strategyCreated: (message) =>
    @$log.log("Created Strategy", message)

  strategyStarted: (message) =>
    @$log.log("Starting Strategy", message)
#    if (!@strategies[marketId]?) then @strategies[marketId] = {}
#    if (!@strategies[marketId][selectionId]?) then @strategies[marketId][selectionId] = {}
#    @strategies[marketId][selectionId][handicap] = {running: true}

  strategyStateChanged: (message) =>
    @$log.log("Strategy State Changed", message)

  strategyStopped: (message) =>
    @$log.log("Stopping Strategy", message)
#    @strategies[marketId][selectionId][handicap] = false

servicesModule.service('AutoTraderService', ['$log', AutoTraderService])