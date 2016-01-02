class DataModelService

  constructor: (@$log, @$rootScope) ->
    @navData = { root: {} }
    @marketBookData = {}
    @marketCatalogueData = {}
    @eventData = {}
    @eventTypeData = {}
    @monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"]

  setNavData: (navData) => @navData.root = navData

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
    output = "Fixtures " + day + " " + @monthNames[month]
    @$log.log output
    output

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

  getEventType: (data, eventTypeId) =>
    _getEventType = (data,eventTypeId) =>
      if data.type == "EVENT_TYPE" && data.id == eventTypeId
        [data]
      else if angular.isDefined(data.children)
        (data.children.map (x) -> _getEventType(x, eventTypeId)).reduce((a,b) -> a.concat(b))
      else
        []
    eventTypes = _getEventType(data, eventTypeId)
    if eventTypes.length > 0 then eventTypes[0] else undefined

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
        (data.children.map (x) -> _getMarket(x, marketType)).reduce((a,b) -> a.concat(b))
      else
        []

    _getMarket(data, marketType)

  getMarketIds: (data, marketType) =>
    _getMarketIds = (data, marketType) =>
      if data.type == "MARKET" && (marketType == undefined || data.marketType == marketType)
        [data.id]
      else if angular.isDefined(data.children)
        (data.children.map (x) -> _getMarketIds(x, marketType)).reduce((a,b) -> a.concat(b))
      else
        []

    _getMarketIds(data, marketType)


servicesModule.service('DataModelService', ['$log', '$rootScope', DataModelService])