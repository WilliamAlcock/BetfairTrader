directivesModule.directive('market',  ['$log', 'DataModelService', 'WebSocketService', ($log, DataModelService, WebSocketService) ->
  templateUrl: '/assets/partials/main/soccer/directives/market.html'
  scope: {
    item: '='
    onRunnerClicked: '&'
  }
  link: (scope, iElement, iAttrs) ->
    $log.debug 'market content directive'

    WebSocketService.listMarketBook([scope.item.id])
    WebSocketService.subscribeToMarkets([scope.item.id], "BEST")
    WebSocketService.listMarketCatalogue([scope.item.id])

    scope.$on '$destroy', () ->
      WebSocketService.unSubscribeFromMarkets([scope.item.id], "BEST")

    scope.getBookData = () =>
      if angular.isDefined(DataModelService.marketBookData[scope.item.id]) then DataModelService.marketBookData[scope.item.id] else {}

    scope.catalogueData = DataModelService.marketCatalogueData
])
