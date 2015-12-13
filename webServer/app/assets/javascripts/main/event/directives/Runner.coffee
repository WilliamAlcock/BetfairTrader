directivesModule.directive('runner',  ['$log', '$window', 'DataModelService', ($log, $window, DataModelService) ->
  templateUrl: '/assets/partials/main/event/directives/runner.html'
  scope: {
    id: '='
    catalogue: '='
    last: '='
  }
  link: (scope, iElement, iAttrs) ->
    options = 'height=310, width=300,menubar=no, resizable=no, scrollbars=no'
    url = 'ladder/' + scope.id + '/' + scope.catalogue.uniqueId

    scope.openLadder = () ->
      $window.open(url, scope.catalogue.runnerName, 'width=400, height=800, scrollbars=no', true)
      scope.counter++

    scope.bookData = DataModelService.marketBookData

    $log.debug 'runner directive'
])
