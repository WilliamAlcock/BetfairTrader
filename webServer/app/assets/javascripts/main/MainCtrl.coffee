class MainCtrl

  constructor: (@$log, @$state) ->
    @$log.debug "Main Controller"

    @selected = @$state.current.name.split(".")[2]

    console.log('SELECTED TAB', @selected, @$state.current)

    @tabs = [
      {name: "soccer", display: 'Soccer', state: 'init.main.soccer'},
      {name: "horseRacing", display: 'Horse Racing', state: 'init.main.horseRacing'}
    ]

    @select = (name, state) =>
      @selected = name
      @$state.go(state)

controllersModule.controller('MainCtrl', ['$log', '$state', MainCtrl])