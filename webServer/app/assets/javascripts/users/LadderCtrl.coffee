class LadderCtrl

  constructor: ($log, WebSocketService) ->
    $log.debug "Ladder Controller"
    @data = [
      {price: 0.30, sizeToBack: 269,  sizeToLay: 0,     ordersToBack: 0, ordersToLay: 0, totalTraded: 25056},
      {price: 0.29, sizeToBack: 349,  sizeToLay: 0,     ordersToBack: 0, ordersToLay: 0, totalTraded: 34256},
      {price: 0.28, sizeToBack: 1405, sizeToLay: 0,     ordersToBack: 0, ordersToLay: 0, totalTraded: 43965},
      {price: 0.27, sizeToBack: 6542, sizeToLay: 0,     ordersToBack: 3, ordersToLay: 0, totalTraded: 65496},
      {price: 0.26, sizeToBack: 2545, sizeToLay: 0,     ordersToBack: 5, ordersToLay: 0, totalTraded: 30000},
      {price: 0.25, sizeToBack: 0,    sizeToLay: 2375,  ordersToBack: 0, ordersToLay: 2, totalTraded: 25000},
      {price: 0.24, sizeToBack: 0,    sizeToLay: 4934,  ordersToBack: 0, ordersToLay: 9, totalTraded: 10346},
      {price: 0.23, sizeToBack: 0,    sizeToLay: 3824,  ordersToBack: 0, ordersToLay: 0, totalTraded: 986},
      {price: 0.22, sizeToBack: 0,    sizeToLay: 324,   ordersToBack: 0, ordersToLay: 0, totalTraded: 875},
      {price: 0.21, sizeToBack: 0,    sizeToLay: 285,   ordersToBack: 0, ordersToLay: 0, totalTraded: 0},
    ]
    WebSocketService.sendMessage("HEY MO FO IM THE LADDER CONTROLLER !")

controllersModule.controller('LadderCtrl', ['$log', 'WebSocketService', LadderCtrl])