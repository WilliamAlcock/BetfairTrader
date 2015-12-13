class CountryNameService

  constructor: (@$log, @$http, @$q) ->
    @$log.debug "Country Name Service"

    # TODO get this url from config
    @data = {}

  init: () =>
    deferred = @$q.defer()
    @$http.get("http://localhost:9000/assets/data/countryNames.json").then(
      (response) =>
        for index, row of response.data
          @data[row.Code] = row.Name
        @data.Int = "International"
        deferred.resolve()
    )
    deferred.promise

servicesModule.service('CountryNameService', ['$log', '$http', '$q', CountryNameService])