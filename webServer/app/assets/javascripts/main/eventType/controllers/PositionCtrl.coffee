class PositionCtrl

  constructor: (@$log, @$scope, @$stateParams, DataModelService) ->
    @$log.debug "Event Type Position Controller"

    @navigationData = DataModelService.navData

controllersModule.controller('eventTypePositionCtrl', ['$log', '$scope', '$stateParams', 'DataModelService', PositionCtrl])