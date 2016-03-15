dateFilter = () -> (time) ->
  new Date(time).toTimeString().substr(0,5)

filtersModule.filter('dateFilter', [dateFilter])