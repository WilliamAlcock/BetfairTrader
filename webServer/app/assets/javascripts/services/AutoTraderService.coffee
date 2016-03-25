class AutoTraderService

  constructor: (@$log) ->

    @strategies = {}

  setStrategy: (marketId, selectionId, handicap, strategyId, isRunning, currentState) =>
    if (!@strategies[marketId]?) then @strategies[marketId] = {}
    if (!@strategies[marketId][selectionId]?) then @strategies[marketId][selectionId] = {}
    @strategies[marketId][selectionId][handicap] = {
      strategyId: strategyId
      isRunning: isRunning
      currentState: currentState
    }
    console.log('set strategy', @strategies)

  isStrategyRunning: (marketId, selectionId, handicap) =>
    @strategies[marketId]? && @strategies[marketId][selectionId]? && @strategies[marketId][selectionId][handicap]? && @strategies[marketId][selectionId][handicap].isRunning

  strategyCreated: (message) => @setStrategy(message.marketId, message.selectionId, message.handicap, message.strategyId, false, 'Created')

  strategyStarted: (message) => @setStrategy(message.marketId, message.selectionId, message.handicap, message.strategyId, true, message.state)

  strategyStateChanged: (message) => @setStrategy(message.marketId, message.selectionId, message.handicap, message.strategyId, true, message.newState)

  strategyStopped: (message) => @setStrategy(message.marketId, message.selectionId, message.handicap, message.strategyId, false, 'Stopped')

servicesModule.service('AutoTraderService', ['$log', AutoTraderService])