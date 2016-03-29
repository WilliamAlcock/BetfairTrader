class StrategyModalCtrl

  constructor: (@$log, @$scope, @$uibModalInstance, @DataModelService, @PriceService, @marketId, @catalogue) ->
    @$log.debug "Strategy Modal Controller"

    @data = {
      startSide: "BACK"
      size: 2
      layPrice: 1.01
      backPrice: 1.01
      stopPrice: 1.01
    }

    @bookData = @DataModelService.marketBookData
    @marketCatalogue = @DataModelService.marketCatalogueData

    @disabled = {
      startTime: true,
      stopTime: true
    }

  isFormInvalid: () => @$scope.strategyForm.$invalid

  isPriceValid: (price) => @PriceService.isPriceValid(price)

  snapPriceUp: (key) => @data[key] = @PriceService.getClosestPriceAbove(parseFloat(@$scope.strategyForm[key].$viewValue))

  snapPriceDown: (key) => @data[key] = @PriceService.getClosestPriceBelow(parseFloat(@$scope.strategyForm[key].$viewValue))

  disable: (key) =>
    @disabled[key] = true
    delete @data[key]

  enable: (key) =>
    @data[key] = @marketCatalogue[@marketId].marketStartTime
    @disabled[key] = false

  ok: () => @$uibModalInstance.close(@data)

  cancel: () => @$uibModalInstance.dismiss('Cancel')

controllersModule.controller('StrategyModalCtrl', ['$log', '$scope', '$uibModalInstance', 'DataModelService', 'PriceService', 'marketId', 'catalogue', StrategyModalCtrl])