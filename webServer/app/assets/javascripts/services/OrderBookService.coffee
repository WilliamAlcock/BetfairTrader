class OrderBookService

  constructor: (@$log) ->
    @orders = {}

    @matches = {}

    @audio = new Audio('/assets/audio/Note8.wav');

  setData: (collection, marketId, selectionId, handicap, id, data) =>
    if (!collection[marketId]?)
      collection[marketId] = {}
    if (!collection[marketId][selectionId]?)
      collection[marketId][selectionId] = {}
    if (!collection[marketId][selectionId][handicap]?)
      collection[marketId][selectionId][handicap] = {}

    collection[marketId][selectionId][handicap][id] = data

  setOrders: (currentOrders) => @setData(@orders, order.marketId, order.selectionId, order.handicap, order.betId, order) for order in currentOrders

  setMatches: (currentMatches) => @setData(@matches, m.marketId, m.selectionId, m.handicap, m._match.side, m._match) for m in currentMatches

  orderPlaced: (update) => @setData(@orders, update.marketId, update.selectionId, update.handicap, update.order.betId, update.order)

  orderUpdated: (update) => @setData(@orders, update.marketId, update.selectionId, update.handicap, update.order.betId, update.order)

  orderExecuted: (update) =>
    delete @orders[update.marketId][update.selectionId][update.handicap][update.order.betId]
    if (Object.keys(@orders[update.marketId][update.selectionId][update.handicap]).length == 0)
      delete @orders[update.marketId][update.selectionId][update.handicap]
    if (Object.keys(@orders[update.marketId][update.selectionId]).length == 0)
      delete @orders[update.marketId][update.selectionId]
    if (Object.keys(@orders[update.marketId]).length == 0)
      delete @orders[update.marketId]

  orderMatched: (update) =>
    @setData(@matches, update.marketId, update.selectionId, update.handicap, update._match.side, update._match)
    @audio.play()

servicesModule.service('OrderBookService', ['$log', OrderBookService])