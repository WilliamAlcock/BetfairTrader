class NavigationCtrl

  constructor: (@$log, @$scope, @$stateParams, @DataModelService, @isSelected) ->
    @$log.debug "Event Navigation Controller"

    @data = @DataModelService.navData
    @bookData = @DataModelService.marketBookData
    @viewData = {}

    @$scope.$watch 'data.root', @updateData

    @updateData()

  selectMarket: (id, $event) =>
    @isSelected[id] = if angular.isUndefined(@isSelected[id]) then true else !@isSelected[id]
    $event.stopPropagation()

  updateData: () =>
    @$log.log "updating event navigation data", @$stateParams
    if angular.isDefined(@data.root.children)
      @viewData = @DataModelService.getEvent(@data.root, @$stateParams.eventId)
      @$log.log "view data", @viewData

controllersModule.controller('eventNavigationCtrl', ['$log', '$scope', '$stateParams', 'DataModelService', 'isSelected', NavigationCtrl])