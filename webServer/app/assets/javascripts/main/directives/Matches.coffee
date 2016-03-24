directivesModule.directive('matches',  ['$log', 'DataModelService', 'WebSocketService', ($log, DataModelService, WebSocketService) ->
  templateUrl: '/assets/partials/main/directives/matches.html'
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

    scope.getLiability = (match) => DataModelService.getLiability(match.side, match.price, match.size)

    scope.getProfit = (match) => DataModelService.getProfit(match.side, match.price, match.size)


    WebSocketService.listMarketCatalogue([scope.marketId])

    $log.debug 'Matches directive'
])