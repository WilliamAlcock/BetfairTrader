class DataModelService

  constructor: (@$log, @$rootScope) ->
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
        output = (d for d in @navData.soccerData.allFootball when d.name is location[1])[0]      # get the country
        (d for d in output.navData when d.id is location[2])[0]                                  # get the event
      else {}

    if (event?)
      location = event.split "/"
      location.shift()                                       # remove the first index as that will be the last index of the coupon
      for l in location
        data = (d for d in data.children when d.id is l)[0]

    data


servicesModule.service('DataModelService', ['$log', '$rootScope', DataModelService])