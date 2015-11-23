directivesModule.directive('runner',  ['$log', '$window', ($log, $window) ->
  templateUrl: '/assets/partials/runner.html'
  scope: {
    description: '@'
    avToBack: '='
    avToLay: '='
    bottomBorder: '='
  }
  link: (scope, iElement, iAttrs) ->
    scope.counter = 1

    options = 'height=310, width=300,menubar=no, resizable=no, scrollbars=no'

    scope.openLadder = () ->
      $window.open('ladder', 'ladder' + scope.counter, options, true)
      scope.counter++

    $log.debug 'runner directive'
])
