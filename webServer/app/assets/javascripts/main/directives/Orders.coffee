directivesModule.directive('orders',  ['$log', 'DataModelService', 'WebSocketService', ($log, DataModelService, WebSocketService) ->
  templateUrl: '/assets/partials/main/directives/orders.html'
  scope: {
    marketId: '='
    selectionId: '='
    handicap: '='
    data: '='
  }
  link: (scope, iElement, iAttrs) ->
    scope.catalogueData = DataModelService.marketCatalogueData

    scope.runnerCatalogue = {}

    scope.getRunnerCatalogue = () =>
      if (Object.keys(scope.runnerCatalogue).length == 0)
        scope.runnerCatalogue = DataModelService.getRunnerCatalogue(scope.marketId, parseInt(scope.selectionId, 10))
      scope.runnerCatalogue

    scope.getLiability = (order) => DataModelService.getLiability(order.side, order.priceSize.price, order.priceSize.size)

    scope.getProfit = (order) => DataModelService.getProfit(order.side, order.priceSize.price, order.priceSize.size)

    scope.cancelOrder = (order) => WebSocketService.cancelOrders(order.marketId, [order])

    scope.getOrderInfo = (order) => console.log('GET INFO CALLED', order)

    WebSocketService.listMarketCatalogue([scope.marketId])

    $log.debug 'Orders directive'
])
