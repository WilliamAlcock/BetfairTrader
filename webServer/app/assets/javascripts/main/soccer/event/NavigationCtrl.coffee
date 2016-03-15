class NavigationCtrl

  constructor: (@$log, @$scope, @$stateParams, @$state, @DataModelService) ->
    @$log.debug "Soccer Event Navigation Controller"

    @navData = @DataModelService.getSoccerNavData(@$stateParams.coupon, @$stateParams.event)

  selected: (id) ->
    @$log.log("selected", id, @$state.current.data.isSelected[id])
    if (@$state.current.data.isSelected[id]?)
      @$state.current.data.isSelected[id] = !@$state.current.data.isSelected[id]
    else
      @$state.current.data.isSelected[id] = true

controllersModule.controller('soccerEventNavigationCtrl', ['$log', '$scope', '$stateParams', '$state', 'DataModelService', NavigationCtrl])