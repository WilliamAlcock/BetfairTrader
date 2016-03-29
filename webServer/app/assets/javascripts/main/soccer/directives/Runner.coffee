directivesModule.directive('runner',  ['$log', '$window', '$uibModal', 'DataModelService', 'AutoTraderService', 'WebSocketService', ($log, $window, $uibModel, DataModelService, AutoTraderService, WebSocketService) ->
  templateUrl: '/assets/partials/main/soccer/directives/runner.html'
  scope: {
    id: '='
    catalogue: '='
    last: '='
  }
  link: (scope, iElement, iAttrs) ->
    url = '/ladder/' + scope.id + '/' + scope.catalogue.uniqueId

    scope.openLadder = () ->
      $log.log("opening ladder url", url, scope.catalogue.runnerName)
      $window.open(url, scope.catalogue.runnerName, 'width=400, height=570, scrollbars=no', true)
      scope.counter++

    scope.bookData = DataModelService.marketBookData

    scope.isStrategyRunning = () -> AutoTraderService.isStrategyRunning(scope.id, scope.catalogue.selectionId, scope.catalogue.handicap)

    scope.startStrategy = () =>
      modelInstance = $uibModel.open({
        animation: false,
        templateUrl: '/assets/partials/main/modals/strategy.html'
        controller: 'StrategyModalCtrl as mod'
        resolve: {
          marketId: () -> scope.id
          catalogue: () -> scope.catalogue
        }
        backdrop: false
        keyboard: true
        openedClass: 'bf-strategy-model-body'
        size: 'md'
        windowClass: 'bf-strategy-model-window'
        windowTopClass: 'bf-strategy-model-top-window'
      })

      modelInstance.result.then (params) ->
        params.marketId = scope.id
        params.selectionId = scope.catalogue.selectionId
        $log.log('sending params', params)
        WebSocketService.startStrategy(scope.id, scope.catalogue.selectionId, scope.catalogue.handicap, params)

    scope.stopStrategy = () -> WebSocketService.stopStrategy(scope.id, scope.catalogue.selectionId, scope.catalogue.handicap)

    $log.debug 'runner directive'
])
