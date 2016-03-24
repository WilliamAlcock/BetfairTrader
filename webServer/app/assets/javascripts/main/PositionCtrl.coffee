class PositionCtrl

  constructor: (@$log, @$scope, @OrderBook, @DataModelService) ->
    @$log.debug "Position Controller"

    @isOpen = {
      orders: true
      matches: true
    }

    @orders = @OrderBook.orders

    @matches = @OrderBook.matches

    @catalogueData = @DataModelService.marketCatalogueData

  getRunnerCatalogue: (marketId, selectionId) => @DataModelService.getRunnerCatalogue(marketId, parseInt(selectionId, 10))

controllersModule.controller('positionCtrl', ['$log', '$scope', 'OrderBookService', 'DataModelService', PositionCtrl])