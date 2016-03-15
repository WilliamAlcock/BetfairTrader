class DetailCtrl

  constructor: (@$log, @$scope, @$stateParams, @$state, @DataModelService) ->
    @$log.debug "Horse Racing Detail Controller"

controllersModule.controller('horseRacingDetailCtrl', ['$log', '$scope', '$stateParams', '$state', 'DataModelService', DetailCtrl])