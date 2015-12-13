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
    data = data.filter (x) => max >= x.price >= min
    data.sort((a,b) -> b.price - a.price)

  mergeRow: (data, tradedVol, price) ->
    output = if data.length > 0 && price == data[0].price then data.shift() else {sizeToBack: 0, price: price, sizeToLay: 0}
    output.tradedVol = if tradedVol.length > 0 && price == tradedVol[0].price then tradedVol.shift().size else 0
    output

  getPriceData: (availableToBack, availableToLay, tradedVolume, center, depth) ->
    maxPrice = @getPriceAbove(center, depth + 1)
    minPrice = @getPriceBelow(center, depth)
    data = (availableToBack.map (x) -> {sizeToBack: x.size, price: x.price, sizeToLay: 0}).concat(
      (availableToLay.map (x) -> {sizeToBack: 0, price: x.price, sizeToLay: x.size}))

    data = @filterAndSort(data, minPrice, maxPrice)
    tradedVol = @filterAndSort(tradedVolume, minPrice, maxPrice)
    prices = (@getPriceBelow(maxPrice, x) for x in [0...((depth + 1) * 2)])

    output = []
    while prices.length > 0
      output.push(@mergeRow(data, tradedVol, prices.shift()))

    output

  servicesModule.service('PriceService', [PriceService])