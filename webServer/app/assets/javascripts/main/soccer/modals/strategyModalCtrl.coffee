class StrategyModalCtrl

  constructor: (@$log, @$scope, @$uibModalInstance) ->
    @$log.debug "Strategy Modal Controller"

  ok: () => @$uibModalInstance.close({})

  cancel: () => @$uibModalInstance.dismiss('Cancel')

controllersModule.controller('StrategyModalCtrl', ['$log', '$scope', '$uibModalInstance', StrategyModalCtrl])