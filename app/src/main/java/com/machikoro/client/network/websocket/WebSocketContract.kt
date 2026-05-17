package com.machikoro.client.network.websocket

object WebSocketContract {
    const val endpointPath: String = "/ws"
    const val stompVersion: String = "1.2"
    const val appDestinationPrefix: String = "/app"
    const val topicDestinationPrefix: String = "/topic"
    const val queueDestinationPrefix: String = "/queue"
    const val publicTopic: String = "/topic/public"
    const val gameTopicPrefix: String = "/topic/game"
    const val errorsQueue: String = "/user/queue/errors"
    const val addUserDestination: String = "/app/chat.addUser"
    const val chatSendDestination: String = "/app/chat.send"
    const val rollDiceDestination: String = "/app/game.rollDice"
    const val createLobbyDestination: String = "/app/lobby.create"
    const val joinLobbyDestination = "/app/lobby.join"
    const val gameStartDestination: String = "/app/game.start"
    // Server PR #216 exposes PurchaseRequest at @MessageMapping("/game.purchase").
    const val purchaseDestination: String = "/app/game.purchase"
    const val gameSyncDestination: String = "/app/game.sync"
    // Per-user reconnect snapshot queue. The server resolves /user/** to the
    // authenticated principal, so each client only sees its own snapshot.
    const val gameSyncQueue: String = "/user/queue/game-sync"
    const val defaultSender: String = "android-client"
}