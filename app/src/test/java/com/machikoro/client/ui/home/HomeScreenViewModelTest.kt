package com.machikoro.client.ui.home

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenViewModelTest {

    @Test
    fun exposesLobbyCodeFromWebSocketClient() {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        fakeClient.mutableLobbyCode.value = "AJ25Z39"

        assertEquals("AJ25Z39", viewModel.lobbyCode.value)
    }

    @Test
    fun exposesActiveGameIdAndHostStateFromWebSocketClient() {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        fakeClient.mutableActiveGameId.value = 42
        fakeClient.mutableIsLobbyHost.value = true

        assertEquals(42, viewModel.activeGameId.value)
        assertTrue(viewModel.isLobbyHost.value)
    }

    @Test
    fun createLobbyConnectsAndSendsCreateLobbyRequest() {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        viewModel.createLobby()

        assertTrue(fakeClient.connectCalled)
        assertTrue(fakeClient.sendCreateLobbyCalled)
    }

    @Test
    fun startGameDelegatesToWebSocketClient() {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        viewModel.startGame()

        assertTrue(fakeClient.sendGameStartCalled)
    }

    private class FakeWebSocketClient : WebSocketClient {
        override val connectionStatus: StateFlow<ConnectionStatus> =
            MutableStateFlow(ConnectionStatus.IDLE)

        override val gamePhase: StateFlow<GamePhase> =
            MutableStateFlow(GamePhase.NONE)

        override val players: StateFlow<List<PlayerCoinState>> =
            MutableStateFlow(emptyList())

        val mutableLobbyCode = MutableStateFlow<String?>(null)
        override val lobbyCode: StateFlow<String?> = mutableLobbyCode
        val mutableActiveGameId = MutableStateFlow<Int?>(null)
        override val activeGameId: StateFlow<Int?> = mutableActiveGameId
        val mutableIsLobbyHost = MutableStateFlow(false)
        override val isLobbyHost: StateFlow<Boolean> = mutableIsLobbyHost

        var connectCalled = false
        var disconnectCalled = false
        var sendGameStartCalled = false
        var sendCreateLobbyCalled = false

        override fun connect() {
            connectCalled = true
        }

        override fun disconnect() {
            disconnectCalled = true
        }

        override fun sendGameStart() {
            sendGameStartCalled = true
        }

        override fun sendCreateLobby() {
            sendCreateLobbyCalled = true
        }
    }
}
