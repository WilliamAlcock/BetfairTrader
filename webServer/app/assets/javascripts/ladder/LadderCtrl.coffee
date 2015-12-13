class LadderCtrl

  constructor: (@$log, @$stateParams, @$scope, @WebSocketService, @DataModelService, @PriceService) ->
    @bookData = @DataModelService.marketBookData
    @catalogueData = @DataModelService.marketCatalogueData

    @prices = []

    @center = undefined
    @catalogue = undefined
    @book = {}

    @$log.log "Ladder Controller", @$stateParams, @DataModelService

    @WebSocketService.subscribeToMarkets([@$stateParams.marketId], "ALL_AND_TRADED")
    @WebSocketService.listMarketCatalogue([@$stateParams.marketId])

    @$scope.$on '$destroy', () ->
      @WebSocketService.unSubscribeFromMarkets([@$stateParams.marketId], "ALL_AND_TRADED")

    @$scope.$on 'market-' + @$stateParams.marketId, @applyPrices

  scrollUp: () =>
    @center = @PriceService.incrementPrice(@center)
    @getPrices()

  scrollDown: () =>
    @center = @PriceService.decrementPrice(@center)
    @getPrices()

  snapToBid: () =>
    @center = @bookData[@$stateParams.marketId].runnersMap[@$stateParams.selectionId].ex.availableToBack[0].price
    @getPrices()

  snapToOffer: () =>
    @center = @bookData[@$stateParams.marketId].runnersMap[@$stateParams.selectionId].ex.availableToLay[0].price
    @getPrices()

  applyPrices: () =>
    @$scope.$apply(@getPrices())

  getPrices: () =>
    if angular.isDefined(@bookData[@$stateParams.marketId]) && @center == undefined
      @center = @bookData[@$stateParams.marketId].runnersMap[@$stateParams.selectionId].ex.availableToBack[0].price
    if angular.isDefined(@catalogueData[@$stateParams.marketId]) && @catalogue == undefined
      @catalogue = x for x in @catalogueData[@$stateParams.marketId].runners when x.uniqueId == @$stateParams.selectionId
    @prices = @PriceService.getPriceData(
      @bookData[@$stateParams.marketId].runnersMap[@$stateParams.selectionId].ex.availableToBack.slice(),
      @bookData[@$stateParams.marketId].runnersMap[@$stateParams.selectionId].ex.availableToLay.slice(),
      @bookData[@$stateParams.marketId].runnersMap[@$stateParams.selectionId].ex.tradedVolume.slice(),
      @center,
      10
    )
    @book = @bookData[@$stateParams.marketId].runnersMap[@$stateParams.selectionId]

controllersModule.controller('LadderCtrl', ['$log', '$stateParams', '$scope', 'WebSocketService', 'DataModelService', 'PriceService', LadderCtrl])