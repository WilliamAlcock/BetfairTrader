class DetailCtrl

  constructor: (@$log, @$scope, @$stateParams, @$state, @DataModelService) ->
    @$log.debug "Event Type Detail Controller"

    @data = @DataModelService.navData
    @viewData = []

    @$scope.$watch 'data.root', @updateViewData

  selectEvent: (eventId) =>
    @$state.go('init.main.event', {eventId: eventId.eventId})

  sortByTime: (a,b) -> new Date(a.startTime).getTime() - new Date(b.startTime).getTime()

  updateViewData: () =>
    @$log.log "updating view data"
    if angular.isDefined(@data.root.children)
      @viewData = if @$stateParams.groupId == 'Today'
        @DataModelService.getTodaysEvents(@data.root)
      else if @$stateParams.groupId == 'Tomorrow'
        @DataModelService.getTomorrowsEvents(@data.root)
      else
        @DataModelService.getGroup(@data.root, @$stateParams.groupId)

controllersModule.controller('eventTypeDetailCtrl', ['$log', '$scope', '$stateParams', '$state', 'DataModelService', DetailCtrl])