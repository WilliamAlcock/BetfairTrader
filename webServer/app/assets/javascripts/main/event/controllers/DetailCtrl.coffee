class DetailCtrl

  constructor: (@$log, @$scope, @$stateParams, @DataModelService, @isSelected) ->
    @$log.debug "Event Detail Controller"

    @data = @DataModelService.navData
    @bookData = @DataModelService.marketBookData
    @viewData = {}
    @isOpen = {}

    @$scope.$watch 'data.root', @updateViewData

  updateViewData: () =>
    @$log.log "updating event view data", @$stateParams
    if angular.isDefined(@data.root.children)
      @viewData = @DataModelService.getEvent(@data.root, @$stateParams.eventId)
      @$log.log "view data", @viewData
      @isOpen[x.id] = true for x in @viewData.children when angular.isUndefined(@isOpen[x.id])

controllersModule.controller('eventDetailCtrl', ['$log', '$scope', '$stateParams', 'DataModelService', 'isSelected', DetailCtrl])