class WebSocketService

  EVENT_TYPE_UPDATE = "eventTypeUpdate"
  EVENT_UPDATE = "eventUpdate"
  MARKET_BOOK_UPDATE = "marketUpdate"
  MARKET_CATALOGUE_UPDATE = "marketCatalogueUpdate"
  NAVIGATION_DATA_UPDATE = "navigationDataUpdate"

  channelMap = {
    "EventTypeUpdate":        EVENT_TYPE_UPDATE,
    "EventUpdate":            EVENT_UPDATE,
    "MarketBookUpdate":       MARKET_BOOK_UPDATE,
    "MarketCatalogueUpdate":  MARKET_CATALOGUE_UPDATE,
    "NavigationDataUpdate":   NAVIGATION_DATA_UPDATE
  }

  constructor: (@$log, @$q, @$rootScope, @DataModel, @OrderBook) ->
    @$log.debug "constructing Websocket service"
    @id = 0

  init: () ->
    deferred = @$q.defer()

    # TODO get websocket address from config
    @ws = new WebSocket("ws://localhost:9000/socket")

    @ws.onopen = =>
      @$log.debug "WebSocket open"
      @subscribeToSystemAlerts()        # TODO move this to resolve in state
      @subscribeToOrderUpdates()        # TODO move this to resolve in state
      @listCurrentOrders()              # TODO move this to resolve in state
      @listMatches()                    # TODO move this to resolve in state
      deferred.resolve()

    @ws.onclose = => @$log.debug "WebSocket closed"

    @ws.onerror = => @$log.debug "WebSocket error"

    @ws.onmessage = (message) => @parseResponse(angular.fromJson(message.data))

    deferred.promise

  parseResponse: (message) =>
    if angular.isUndefined(message.jsonrpc) || message.jsonrpc != "2.0"
      throw "Invalid response, no jsonrpc identifier"

    if angular.isDefined(message.result)
      switch message.result.resultType
        when "MarketCatalogueUpdate"
          @$rootScope.$apply(@DataModel.setMarketCatalogueData(message.result.result.marketId, message.result.result))
          @$rootScope.$broadcast('catalogue-' + message.result.result.marketId, message.result.result)
        when "MarketBookUpdate"
          @$rootScope.$apply(@DataModel.setMarketBookData(message.result.result.marketId, message.result.result))
#          @$log.log "got market book data", @DataModel.marketBookData, message.result.result.marketId
          @$rootScope.$broadcast('market-' + message.result.result.marketId, message.result.result)
        when "SoccerData"
          @$log.log("Soccer Data -> ", message)
          @$rootScope.$apply(@DataModel.setSoccerData(message.result.result))
        when "HorseRacingData"
          @$log.log("Horse Racing Data -> ", message)
          @$rootScope.$apply(@DataModel.setHorseRacingData(message.result.result))
        when "CurrentOrdersUpdate"
          @$log.log("Current order update", message)
          @$rootScope.$apply(@OrderBook.setOrders(message.result.result.currentOrders))
        when "CurrentMatchesUpdate"
          @$log.log("Current match update", message)
          @$rootScope.$apply(@OrderBook.setMatches(message.result.result.matches))
        when "OrderMatched"
          @$log.log("Order Matched", message)
          @$rootScope.$apply(@OrderBook.orderMatched(message.result.result))
        when "OrderPlaced"
          @$log.log("Order Placed", message)
          @$rootScope.$apply(@OrderBook.orderPlaced(message.result.result))
        when "OrderUpdated"
          @$log.log("Order Updated", message)
          @$rootScope.$apply(@OrderBook.orderUpdated(message.result.result))
        when "OrderExecuted"
          @$log.log("Order Executed", message)
          @$rootScope.$apply(@OrderBook.orderExecuted(message.result.result))

        else null
    else if angular.isDefined(message.error)
      console.log("error response " + message.error)
    else
      throw "Invalid response"

  sendJsonrpcMessage: (message) ->
    message.jsonrpc = "2.0"
    message.id = ++@id
    @ws.send(JSON.stringify(message))

  subscribeToSystemAlerts: () -> @sendJsonrpcMessage({method: "subscribeToSystemAlerts", params: {}})

  subscribeToOrderUpdates: () -> @sendJsonrpcMessage({method: "subscribeToOrderUpdates", params: {markets: []}})

  listCurrentOrders: () -> @sendJsonrpcMessage({method: "listCurrentOrders", params: {betIds: [], marketIds: []}})

  listMatches: () -> @sendJsonrpcMessage({method: "listMatches", params: {}})

  getNavigationData: (eventTypeId) -> @sendJsonrpcMessage({method: "getNavigationData", params: {eventTypeId: eventTypeId}})

  subscribeToMarkets: (markets, pollingGroup) -> @sendJsonrpcMessage({method: "subscribeToMarkets", params: {markets: markets, pollingGroup: {group: pollingGroup}}})

  unSubscribeFromMarkets: (markets, pollingGroup) -> @sendJsonrpcMessage({method: "unSubscribeFromMarkets", params: {markets: markets, pollingGroup: {group: pollingGroup}}})

  listMarketCatalogue: (marketIds) ->
    marketFilter = {
      marketIds: marketIds
      exchangeIds: []
      eventTypeIds: []
      eventIds: []
      venues: []
      competitionIds: []
      marketTypeCodes: []
      marketCountries: []
      marketBettingTypes: []
      withOrders: []
    }
    @sendJsonrpcMessage({method: "listMarketCatalogue", params: {marketFilter: marketFilter, sort: 'MAXIMUM_TRADED'}})

  placeOrders: (marketId, selectionId, handicap, side, price, size, customerRef) ->
    instruction = {
      orderType: "LIMIT",
      selectionId: selectionId,
      handicap: handicap,
      side: side,
      limitOrder: {size: size, price: price, persistenceType: "LAPSE"}
    }
    @sendJsonrpcMessage({method: "placeOrders", params: {marketId: marketId, instructions: [instruction]}})
#
#  replaceOrders: (marketId, instructions, customerRef) ->
#    @sendJsonrpcMessage({method: "replaceOrders", params: {marketId: marketId, instructions: instructions, customerRef: customerRef}})
#
#  updateOrders: (marketId, instructions, customerRef) ->
#    @sendJsonrpcMessage({method: "updateOrders",  params: {marketId: marketId, instructions: instructions, customerRef: customerRef}})
#

  cancelOrders: (marketId, orders, customerRef) ->
    console.log(marketId, orders)
    instructions = orders.map (x) -> {betId:x.betId}
    @sendJsonrpcMessage({method: "cancelOrders",  params: {marketId: marketId, instructions: instructions}})

servicesModule.service('WebSocketService', ['$log', '$q', '$rootScope', 'DataModelService', 'OrderBookService', WebSocketService])