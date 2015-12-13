directivesModule.directive('eventItem',  ['$log', '$compile', 'WebSocketService', ($log, $compile, DataModelService) ->
  templateUrl: ''
  scope: {
    item: '='
    onEventClicked: '&'
    listAllMarkets: '='
  }
  link: (scope, element, attrs) ->

    $log.debug 'event item directive'

    sortByTime = (a,b) -> new Date(a.startTime).getTime() - new Date(b.startTime).getTime()

    scope.groups   = (x for x in scope.item.children when x.type == 'GROUP').sort(sortByTime)
    scope.events   = (x for x in scope.item.children when x.type == 'EVENT').sort(sortByTime)
    scope.matchOdds = (x for x in scope.item.children when x.type == 'MARKET' && x.marketType == 'MATCH_ODDS')
    scope.markets  = (x for x in scope.item.children when x.type == 'MARKET' && x.marketType != 'MATCH_ODDS').sort(sortByTime)

    template = [
      '<div class="row event-item" ng-if="bookData[matchOdds.id].status != \'CLOSED\'">'
        '<div class="col-lg-6 col-xs-12" ng-class="{undefined:\'price-label\', false:\'price-label\', true:\'heading\'}[listAllMarkets]"'
          if scope.listAllMarkets then '>' else 'ng-click="onEventClicked({eventId:item.id})">'
          '{{::item.name}}'
        '</div>'
        '<div ng-if="!listAllMarkets">'
          '<match-odds ng-if="matchOdds.length > 0" item="matchOdds[0]"></match-odds>'
          '<div ng-if="matchOdds.length == 0" class="text-center match-odds-placeholder" ng-click="onEventClicked({eventId:item.id})">'
            'NO MATCH ODDS - CLICK TO SEE OTHER MARKETS</div>'
        '</div>'
        '<div class="col-xs-12">'
          '<group-item ng-repeat="group in groups track by group.id" item="group" on-event-clicked="onEventClicked({eventId:eventId})"></group-item>'
          '<event-item ng-repeat="event in events track by event.id" item="event" on-event-clicked="onEventClicked({eventId:eventId})"></event-item>'
          '<div ng-if="listAllMarkets">'
            '<div>Others</div>'
            '<div ng-repeat="market in markets track by market.id" class="col-xs-12 price-label">'
              '{{market.name}}'
            '</div>'
          '</div>'
        '</div>'
      '</div>'
    ].join('')

    # template is compiled and added in link function because of recursive nature
    $compile(template)(scope, (cloned, scope) -> element.append(cloned))

#    '<span class="time" ng-if="item.hasMatchOdds">'
#    '{{item.startTime | dateFilter:bookData[matchOdds.id].status:bookData[matchOdds.id].inplay}}'
#    '</span>'
#    scope.catalogueData = DataModelService.marketCatalogueData
])
