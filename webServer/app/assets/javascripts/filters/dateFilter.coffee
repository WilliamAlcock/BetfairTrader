dateFilter = () -> (time) ->
  date = new Date(time)
#  now = new Date()
#  diff = date.getTime() - now.getTime()
#  if diff < 0
#    diff/-60000
#  else
  date.toTimeString().substr(0,5)

filtersModule.filter('dateFilter', [dateFilter])