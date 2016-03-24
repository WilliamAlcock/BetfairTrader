class DetailCtrl

  constructor: (@$log, @$scope, @$stateParams, @$state, @DataModelService) ->
    @$log.debug "Soccer Detail Controller"

    @navData = @DataModelService.getSoccerNavData(@$stateParams.coupon)

    @$scope.$on 'soccerDataUpdated', @update

  selectEvent: (id) => @$state.go("init.main.soccer.coupon.event", {event: id.eventId})

  update: () =>
    @navData = @DataModelService.getSoccerNavData(@$stateParams.coupon)
    @$scope.$apply()

controllersModule.controller('soccerCouponDetailCtrl', ['$log', '$scope', '$stateParams', '$state', 'DataModelService', DetailCtrl])