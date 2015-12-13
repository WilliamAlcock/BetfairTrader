class NavigationCtrl

  constructor: (@$log, @$scope, @$stateParams, @$state, @DataModelService, @CountryNameService) ->
    @$log.debug "Event Type Navigation Controller"

    @data = @DataModelService.navData
    @countries = {}
    @topTenCountries = {}
    @internationals = {}

    @isOpen = {}

    @$scope.$watch 'data.root', @updateData

    @updateData()

  updateData: () =>
    if angular.isDefined(@data.root.children)
      eventType = (x for x in @data.root.children when x.id == @$stateParams.id)[0]
      countryGroups = @getCountryGroups(eventType)
      @internationals.data = countryGroups.Int.children.sort(@sortByMarkets)
      delete countryGroups.Int
      @countries.data = (@getCountryRow(key, val) for key, val of countryGroups).sort(@sortByMarkets)
      @topTenCountries.data = @countries.data[..9]
      @isOpen[x.id] = (@DataModelService.getGroup(x, @$stateParams.groupId)?) for x in @topTenCountries.data

  selectGroup: (groupId) ->
    @$state.go('init.main.eventType', {id: @$stateParams.id, groupId: groupId})

  sortByMarkets: (x,y) -> y.numberOfMarkets - x.numberOfMarkets

  getCountryGroups: (data) =>
    countryGroups = {}
    # get countries with marketCount
    for index, row of data.children
      countryCode = if (row.countryCode == "") then "Int" else row.countryCode
      if angular.isDefined(countryGroups[countryCode])
        countryGroups[countryCode].numberOfMarkets = countryGroups[countryCode].numberOfMarkets + row.numberOfMarkets
        if row.hasGroupGrandChildren
          countryGroups[countryCode].children = countryGroups[countryCode].children.concat(row.children)
        else
          countryGroups[countryCode].children.push(row)
        countryGroups[countryCode].hasGroupChildren = countryGroups[countryCode].hasGroupChildren || row.type == 'GROUP'
        countryGroups[countryCode].hasGroupGrandChildren = countryGroups[countryCode].hasGroupGrandChildren || row.hasGroupChildren
      else
        countryGroups[countryCode] = {
          numberOfMarkets: row.numberOfMarkets,
          children: if row.hasGroupGrandChildren then row.children else [row],
          hasGroupChildren: row.type == 'GROUP',
          hasGroupGrandChildren: row.hasGroupChildren
        }
    countryGroups

  getCountryRow: (key, val) => {
    id: key,
    name: @CountryNameService.data[key],
    numberOfMarkets: val.numberOfMarkets
    children: val.children
    hasGroupChildren: val.hasGroupChildren
    hasGroupGrandChildren: val.hasGroupGrandChildren
  }

controllersModule.controller('eventTypeNavigationCtrl', ['$log', '$scope', '$stateParams', '$state', 'DataModelService', 'CountryNameService', NavigationCtrl])