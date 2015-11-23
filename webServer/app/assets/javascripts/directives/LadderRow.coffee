directivesModule.directive('ladderRow',  ['$log', ($log) ->
  templateUrl: '/assets/partials/ladderRow.html'
  scope: {
    data: '='
  }
  link: (scope, iElement, iAttrs) ->
    $log.debug 'ladder row directive'
])