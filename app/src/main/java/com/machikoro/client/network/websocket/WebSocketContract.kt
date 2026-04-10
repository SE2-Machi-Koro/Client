package com.machikoro.client.network.websocket

object WebSocketContract {
    const val endpointPath: String = "/ws"
    const val appDestinationPrefix: String = "/app"
    const val topicDestinationPrefix: String = "/topic"
    const val queueDestinationPrefix: String = "/queue"
    const val publicTopic: String = "/topic/public"
    const val errorQueue: String = "/queue/errors"
}
