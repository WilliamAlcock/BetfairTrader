class MainCtrl

  constructor: (@$log, @$state, DataModelService) ->
    @$log.debug "Main Controller"
    @navigationData = DataModelService.navData

    # TODO get this from config
    @visibleTabs = {
      1:    {active: true,  disabled: false}         # Soccer
      2:    {active: false, disabled: false}         # Tennis
      7:    {active: false, disabled: false}         # Horse Racing
      197:  {active: false, disabled: false}         # Cricket
      4339: {active: false, disabled: false}         # Greyhound Racing
      6231: {active: false, disabled: false}         # Financial Bets
    }

    @showEventType = (id) => angular.isDefined(@visibleTabs[id])

    @onTabSelected = (key) => @$state.go('init.main.eventType', {id: key, countryCode: "GB"})

controllersModule.controller('MainCtrl', ['$log', '$state', 'DataModelService', MainCtrl])