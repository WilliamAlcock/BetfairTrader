class PositionCtrl

  constructor: (@$log, @$scope, @$stateParams, DataModelService) ->
    @$log.debug "Event Position Controller"

controllersModule.controller('eventPositionCtrl', ['$log', '$scope', '$stateParams', 'DataModelService', PositionCtrl])