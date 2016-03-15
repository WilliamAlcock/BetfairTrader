class MainCtrl

  constructor: (@$log, @$state) ->
    @$log.debug "Main Controller"

    @selected = @$state.current.url.split("/")[1]

    @tabs = [
      {name: "soccer", display: 'Soccer', state: 'init.main.soccer'},
      {name: "horseRacing", display: 'Horse Racing', state: 'init.main.horseRacing'}
    ]

    @select = (name, state) =>
      @selected = name
      @$state.go(state)

controllersModule.controller('MainCtrl', ['$log', '$state', MainCtrl])