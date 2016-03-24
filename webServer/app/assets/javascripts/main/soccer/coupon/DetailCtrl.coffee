class DetailCtrl

  constructor: (@$log, @$scope, @$stateParams, @$state, @DataModelService) ->
    @$log.debug "Soccer Detail Controller"

    @navData = @DataModelService.getSoccerNavData(@$stateParams.coupon)

  selectEvent: (id) => @$state.go("init.main.soccer.coupon.event", {event: id.eventId})

controllersModule.controller('soccerCouponDetailCtrl', ['$log', '$scope', '$stateParams', '$state', 'DataModelService', DetailCtrl])