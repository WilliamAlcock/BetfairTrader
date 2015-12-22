class PriceService

  constructor: () ->

  getPriceIncrement: (price) ->
    if price < 2 then return 0.01
    if price < 3 then return 0.02
    if price < 4 then return 0.05
    if price < 6 then return 0.1
    if price < 10 then return 0.2
    if price < 20 then return 0.5
    if price < 30 then return 1
    if price < 50 then return 2
    if price < 100 then return 5
    return 10

  getPriceDecrement: (price) ->
    if price > 100 then return -10
    if price > 50 then return -5
    if price > 30 then return -2
    if price > 20 then return -1
    if price > 10 then return -0.5
    if price > 6 then return -0.2
    if price > 4 then return -0.1
    if price > 3 then return -0.05
    if price > 2 then return -0.02
    return -0.01

  decrementPrice: (price) => Math.round((price + @getPriceDecrement(price)) * 100) / 100

  incrementPrice: (price) => Math.round((price + @getPriceIncrement(price)) * 100) / 100

  getPriceDelta: (price, ticks, deltaFunc) => if ticks > 0 then @getPriceDelta(deltaFunc(price), (ticks - 1), deltaFunc) else price

  getPriceBelow: (price, ticks) -> @getPriceDelta(price, ticks, @decrementPrice)

  getPriceAbove: (price, ticks) -> @getPriceDelta(price, ticks, @incrementPrice)

  filterAndSort: (data, min, max) ->
    (data.filter (x) => max >= x.price >= min).sort((a,b) -> b.price - a.price)

  getOrderVolume: (data) ->
    output = {}
    for order in data when order.status == "EXECUTABLE"
      if angular.isUndefined(output[order.price]) then output[order.price] = {}
      if angular.isUndefined(output[order.price][order.side])
        output[order.price][order.side] = order.size
      else
        output[order.price][order.side] += order.size
    ({price:parseFloat(key), back: val.BACK, lay: val.LAY} for key, val of output)

  mergeRow: (data, tradedVol, orders, price) ->
    output = if data.length > 0 && price == data[0].price then data.shift() else {sizeToBack: 0, price: price, sizeToLay: 0}
    output.tradedVol = if tradedVol.length > 0 && price == tradedVol[0].price then tradedVol.shift().size else 0
    order = if orders.length > 0 && price == orders[0].price then orders.shift() else {back: 0, lay: 0}
    console.log "ORDER", order
    output.ordersToBack = if angular.isUndefined(order.back) then 0 else order.back
    output.ordersToLay = if angular.isUndefined(order.lay) then 0 else order.lay
    output

  getPriceData: (availableToBack, availableToLay, tradedVolume, orders, center, depth) ->
    if (orders == null) then orders = []
    console.log "Orders", orders
    maxPrice = @getPriceAbove(center, depth + 1)
    minPrice = @getPriceBelow(center, depth)
    data = (availableToBack.map (x) -> {sizeToBack: x.size, price: x.price, sizeToLay: 0}).concat(
      (availableToLay.map (x) -> {sizeToBack: 0, price: x.price, sizeToLay: x.size}))

    data = @filterAndSort(data, minPrice, maxPrice)
    tradedVol = @filterAndSort(tradedVolume, minPrice, maxPrice)
    ordersByPrice = @filterAndSort(@getOrderVolume(orders), minPrice, maxPrice)
    prices = (@getPriceBelow(maxPrice, x) for x in [0...((depth + 1) * 2)])
    console.log
    output = []
    while prices.length > 0
      output.push(@mergeRow(data, tradedVol, ordersByPrice, prices.shift()))

    output

  servicesModule.service('PriceService', [PriceService])