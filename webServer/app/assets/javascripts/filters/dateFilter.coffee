dateFilter = () -> (time, fullDate) ->
  date = new Date(time).toTimeString()
  if (fullDate) then date else date.substr(0,5)

filtersModule.filter('dateFilter', [dateFilter])