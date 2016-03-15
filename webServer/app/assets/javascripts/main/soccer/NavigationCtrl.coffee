class NavigationCtrl

  constructor: (@$log, @$scope, @$stateParams, @$state, @DataModelService) ->
    @$log.debug "Soccer Navigation Controller"

    @navData = @DataModelService.navData

    @menu = {
      name: 'main',               # main / allFootball / country
      country: '',                # if country is selected this is the data
      stack: [['main']]                   # where the data is in navData - this is used for going back & for the coupon
    }

  getCoupon: (stack, id) =>
    output = "allFootball"
    output = output + "/" + s[1] for s in stack when s.length > 1             # if s.length is > 1 then it must have an id to add to the coupon
    output + "/" + id

  selectGroup: (selected, id) =>
    switch selected
      when "Today"
        @$state.go("init.main.soccer.coupon", {coupon: 'Today'})
      when "Tomorrow"
        @$state.go("init.main.soccer.coupon", {coupon: 'Tomorrow'})
      when "allFootball"
        @menu.name = 'allFootball'
        @menu.stack.push(['allFootball'])
      when "country"
        @menu.name = 'country'
        @menu.stack.push(['country', id])
        @menu.country = (i for i in @navData.soccerData.allFootball when i.name is id)[0]
      when "event"
        @$state.go("init.main.soccer.coupon", {coupon: @getCoupon(@menu.stack, id)})
      when "goBack"
        @menu.stack.pop()
        @menu.name = @menu.stack[@menu.stack.length - 1][0]
      else null

controllersModule.controller('soccerNavigationCtrl', ['$log', '$scope', '$stateParams', '$state', 'DataModelService', NavigationCtrl])