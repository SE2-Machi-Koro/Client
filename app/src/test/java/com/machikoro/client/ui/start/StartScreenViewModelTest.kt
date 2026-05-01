package com.machikoro.client.ui.start

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.session.Session
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartScreenViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateUsesPlaceholderValues() = runTest {
        val viewModel = StartScreenViewModel(FakeWebSocketClient(), FakeSessionStateHolder())

        advanceUntilIdle()

        assertEquals("Machi Koro Client", viewModel.state.value.title)
        assertEquals(ConnectionStatus.IDLE, viewModel.state.value.connectionStatus)
        assertNull(viewModel.state.value.loggedInAs)
    }

    @Test
    fun clientStatusUpdatesAreReflectedInScreenState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = StartScreenViewModel(fakeClient, FakeSessionStateHolder())

        fakeClient.emit(ConnectionStatus.CONNECTING)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.CONNECTING, viewModel.state.value.connectionStatus)

        fakeClient.emit(ConnectionStatus.CONNECTED)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.CONNECTED, viewModel.state.value.connectionStatus)

        fakeClient.emit(ConnectionStatus.ERROR)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.ERROR, viewModel.state.value.connectionStatus)
    }

    @Test
    fun sessionUpdatesAreReflectedInLoggedInAs() = runTest {
        val sessionHolder = FakeSessionStateHolder()
        val viewModel = StartScreenViewModel(FakeWebSocketClient(), sessionHolder)

        sessionHolder.signIn(token = "uuid-123", username = "alice")
        advanceUntilIdle()
        assertEquals("alice", viewModel.state.value.loggedInAs)

        sessionHolder.signOut()
        advanceUntilIdle()
        assertNull(viewModel.state.value.loggedInAs)
    }

    @Test
    fun playersFlowUpdatesPlayerList() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = StartScreenViewModel(fakeClient, FakeSessionStateHolder())

        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(id = "1", displayName = "alice", coins = 3),
                PlayerCoinState(id = "2", displayName = "bob", coins = 5),
            )
        )
        advanceUntilIdle()

        assertEquals(listOf("alice", "bob"), viewModel.state.value.playerList)
    }

    @Test
    fun playerListIsEmptyInitially() = runTest {
        val viewModel = StartScreenViewModel(FakeWebSocketClient(), FakeSessionStateHolder())

        advanceUntilIdle()

        assertEquals(emptyList<String>(), viewModel.state.value.playerList)
    }

    @Test
    fun onStartGameDelegatesToWebSocketClient() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = StartScreenViewModel(fakeClient, FakeSessionStateHolder())

        viewModel.onStartGame()

        assertTrue(fakeClient.gameStartSent)
    }

    private class FakeWebSocketClient : WebSocketClient {
        override val connectionStatus: StateFlow<ConnectionStatus>
            get() = mutableConnectionStatus

        override val gamePhase: StateFlow<GamePhase>
            get() = mutableGamePhase

        override val players: StateFlow<List<PlayerCoinState>>
            get() = mutablePlayers

        private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
        private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)
        private val mutablePlayers = MutableStateFlow<List<PlayerCoinState>>(emptyList())

        var gameStartSent = false

        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun sendGameStart() {
            gameStartSent = true
        }

        fun emit(status: ConnectionStatus) {
            mutableConnectionStatus.value = status
        }

        fun emitPlayers(players: List<PlayerCoinState>) {
            mutablePlayers.value = players
        }
    }

    private class FakeSessionStateHolder : SessionStateHolder {
        private val mutableSession = MutableStateFlow<Session?>(null)
        override val session: StateFlow<Session?> = mutableSession.asStateFlow()
        override fun signIn(token: String, username: String) {
            mutableSession.value = Session(token, username)
        }
        override fun signOut() {
            mutableSession.value = null
        }
    }
}
