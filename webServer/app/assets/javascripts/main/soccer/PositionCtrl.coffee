class PositionCtrl

  constructor: (@$log, @$scope, @$stateParams) ->
    @$log.debug "Soccer Position Controller"

controllersModule.controller('soccerPositionCtrl', ['$log', '$scope', '$stateParams', PositionCtrl])