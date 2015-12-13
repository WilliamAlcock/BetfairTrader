class DataModelService

  constructor: (@$log, @$rootScope) ->
    @navData = { root: {} }
    @competitions = { lookup: {} }
    @marketBookData = {}
    @marketCatalogueData = {}
    @eventData = {}
    @eventTypeData = {}
    @monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"]

  setMarketBookData: (data) =>
    @marketBookData[data.marketId] = data

  setNavData: (navData, competitions) =>
    @navData.root = navData
    @competitions.lookup = {}
    for row in competitions
      @competitions.lookup[row.competition.id] =
        {name: row.competition.name, region: row.competitionRegion, marketCount: row.marketCount}

  isCompetition: (id) => angular.isDefined(@competitions.lookup[id])

  filterForId: (data, id) -> (x for x in data when x.id == id)

  getTodaysEvents: (data) => {
    name: "Todays Events"
    type: "GROUP"
    children: @getAllEventsForGroup(data, @getFixtureName(new Date()))
  }

  getTomorrowsEvents: (data) => {
    name: "Tomorrows Events"
    type: "GROUP"
    children: @getAllEventsForGroup(data, @getFixtureName(new Date(new Date().getTime() + (24 * 60 * 60 * 1000))))
  }

  getFixtureName: (date) =>
    day = date.getDate()
    if day < 10 then day = '0' + day
    month = date.getMonth()
    "Fixtures " + day + " " + @monthNames[month]

#  TODO Clean up the duplication in the methods below
  getAllEventsForGroup: (data, groupName) =>
    _getEventsForGroup = (data, groupName) =>
      if data.type == 'GROUP' && data.name.startsWith(groupName)
        data.children
      else if data.hasGroupChildren || data.hasGroupGrandChildren
        (data.children.map (x) -> _getEventsForGroup(x, groupName)).reduce((a,b) -> a.concat(b))
      else
        []

    _getEventsForGroup(data, groupName)

#  TODO rename as this also returns events
  getGroup: (data, groupId) =>
    _getGroup = (data, groupId) =>
      if (data.type == 'GROUP' || data.type == 'EVENT') && data.id == groupId
        [data]
      else if data.hasGroupChildren || data.hasGroupGrandChildren
        (data.children.map (x) -> _getGroup(x, groupId)).reduce((a,b) -> a.concat(b))
      else
        []
    groups = _getGroup(data, groupId)
    if groups.length > 0 then groups[0] else undefined

  getEvent: (data, eventId) =>
    _getEvent = (data, eventId) =>
      if data.type == "EVENT" && data.id == eventId
        [data]
      else if angular.isDefined(data.children)
        (data.children.map (x) -> _getEvent(x, eventId)).reduce((a,b) -> a.concat(b))
      else
        []
    events = _getEvent(data, eventId)
    if events.length > 0 then events[0] else undefined

  getMarket: (data, marketType) =>
    _getMarket = (data, marketType) =>
      if data.type == "MARKET" && (marketType == undefined || data.marketType == marketType)
        [data]
      else if angular.isDefined(data.children)
        markets = data.children.map (x) -> _getMarket(x, marketType)
        markets.reduce((a,b) -> a.concat(b))
      else
        []

    _getMarket(data, marketType)

  getMarketIds: (data, marketType) =>
    _getMarketIds = (data, marketType) =>
      if data.type == "MARKET" && (marketType == undefined || data.marketType == marketType)
        [data.id]
      else if angular.isDefined(data.children)
        markets = data.children.map (x) -> _getMarketIds(x, marketType)
        markets.reduce((a,b) -> a.concat(b))
      else
        []

    _getMarketIds(data, marketType)


servicesModule.service('DataModelService', ['$log', '$rootScope', DataModelService])