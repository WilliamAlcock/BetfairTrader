class DetailCtrl

  constructor: (@$log, @$scope, @$stateParams, @$state, @DataModelService) ->
    @$log.debug "Soccer Event Detail Controller"

    @navData = @DataModelService.getSoccerNavData(@$stateParams.coupon, @$stateParams.event)

    @$scope.$on 'soccerDataUpdated', @update

    @isOpen = {}

  update: () =>
    @navData = @DataModelService.getSoccerNavData(@$stateParams.coupon, @$stateParams.event)
    console.log('NEW NAV DATA', @navData)
    @$scope.$apply()

controllersModule.controller('soccerEventDetailCtrl', ['$log', '$scope', '$stateParams', '$state', 'DataModelService', DetailCtrl])