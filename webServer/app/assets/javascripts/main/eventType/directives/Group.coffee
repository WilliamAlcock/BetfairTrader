directivesModule.directive('group',  ['$log', '$compile', ($log, $compile) ->
  template: ''
  scope: {
    item: '='
    onEventClicked: '&'
    hideHeading: '='
  }
  link: (scope, element, attrs) ->
    $log.debug 'group item directive'

    sortByTime = (a,b) -> new Date(a.startTime).getTime() - new Date(b.startTime).getTime()

    scope.groups   = (x for x in scope.item.children when x.type == 'GROUP').sort(sortByTime)
    scope.events   = (x for x in scope.item.children when x.type == 'EVENT').sort(sortByTime)

    template = [
      '<div class="row group-item">'
        '<div ng-if="!hideHeading" class="heading">{{item.name}}</div>'
        '<div class="col-xs-12">'
          '<group ng-repeat="group in groups track by group.id" item="group" on-event-clicked="onEventClicked({eventId:eventId})"></group>'
          '<event ng-repeat="event in events track by event.id" item="event" on-event-clicked="onEventClicked({eventId:eventId})"></event>'
        '</div>'
      '</div>'
    ].join('')

    # template is compiled and added in link function because of recursive nature
    $compile(template)(scope, (cloned, scope) -> element.append(cloned))
])