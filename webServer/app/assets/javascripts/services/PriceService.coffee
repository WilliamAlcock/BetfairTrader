class PriceService

  constructor: (@$filter) ->

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

  # TODO this should be a filter


  # TODO add tests so no one ever breaks this !

  # I apologise to anyone who ever reads the code in the following 5 functions
  # In my defence I would like to point out how astonishingly bad javascript is at handling with numbers and maths
  # That is if whatever value you have stored is still a Number, it might through some magic have been converted to a String !

  round: (input, pow) => parseFloat(@$filter('number')(input, pow).replace(',', ''))

  trunc: (input, pow) => Math.trunc(input * Math.pow(10, pow)) / Math.pow(10, pow)

  getDp: (num) =>
    match = (''+num).match(/(?:\.(\d+))?(?:[eE]([+-]?\d+))?$/)
    if (!match) then 0 else Math.max(0, (if (match[1]) then match[1].length else 0) - (if (match[2]) then +match[2] else 0))

  isPriceValid: (price) ->
    if price < 1.01 then return false
    if price > 1000 then return false
    inc = @getPriceIncrement(price)
    mod = @trunc(price % inc, @getDp(price))
    mod == 0

  getClosestPriceAbove: (price) =>
    inc = @getPriceIncrement(price)
    mod = @trunc(price % inc, @getDp(price))
    @round(price + inc - mod, @getDp(inc))

  getClosestPriceBelow: (price) =>
    dec = @getPriceDecrement(price) - 0.009
    @getClosestPriceAbove(price + dec)

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

  servicesModule.service('PriceService', ['$filter', PriceService])