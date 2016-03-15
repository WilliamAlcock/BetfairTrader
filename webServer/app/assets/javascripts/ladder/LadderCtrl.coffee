class LadderCtrl

  constructor: (@$log, @$stateParams, @$scope, @WebSocketService, @DataModelService, @PriceService) ->

    # TODO get these values from config
    @depth = 5
    @orderSize = 2
    @prices = []
    @isPositionOpen = true

    @catalogueData = @DataModelService.marketCatalogueData

    @catalogue = undefined

    @$log.log "Ladder Controller", @$stateParams, @DataModelService

    @WebSocketService.subscribeToMarkets([@$stateParams.marketId], "ALL_AND_TRADED")
    @WebSocketService.listMarketCatalogue([@$stateParams.marketId])

    @$scope.$on '$destroy', () -> @WebSocketService.unSubscribeFromMarkets([@$stateParams.marketId], "ALL_AND_TRADED")

    @cancelMarketWatch = @$scope.$on 'market-' + @$stateParams.marketId, @updateRunner
    @cancelCatalogueWatch = @$scope.$on 'catalogue-' + @$stateParams.marketId, @updateRunner

  togglePosition: () =>
    @isPositionOpen = !@isPositionOpen

  getRunner: () =>
    if (@DataModelService.marketBookData[@$stateParams.marketId]?)
      @DataModelService.marketBookData[@$stateParams.marketId].runners[@$stateParams.selectionId]
    else {}

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

  updateRunner: () =>
    @$log.log('updating runner', )
    if (@catalogueData[@$stateParams.marketId]?)
      @catalogue = (x for x in @catalogueData[@$stateParams.marketId].runners when x.uniqueId == @$stateParams.selectionId)[0]
      @$log.log('got catalogue data', @catalogueData[@$stateParams.marketId], @catalogue)
      @snapToBid()
      @$scope.$apply()
      @cancelMarketWatch()
      @cancelCatalogueWatch()

controllersModule.controller('LadderCtrl', ['$log', '$stateParams', '$scope', 'WebSocketService', 'DataModelService', 'PriceService', LadderCtrl])