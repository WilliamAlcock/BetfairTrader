class DetailCtrl

  constructor: (@$log, @$scope, @$stateParams, @$state, @DataModelService) ->
    @$log.debug "Soccer Event Detail Controller"

    @navData = @DataModelService.getSoccerNavData(@$stateParams.coupon, @$stateParams.event)

    @isOpen = {}

controllersModule.controller('soccerEventDetailCtrl', ['$log', '$scope', '$stateParams', '$state', 'DataModelService', DetailCtrl])