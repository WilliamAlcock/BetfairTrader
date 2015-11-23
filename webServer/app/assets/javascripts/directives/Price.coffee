directivesModule.directive('price',  ['$log', ($log) ->
  templateUrl: '/assets/partials/price.html'
  scope: {
    priceToBack: '='
    priceToLay: '='
  }
  link: (scope, iElement, iAttrs) ->
    $log.debug 'price directive'
])


