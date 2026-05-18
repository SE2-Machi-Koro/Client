package com.machikoro.client.network.websocket

import org.junit.Assert.assertEquals
import org.junit.Test

class WebSocketContractTest {
    @Test
    fun matchesCurrentServerEndpointAndDestinationSetup() {
        assertEquals("/ws", WebSocketContract.endpointPath)
        assertEquals("/app", WebSocketContract.appDestinationPrefix)
        assertEquals("/topic", WebSocketContract.topicDestinationPrefix)
        assertEquals("/queue", WebSocketContract.queueDestinationPrefix)
        assertEquals("/topic/public", WebSocketContract.publicTopic)
        assertEquals("/user/queue/errors", WebSocketContract.errorsQueue)
        assertEquals("/app/game.purchase", WebSocketContract.purchaseDestination)
        assertEquals("/user/queue/game-sync", WebSocketContract.gameSyncQueue)
        assertEquals("/queue/lobby-user", WebSocketContract.lobbyQueuePrefix)
        assertEquals("/topic/game", WebSocketContract.gameTopicPrefix)
    }
}
