class StrategyModalCtrl

  constructor: (@$log, @$scope, @$uibModalInstance, @PriceService) ->
    @$log.debug "Strategy Modal Controller"

    @data = {
      size: 2
      layPrice: 1.01
      backPrice: 1.01
      stopPrice: 1.01
    }

  isFormInvalid: () =>
    @$log.log('isformvalid', @$scope.strategyForm)
    @$scope.strategyForm.$invalid

  isPriceValid: (price) => @PriceService.isPriceValid(price)

  snapPriceUp: (key) => @data[key] = @PriceService.getClosestPriceAbove(parseFloat(@$scope.strategyForm[key].$viewValue))

  snapPriceDown: (key) => @data[key] = @PriceService.getClosestPriceBelow(parseFloat(@$scope.strategyForm[key].$viewValue))

  ok: () => @$uibModalInstance.close(@data)

  cancel: () => @$uibModalInstance.dismiss('Cancel')

controllersModule.controller('StrategyModalCtrl', ['$log', '$scope', '$uibModalInstance', 'PriceService', StrategyModalCtrl])