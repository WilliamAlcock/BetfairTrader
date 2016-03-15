directivesModule.directive('runner',  ['$log', '$window', 'DataModelService', ($log, $window, DataModelService) ->
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

    $log.debug 'runner directive'
])
