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

  constructor: (@$log, @$q, @$rootScope, @DataModel) ->
    @$log.debug "constructing Websocket service"
    @id = 0

  init: () ->
    deferred = @$q.defer()

    # TODO get websocket address from config
    @ws = new WebSocket("ws://localhost:9000/socket")

    @ws.onopen = =>
      @$log.debug "WebSocket open"
      # TODO move this to resolve in state
      @subscribeToNavData()
      deferred.resolve()

    @ws.onclose = =>
      @$log.debug "WebSocket closed"

    @ws.onerror = =>
      @$log.debug "WebSocket error"

    @ws.onmessage = (message) =>
      @parseResponse(angular.fromJson(message.data))

    deferred.promise

  parseResponse: (message) =>
    if angular.isUndefined(message.jsonrpc) || message.jsonrpc != "2.0"
      throw "Invalid response, no jsonrpc identifier"

    if angular.isDefined(message.result)
      switch message.result.resultType
#        when "EventTypeUpdate" then @DataModel.eventTypeData[message.result]
#        when "EventUpdate" then @DataModel.eventData
        when "MarketCatalogueUpdate"
          @$rootScope.$apply(@DataModel.marketCatalogueData[message.result.result.marketId] = message.result.result)
        when "MarketBookUpdate"
          @$rootScope.$apply(@DataModel.marketBookData[message.result.result.marketId] = message.result.result)
#          @$log.log "got market book data", @DataModel.marketBookData, message.result.result.marketId
          @$rootScope.$broadcast('market-' + message.result.result.marketId, message.result.result)
        when "NavigationDataUpdate"
          @$rootScope.$apply(@DataModel.setNavData(message.result.result))
        else null
    else if angular.isDefined(message.error)
      println "error response " + message.error
    else
      throw "Invalid response"

  sendJsonrpcMessage: (message) ->
    message.jsonrpc = "2.0"
    message.id = ++@id
    @ws.send(JSON.stringify(message))

  subscribeToNavData: ->
    @sendJsonrpcMessage({method: "subscribeToNavData", params: {}})

  subscribeToMarkets: (markets, pollingGroup) ->
    @sendJsonrpcMessage({method: "subscribeToMarkets", params: {markets: markets, pollingGroup: {group: pollingGroup}}})

  unSubscribeFromMarkets: (markets, pollingGroup) ->
    @sendJsonrpcMessage({method: "unSubscribeFromMarkets", params: {markets: markets, pollingGroup: {group: pollingGroup}}})

  listMarketCatalogue: (marketIds) ->
    @sendJsonrpcMessage({method: "listMarketCatalogue", params: {marketIds: marketIds}})

#  listEventTypes: () ->
#    @sendJsonrpcMessage({method: "listEventTypes", params: {}})
#
#  listEvents: (eventTypeId) ->
#    @sendJsonrpcMessage({method: "listEvents", params: {eventTypeId: eventTypeId}})
#
#  stopPollingAllMarkets: () ->
#    @sendJsonrpcMessage({method: "stopPollingAllMarkets", params: {}})
#
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
    instructions = orders.map (x) -> {betId:x.betId}
    @sendJsonrpcMessage({method: "cancelOrders",  params: {marketId: marketId, instructions: instructions}})

servicesModule.service('WebSocketService', ['$log', '$q', '$rootScope', 'DataModelService', WebSocketService])