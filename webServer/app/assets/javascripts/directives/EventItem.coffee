directivesModule.directive('eventItem',  ['$log', ($log) ->
  templateUrl: '/assets/partials/eventItem.html'
  scope: {
    description: '@'
    homeOdds: '='
    drawOdds: '='
    awayOdds: '='
    bottomBorder: '='
  }
  link: (scope, iElement, iAttrs) ->
    $log.debug 'event directive'
])
