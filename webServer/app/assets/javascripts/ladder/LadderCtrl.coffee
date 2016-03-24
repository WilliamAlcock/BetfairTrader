class LadderCtrl

  constructor: (@$log, @$stateParams, @$scope, @WebSocketService, @DataModelService, @PriceService, @$rootScope) ->

    # TODO get these values from config
    @depth = 5
    @orderSize = 2
    @prices = []
    @isPositionOpen = true

    @catalogueData = @DataModelService.marketCatalogueData

    @catalogue = {}

    @$log.log "Ladder Controller", @$stateParams, @DataModelService

    @WebSocketService.subscribeToMarkets([@$stateParams.marketId], "ALL_AND_TRADED")
    @WebSocketService.listMarketCatalogue([@$stateParams.marketId])

    @$scope.$on '$destroy', () -> @WebSocketService.unSubscribeFromMarkets([@$stateParams.marketId], "ALL_AND_TRADED")

    @cancelMarketWatch = @$scope.$on 'market-' + @$stateParams.marketId, @updateLadder
    @cancelCatalogueWatch = @$scope.$on 'catalogue-' + @$stateParams.marketId, @updateTitle

  togglePosition: () =>
    @isPositionOpen = !@isPositionOpen

  getRunner: () =>
    if (@DataModelService.marketBookData[@$stateParams.marketId]?)
      @DataModelService.marketBookData[@$stateParams.marketId].runners[@$stateParams.selectionId]
    else {}

  getRunnerCatalogue: () =>
    if (Object.keys(@catalogue).length == 0)
      @catalogue = @DataModelService.getRunnerCatalogue(@$stateParams.marketId, parseInt(@$stateParams.selectionId.split('-')[0], 10))
    @catalogue

  placeOrder: (side, price, size) =>
    @WebSocketService.placeOrders(
      @$stateParams.marketId,
      @getRunner().selectionId,
      @getRunner().handicap,
      side,
      price,
      size
    )

  cancelOrders: (side, price) =>
    @WebSocketService.cancelOrders(
      @$stateParams.marketId,
      @getRunner().orders.filter (x) -> x.price == price && x.side == side
    )

  setOrderSize: (size) => @orderSize = size

  sell: (price, size) => @placeOrder("BACK", Number(price), size)

  cancelSell: (price) => @cancelOrders("BACK", Number(price))

  buy: (price, size) => @placeOrder("LAY", Number(price), size)

  cancelBuy: (price) => @cancelOrders("LAY", Number(price))

  scrollUp: () => @prices = @PriceService.scrollUp(@prices)

  scrollDown: () => @prices = @PriceService.scrollDown(@prices)

  snapToBid: () =>
    @prices = @PriceService.getLadderPrices(@getRunner().ex.availableToBack[0].price, @depth)
    @$log.log('set prices', @prices)

  snapToOffer: () => @prices = @PriceService.getLadderPrices(@getRunner().ex.availableToLay[0].price, @depth)

  updateTitle: () =>
    if (@catalogueData[@$stateParams.marketId]?)
      @$rootScope.title = @catalogueData[@$stateParams.marketId].event.name
      @cancelCatalogueWatch()

  updateLadder: () =>
    @snapToBid()
    @$scope.$apply()
    @cancelMarketWatch()

controllersModule.controller('LadderCtrl', ['$log', '$stateParams', '$scope', 'WebSocketService', 'DataModelService', 'PriceService', '$rootScope', LadderCtrl])