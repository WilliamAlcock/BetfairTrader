directivesModule.directive('matchOdds',  ['$log', 'WebSocketService', 'DataModelService', ($log, WebSocketService, DataModelService) ->
  templateUrl: '/assets/partials/main/soccer/directives/matchOdds.html'
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

    scope.getRunner = (id) =>
      if angular.isDefined(DataModelService.marketBookData[scope.item.id])
        DataModelService.marketBookData[scope.item.id].runners[id]

    scope.catalogueData = DataModelService.marketCatalogueData
    $log.log "catData", scope.catalogueData[scope.item.id], scope.item
])