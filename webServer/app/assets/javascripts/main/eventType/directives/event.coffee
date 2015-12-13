directivesModule.directive('event',  ['$log', '$compile', 'DataModelService', ($log, $compile, DataModelService) ->
  templateUrl: ''
  scope: {
    item: '='
    onEventClicked: '&'
    listAllMarkets: '='
    hideHeading: '='
  }
  link: (scope, element, attrs) ->

    $log.debug 'event item directive'

    sortByTime = (a,b) -> new Date(a.startTime).getTime() - new Date(b.startTime).getTime()

    scope.groups   = (x for x in scope.item.children when x.type == 'GROUP').sort(sortByTime)
    scope.events   = (x for x in scope.item.children when x.type == 'EVENT').sort(sortByTime)
    scope.matchOdds = (x for x in scope.item.children when x.type == 'MARKET' && x.marketType == 'MATCH_ODDS')
    scope.markets  = (x for x in scope.item.children when x.type == 'MARKET' && x.marketType != 'MATCH_ODDS').sort(sortByTime)

    scope.bookData = DataModelService.marketBookData

    scope.startTime = new Date(scope.item.startTime).toTimeString().substr(0,5)

#    | dateFilter:bookData[matchOdds.id].status:bookData[matchOdds.id].inplay

    template = [
      '<div class="row event-item price-row" ng-if="bookData[matchOdds[0].id].status != \'CLOSED\'">'
        '<div ng-if="!hideHeading" class="col-lg-6 col-xs-12 price-label" ng-click="onEventClicked({eventId:item.id})">'
          '{{::item.name}}'
          '<span class="time"> {{item.startTime | dateFilter}}</span>'
          '<span class="inplay" ng-if="bookData[matchOdds[0].id].inplay"> inplay</span>'
      '</div>'
        '<div ng-if="!listAllMarkets">'
          '<match-odds ng-if="matchOdds.length > 0" item="matchOdds[0]"></match-odds>'
          '<div ng-if="matchOdds.length == 0" class="text-center match-odds-placeholder" ng-click="onEventClicked({eventId:item.id})">'
            'NO MATCH ODDS - CLICK TO SEE OTHER MARKETS</div>'
        '</div>'
        '<div class="col-xs-12">'
          '<group ng-repeat="group in groups track by group.id" item="group" on-event-clicked="onEventClicked({eventId:eventId})"></group>'
          '<event ng-repeat="event in events track by event.id" item="event" on-event-clicked="onEventClicked({eventId:eventId})"></event>'
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

#    scope.catalogueData = DataModelService.marketCatalogueData
])
