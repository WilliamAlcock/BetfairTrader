class MarketsCtrl

  constructor: ($log, WebSocketService) ->
    $log.debug "Markets Controller"
    @data = [
      {
        description: 'Match Odds',
        open: true,
        runners: [
          {
            description: 'Home',
            avToBack: [ {price: 6.4, size: 419}, {price: 6.2, size: 315}, {price: 6, size: 490} ],
            avToLay:  [ {price: 6.6, size: 246}, {price: 6.8, size: 370}, {price: 7, size: 195} ]
          },
          {
            description: 'Away',
            avToBack: [ {price: 1.63, size: 1200}, {price: 1.62, size: 1513}, {price: 1.61, size: 782} ],
            avToLay: [ {price: 1.64, size: 276}, {price: 1.65, size: 975}, {price: 1.66, size: 2159} ]
          },
          {
            description: 'Draw',
            avToBack: [ {price: 4.2, size: 1370}, {price: 4.1, size: 1048}, {price: 4, size: 836} ],
            avToLay: [ {price: 4.3, size: 764}, {price: 4.4, size: 2929}, {price: 4.5, size: 518} ]
          },
        ]
      },
      {
        description: 'Match Odds',
        open: false,
        runners: [
          {
            description: 'Home',
            avToBack: [ {price: 6.4, size: 419}, {price: 6.2, size: 315}, {price: 6, size: 490} ],
            avToLay:  [ {price: 6.6, size: 246}, {price: 6.8, size: 370}, {price: 7, size: 195} ]
          },
          {
            description: 'Away',
            avToBack: [ {price: 1.63, size: 1200}, {price: 1.62, size: 1513}, {price: 1.61, size: 782} ],
            avToLay: [ {price: 1.64, size: 276}, {price: 1.65, size: 975}, {price: 1.66, size: 2159} ]
          },
          {
            description: 'Draw',
            avToBack: [ {price: 4.2, size: 1370}, {price: 4.1, size: 1048}, {price: 4, size: 836} ],
            avToLay: [ {price: 4.3, size: 764}, {price: 4.4, size: 2929}, {price: 4.5, size: 518} ]
          },
        ]
      },
      {
        description: 'Match Odds',
        open: false,
        runners: [
          {
            description: 'Home',
            avToBack: [ {price: 6.4, size: 419}, {price: 6.2, size: 315}, {price: 6, size: 490} ],
            avToLay:  [ {price: 6.6, size: 246}, {price: 6.8, size: 370}, {price: 7, size: 195} ]
          },
          {
            description: 'Away',
            avToBack: [ {price: 1.63, size: 1200}, {price: 1.62, size: 1513}, {price: 1.61, size: 782} ],
            avToLay: [ {price: 1.64, size: 276}, {price: 1.65, size: 975}, {price: 1.66, size: 2159} ]
          },
          {
            description: 'Draw',
            avToBack: [ {price: 4.2, size: 1370}, {price: 4.1, size: 1048}, {price: 4, size: 836} ],
            avToLay: [ {price: 4.3, size: 764}, {price: 4.4, size: 2929}, {price: 4.5, size: 518} ]
          },
        ]
      },
      {
        description: 'Match Odds',
        open: false,
        runners: [
          {
            description: 'Home',
            avToBack: [ {price: 6.4, size: 419}, {price: 6.2, size: 315}, {price: 6, size: 490} ],
            avToLay:  [ {price: 6.6, size: 246}, {price: 6.8, size: 370}, {price: 7, size: 195} ]
          },
          {
            description: 'Away',
            avToBack: [ {price: 1.63, size: 1200}, {price: 1.62, size: 1513}, {price: 1.61, size: 782} ],
            avToLay: [ {price: 1.64, size: 276}, {price: 1.65, size: 975}, {price: 1.66, size: 2159} ]
          },
          {
            description: 'Draw',
            avToBack: [ {price: 4.2, size: 1370}, {price: 4.1, size: 1048}, {price: 4, size: 836} ],
            avToLay: [ {price: 4.3, size: 764}, {price: 4.4, size: 2929}, {price: 4.5, size: 518} ]
          },
        ]
      }
    ]
    WebSocketService.sendMessage("HI BITCH IM THE MARKETS CONTROLLER !")

controllersModule.controller('MarketsCtrl', ['$log', 'WebSocketService', MarketsCtrl])