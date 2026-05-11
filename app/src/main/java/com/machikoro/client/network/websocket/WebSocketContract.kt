package com.machikoro.client.network.websocket

object WebSocketContract {
    const val endpointPath: String = "/ws"
    const val stompVersion: String = "1.2"
    const val appDestinationPrefix: String = "/app"
    const val topicDestinationPrefix: String = "/topic"
    const val queueDestinationPrefix: String = "/queue"
    const val publicTopic: String = "/topic/public"
    const val gameTopic: String = "/topic/game"
    const val errorQueue: String = "/queue/errors"
    const val addUserDestination: String = "/app/chat.addUser"
    const val chatSendDestination: String = "/app/chat.send"
    const val rollDiceDestination: String = "/app/game.rollDice"
    const val createLobbyDestination: String = "/app/lobby.create"
    const val gameStartDestination: String = "/app/game.start"
    const val defaultSender: String = "android-client"
}
