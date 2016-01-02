class LadderCtrl

  constructor: (@$log, @$stateParams, @$scope, @WebSocketService, @DataModelService, @PriceService) ->
    @depth = 10

    @catalogueData = @DataModelService.marketCatalogueData

    @orderSize = 2
    @prices = []
    @catalogue = undefined

    @$log.log "Ladder Controller", @$stateParams, @DataModelService

    @WebSocketService.subscribeToMarkets([@$stateParams.marketId], "ALL_AND_TRADED")
    @WebSocketService.listMarketCatalogue([@$stateParams.marketId])

    @$scope.$on '$destroy', () -> @WebSocketService.unSubscribeFromMarkets([@$stateParams.marketId], "ALL_AND_TRADED")

    @cancel = @$scope.$on 'market-' + @$stateParams.marketId, @updateRunner

  getRunner: () =>
    if angular.isDefined(@DataModelService.marketBookData[@$stateParams.marketId])
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

  snapToBid: () => @prices = @PriceService.getLadderPrices(@getRunner().ex.availableToBack[0].price, @depth)

  snapToOffer: () => @prices = @PriceService.getLadderPrices(@getRunner().ex.availableToLay[0].price, @depth)

  updateRunner: () =>
    @catalogue = x for x in @catalogueData[@$stateParams.marketId].runners when x.uniqueId == @$stateParams.selectionId
    @snapToBid()
    @cancel()

controllersModule.controller('LadderCtrl', ['$log', '$stateParams', '$scope', 'WebSocketService', 'DataModelService', 'PriceService', LadderCtrl])