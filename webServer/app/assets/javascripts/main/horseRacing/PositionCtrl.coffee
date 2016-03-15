class PositionCtrl

  constructor: (@$log, @$scope, @$stateParams) ->
    @$log.debug "Horse Racing Position Controller"

controllersModule.controller('horseRacingPositionCtrl', ['$log', '$scope', '$stateParams', PositionCtrl])