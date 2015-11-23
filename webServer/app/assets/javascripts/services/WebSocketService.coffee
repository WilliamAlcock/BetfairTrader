class WebSocketService

  constructor: (@$log, @$q) ->
    @$log.debug "constructing Websocket service"

  init: () ->
    deferred = @$q.defer()

    # TODO get websocket address from config
    @ws = new WebSocket("ws://localhost:9000/socket")

    that = @

    @ws.onopen = ->
      that.$log.debug "WebSocket open"
      deferred.resolve()

    @ws.onclose = ->
      that.$log.debug "WebSocket closed"

    @ws.onerror = ->
      that.$log.debug "WebSocket error"

    @ws.onmessage = (message) ->
      that.$log.debug "WebSocket received message " + message.data

    deferred.promise

  sendMessage: (message) ->
    @ws.send(message)

servicesModule.service('WebSocketService', ['$log', '$q', WebSocketService])