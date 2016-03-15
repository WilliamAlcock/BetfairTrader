class NavigationCtrl

  constructor: (@$log, @$scope, @$stateParams, @$state, @DataModelService) ->
    @$log.debug "Horse Racing Navigation Controller"

    @navData = @DataModelService.navData

    @menu = {
      stack: [['main']]
    }

  getState: () -> @menu.stack[@menu.stack.length - 1][0]

  # This is to avoid putting horseRacingData in menu.data before it has loaded
  getStateData: () ->
    if (@getState() == 'main')
      @navData.horseRacingData
    else
      @menu.stack[@menu.stack.length - 1][1]

  getStateChildren: () ->
    if (@getState() == 'main')
      @getStateData().groups
    else
      @getStateData().children

  selectGroup: (selected, id) =>
    switch selected
      when "Today"
        @$log.log('groin to today')
      when "Tomorrow"
        @$log.log('going to tomorrow')
      when "GROUP", "EVENT"
        data = (i for i in @getStateChildren() when i.id is id)[0]
        @$log.log('group selected', selected, id, data.type)
        @menu.stack.push(['group', data])
      when "goBack"
        @menu.stack.pop()
      when "MARKET"
        @$log.log('going to market', id)
      else null

controllersModule.controller('horseRacingNavigationCtrl', ['$log', '$scope', '$stateParams', '$state', 'DataModelService', NavigationCtrl])