directivesModule.directive('eventTypeTabs',  ['$log', ($log) ->
  templateUrl: '/assets/partials/eventTypeTabs.html'
  scope: {}
  link: (scope, iElement, iAttrs) ->
    $log.debug 'event type tab directive'
    scope.tabs = [
      {title:'Football', active:true, disabled:false},
      {title:'Tennis', active:false, disabled:false},
      {title:'Horse Racing', active:false, disabled:false},
      {title:'Dog Racing', active:false, disabled:false},
      {title:'Cricket', active:false, disabled:false}
    ]
])
