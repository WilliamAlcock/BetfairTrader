
dependencies = [
    'ui.bootstrap',
    'myApp.filters',
    'myApp.services',
    'myApp.controllers',
    'myApp.directives',
    'myApp.common',
    'myApp.routeConfig',
    'ui.router'
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
              console.log("I AM HERE")
              WebSocketService.init()
          },
          template: '<ui-view/>'
        })
      .state('ladder', {
          parent: 'init',
          url: "/ladder",
          templateUrl: '/assets/partials/ladder.html',
          controller: 'LadderCtrl as ladder'
        })
  ])

app = angular.module('myApp', dependencies)

angular.module('myApp.routeConfig', ['ui.router'])
    .config(['$stateProvider', '$urlRouterProvider', '$locationProvider',
    ($stateProvider, $urlRouterProvider, $locationProvider) ->
        $locationProvider.html5Mode({
          enabled: true,
          requireBase: false
        })

        $urlRouterProvider.otherwise('/events')

        $stateProvider
          .state('init', {
            abstract: true,
            resolve: {
              webSocket: (WebSocketService) ->
                WebSocketService.init()
            },
            template: '<ui-view/>'
          })
          .state('events', {
            parent: 'init',
            url: "/events",
            templateUrl: '/assets/partials/events.html',
            controller: 'EventsCtrl as events'
          })
          .state('markets', {
            parent: 'init',
            url: "/markets",
            templateUrl: '/assets/partials/markets.html',
            controller: 'MarketsCtrl as markets'
          })
    ])

@commonModule = angular.module('myApp.common', [])
@controllersModule = angular.module('myApp.controllers', [])
@servicesModule = angular.module('myApp.services', [])
@modelsModule = angular.module('myApp.models', [])
@directivesModule = angular.module('myApp.directives', [])
@filtersModule = angular.module('myApp.filters', [])