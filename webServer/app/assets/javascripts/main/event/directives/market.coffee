directivesModule.directive('market',  ['$log', 'DataModelService', 'WebSocketService', ($log, DataModelService, WebSocketService) ->
  templateUrl: '/assets/partials/main/event/directives/market.html'
  scope: {
    item: '='
    onRunnerClicked: '&'
  }
  link: (scope, iElement, iAttrs) ->
    $log.debug 'market content directive'

    WebSocketService.subscribeToMarkets([scope.item.id], "BEST")
    WebSocketService.listMarketCatalogue([scope.item.id])

    scope.$on '$destroy', () ->
      WebSocketService.unSubscribeFromMarkets([scope.item.id], "BEST")

    scope.catalogueData = DataModelService.marketCatalogueData
])
