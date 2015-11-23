class EventsCtrl

  constructor: ($log, WebSocketService) ->
    $log.debug "Events Controller"
    @data = [
      {
        description:'Watford v Man Utd',
        startTime:'12:45',
        homeOdds:{avToBack: 4.8,  avToLay: 4.9},
        drawOdds:{avToBack: 3.5,  avToLay: 3.55},
        awayOdds:{avToBack: 1.94, avToLay: 1.95}
      },
      {
        description:'Chelsea v Norwich',
        startTime:'15:00',
        homeOdds:{avToBack: 1.45, avToLay: 1.46},
        drawOdds:{avToBack: 5,    avToLay: 5.1},
        awayOdds:{avToBack: 8.8,  avToLay: 9}
      },
      {
        description:'Everton v Aston Villa',
        startTime:'15:00',
        homeOdds:{avToBack: 1.6, avToLay: 1.61},
        drawOdds:{avToBack: 4.3, avToLay: 4.4},
        awayOdds:{avToBack: 6.8, avToLay: 7.2}
      },
      {
        description:'Newcastle v Leicester',
        startTime:'15:00',
        homeOdds:{avToBack: 2.96, avToLay: 2.98},
        drawOdds:{avToBack: 3.65, avToLay: 3.7},
        awayOdds:{avToBack: 2.56, avToLay: 2.58}
      },
      {
        description:'Southampton v Stoke',
        startTime:'15:00',
        homeOdds:{avToBack: 1.6, avToLay: 1.61},
        drawOdds:{avToBack: 4.2, avToLay: 4.3},
        awayOdds:{avToBack: 7.2, avToLay: 7.4}
      },
    ]
    WebSocketService.sendMessage("Subscribe")
    WebSocketService.sendMessage("ListEventTypes")


controllersModule.controller('EventsCtrl', ['$log', 'WebSocketService', EventsCtrl])