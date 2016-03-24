dependencies = [
    'ui.bootstrap',
    'myApp.filters',
    'myApp.services',
    'myApp.controllers',
    'myApp.directives',
    'myApp.common',
    'myApp.routeConfig',
    'ui.router',
    'ui.select',
    'ngSanitize'
]

ladderDependencies = [
  'ui.bootstrap',
  'myApp.filters',
  'myApp.services',
  'myApp.controllers',
  'myApp.directives',
  'myApp.common',
  'myLadder.routeConfig',
  'ui.router'
]

ladderApp = angular.module('myLadder', ladderDependencies)

angular.module('myLadder.routeConfig', ['ui.router'])
.config(['$stateProvider', '$urlRouterProvider', '$locationProvider',
    ($stateProvider, $urlRouterProvider, $locationProvider) ->
      $locationProvider.html5Mode({
        enabled: true,
        requireBase: false
      })

      $urlRouterProvider.otherwise('/ladder')

      $stateProvider
      .state('init', {
          abstract: true,
          resolve: {
            webSocket: (WebSocketService) ->
              WebSocketService.init()
          },
          template: '<ui-view/>'
        })
      .state('init.ladder', {
          parent: 'init',
          url: "/ladder/:marketId/:selectionId",
          templateUrl: '/assets/partials/ladder/ladder.html',
          controller: 'LadderCtrl as ladder'
        })

      $locationProvider.html5Mode(true)
  ])

app = angular.module('myApp', dependencies)

angular.module('myApp.routeConfig', ['ui.router'])
    .config(['$stateProvider', '$urlRouterProvider', '$locationProvider',
    ($stateProvider, $urlRouterProvider, $locationProvider) ->
        $locationProvider.html5Mode({
          enabled: true,
          requireBase: false
        })

        $urlRouterProvider.otherwise('/soccer')

        $stateProvider
          .state('init', {
            abstract: true,
            resolve: {
              webSocket: (WebSocketService) -> WebSocketService.init()
            },
            templateUrl: '/assets/partials/main/main.html',
            controller: 'MainCtrl as main'
          })
          .state('init.main', {
            abstract: true,
            views: {
              'position@init': {
                templateUrl: '/assets/partials/main/position.html',
                controller: 'positionCtrl as position'
              }
            }
          })
          .state('init.main.soccer', {
            url: "/soccer",
            onEnter: (WebSocketService) -> WebSocketService.getNavigationData("1"),
            views: {
              'navigation@init': {
                templateUrl: '/assets/partials/main/soccer/navigation.html',
                controller: 'soccerNavigationCtrl as nav'
              },
            },
          })
          .state('init.main.soccer.coupon', {
            url: "/:coupon",
            views: {
              'detail@init': {
                templateUrl: '/assets/partials/main/soccer/coupon/detail.html',
                controller: 'soccerCouponDetailCtrl as detail'
              },
            },
          })
          .state('init.main.soccer.coupon.event', {
            url: "/:event",
            data: {
              isSelected: {                       # TODO get this from config
                MATCH_ODDS:           true,
                CORRECT_SCORE:        false,
                OVER_UNDER_05:        true,
                OVER_UNDER_15:        true,
                OVER_UNDER_25:        true,
                OVER_UNDER_35:        true,
                OVER_UNDER_45:        true,
                OVER_UNDER_55:        true,
                OVER_UNDER_65:        true,
                OVER_UNDER_75:        true,
                OVER_UNDER_05:        true,
              }
            }
            views: {
              'navigation@init': {
                templateUrl: '/assets/partials/main/soccer/event/navigation.html',
                controller: 'soccerEventNavigationCtrl as nav'
              },
              'detail@init': {
                templateUrl: '/assets/partials/main/soccer/event/detail.html',
                controller: 'soccerEventDetailCtrl as detail'
              },
            },
          })

          .state('init.main.horseRacing', {
              url: "/horseRacing",
              onEnter: (WebSocketService) -> WebSocketService.getNavigationData("7")
              params: {},
              views: {
                'navigation@init': {
                  templateUrl: '/assets/partials/main/horseRacing/navigation.html',
                  controller: 'horseRacingNavigationCtrl as nav'
                },
                'detail@init': {
                  templateUrl: '/assets/partials/main/horseRacing/detail.html',
                  controller: 'horseRacingDetailCtrl as detail'
                },
              },
            })

#          .state('init.main.eventType', {
#            url: "/eventType",
#            params: {                                 # TODO These defaults should come from config
#              id: {value: 1},                         # Soccer
#              groupId: {value: "Today"}               # Today
#            },
#            views: {
#              'navigation@init.main': {
#                templateUrl: '/assets/partials/main/eventType/controllers/navigation.html',
#                controller: 'eventTypeNavigationCtrl as nav'
#              },
#              'detail@init.main': {
#                templateUrl: '/assets/partials/main/eventType/controllers/detail.html',
#                controller: 'eventTypeDetailCtrl as detail'
#              },
#              'position@init.main': {
#                templateUrl: '/assets/partials/main/eventType/controllers/position.html',
#                controller: 'eventTypePositionCtrl as position'
#              }
#            },
#          })
#          .state('init.main.event', {
#            url: "/event",
#            resolve: {
#              # TODO get this from config
#              isSelected: () -> {
#                MATCH_ODDS:           true,
#                CORRECT_SCORE:        false,
#                OVER_UNDER_05:        true,
#                OVER_UNDER_15:        true,
#                OVER_UNDER_25:        true,
#                OVER_UNDER_35:        true,
#                OVER_UNDER_45:        true,
#                OVER_UNDER_55:        true,
#                OVER_UNDER_65:        true,
#                OVER_UNDER_75:        true,
#                OVER_UNDER_05:        true,
#                HALF_TIME:            false,
#                HALF_TIME_SCORE:      false,
#                HALF_TIME_FULL_TIME:  false,
#                NEXT_GOAL:            false
#              }
#            },
#            params: {
#              eventId: {},
#            },
#            views: {
#              'navigation@init.main': {
#                templateUrl: '/assets/partials/main/event/controllers/navigation.html',
#                controller: 'eventNavigationCtrl as nav'
#              },
#              'detail@init.main': {
#                templateUrl: '/assets/partials/main/event/controllers/detail.html',
#                controller: 'eventDetailCtrl as detail'
#              },
#              'position@init.main': {
#                templateUrl: '/assets/partials/main/event/controllers/position.html',
#                controller: 'eventPositionCtrl as position'
#              }
#            },
#          })

        $locationProvider.html5Mode(true)
    ])

@commonModule = angular.module('myApp.common', [])
@controllersModule = angular.module('myApp.controllers', [])
@servicesModule = angular.module('myApp.services', [])
@modelsModule = angular.module('myApp.models', [])
@directivesModule = angular.module('myApp.directives', [])
@filtersModule = angular.module('myApp.filters', [])