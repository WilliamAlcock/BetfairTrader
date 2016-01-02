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

  getPriceDelta: (price, ticks, deltaFunc) => if ticks > 0 then @getPriceDelta(deltaFunc(price), ticks - 1, deltaFunc) else price

  formatPrice: (price) -> if price.toString().indexOf('.') == -1 then price + ".0" else price + ""

  getPriceBelow: (price, ticks) -> @formatPrice(@getPriceDelta(price, ticks, @decrementPrice))

  getPriceAbove: (price, ticks) -> @formatPrice(@getPriceDelta(price, ticks, @incrementPrice))

  getLadderPrices: (center, depth) ->
    prices = [@formatPrice(center)]
    prices.unshift(@getPriceAbove(Number(prices[0]), 1)) for [0...depth + 1]
    prices.push(@getPriceBelow(Number(prices[prices.length - 1]), 1)) for [0...depth]
    prices

  scrollUp: (prices) ->
    prices.pop()
    prices.unshift(@getPriceAbove(Number(prices[0]), 1))
    prices

  scrollDown: (prices) ->
    prices.shift()
    prices.push(@getPriceBelow(Number(prices[prices.length - 1]), 1))
    prices

  servicesModule.service('PriceService', [PriceService])