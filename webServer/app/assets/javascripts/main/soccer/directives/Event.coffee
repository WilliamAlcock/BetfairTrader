directivesModule.directive('event',  ['$log', '$compile', 'DataModelService', ($log, $compile, DataModelService) ->
  templateUrl: ''
  scope: {
    item: '='
    onEventClicked: '&'
    listAllMarkets: '='
    hideHeading: '='
  }
  link: (scope, element) ->

    $log.debug 'event item directive'

    sortByTime = (a,b) -> new Date(a.startTime).getTime() - new Date(b.startTime).getTime()

    scope.groups   = (x for x in scope.item.children when x.type == 'GROUP').sort(sortByTime)
    scope.events   = (x for x in scope.item.children when x.type == 'EVENT').sort(sortByTime)
    scope.matchOdds = (x for x in scope.item.children when x.type == 'MARKET' && x.marketType == 'MATCH_ODDS')
    scope.markets  = (x for x in scope.item.children when x.type == 'MARKET' && x.marketType != 'MATCH_ODDS').sort(sortByTime)

    scope.bookData = DataModelService.marketBookData

    scope.startTime = new Date(scope.item.startTime).toTimeString().substr(0,5)

    $log.log "matchOdds", scope.matchOdds

    # id of the event clicked needs to be prefixed with the id of this group
    scope.eventClicked = (id) -> scope.onEventClicked({eventId: scope.item.id + "/" + id.eventId})

    template = [
      '<div class="row bf-event-item price-row" ng-if="bookData[matchOdds[0].id].status != \'CLOSED\'">'
        '<div ng-if="!hideHeading" class="col-lg-6 col-xs-12 price-label" ng-click="onEventClicked({eventId:item.id})">'
          '{{::item.name}}<br>'
          '<span class="time"> {{item.startTime | dateFilter}}</span>'
          '<span class="inplay" ng-if="bookData[matchOdds[0].id].inplay"> inplay</span>'
        '</div>'
        '<div ng-if="!listAllMarkets">'
          '<match-odds ng-if="matchOdds.length > 0" item="matchOdds[0]"></match-odds>'
          '<div ng-if="matchOdds.length == 0" class="text-center col-xs-6 no-match-odds" ng-click="onEventClicked({eventId:item.id})">'
            'NO MATCH ODDS</div>'
        '</div>'
        '<div class="col-xs-12">'
          '<group ng-repeat="group in groups track by group.id" item="group" on-event-clicked="eventClicked({eventId:eventId})"></group>'
          '<event ng-repeat="event in events track by event.id" item="event" on-event-clicked="eventClicked({eventId:eventId})"></event>'
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
])
