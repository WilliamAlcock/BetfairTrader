directivesModule.directive('matchOdds',  ['$log', 'WebSocketService', 'DataModelService', ($log, WebSocketService, DataModelService) ->
  templateUrl: '/assets/partials/main/eventType/directives/matchOdds.html'
  replace: true
  scope: {
    item: '='
  }
  link: (scope, iElement, iAttrs) ->
    $log.debug 'match Odds directive'

    WebSocketService.subscribeToMarkets([scope.item.id], "BEST")
    WebSocketService.listMarketCatalogue([scope.item.id])

    scope.$on '$destroy', () ->
      WebSocketService.unSubscribeFromMarkets([scope.item.id], "BEST")

    scope.bookData = DataModelService.marketBookData
    scope.catalogueData = DataModelService.catalogueData
])