class DataModelService

  constructor: (@$log) ->
    @navData = {
      soccerData: {},
      horseRacingData: {}
    }
    @marketBookData = {}
    @marketCatalogueData = {}

  setSoccerData: (soccerData) =>
    @$log.log("setting soccer data", soccerData)
    @navData.soccerData = soccerData

  setHorseRacingData: (horseRacingData) =>
    @$log.log("setting horse racing data", horseRacingData)
    @navData.horseRacingData = horseRacingData

  setMarketBookData: (marketId, data) => @marketBookData[marketId] = data

  setMarketCatalogueData: (marketId, data) => @marketCatalogueData[marketId] = data

  # return the nav data for the given coupon
  getSoccerNavData: (coupon, event) ->
    location = coupon.split "/"
    data = switch location[0]
      when "Today"
        {
          children: @navData.soccerData.today
          name: "Today's Fixtures"
          type: "EVENT"
        }
      when "Tomorrow"
        {
          children: @navData.soccerData.tomorrow
          name: "Tomorrow's Fixtures"
          type: "EVENT"
        }
      when "allFootball"
        if (@navData.soccerData.allFootball?)
          output = (d for d in @navData.soccerData.allFootball when d.name is location[1])[0]      # get the country
          (d for d in output.navData when d.id is location[2])[0]                                  # get the event
        else null
      else null

    if (event? && @navData.soccerData.allFootball?)
      location = event.split "/"
      location.shift()                                       # remove the first index as that will be the last index of the coupon
      for l in location
        data = (d for d in data.children when d.id is l)[0]

    data

  getRunnerCatalogue: (marketId, selectionId) =>
    if (@marketCatalogueData[marketId]?)
      (rc for rc in @marketCatalogueData[marketId].runners when rc.selectionId is selectionId)[0]
    else {}

  getLiability: (side, price, size) =>
    switch side
      when "BACK" then size
      when "LAY" then (price - 1) * size
      else null

  getProfit: (side, price, size) =>
    switch side
      when "BACK" then size * (price - 1)
      when "LAY" then size
      else null



servicesModule.service('DataModelService', ['$log', DataModelService])