package com.machikoro.client.ui.lobby

import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.LobbyStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.session.Session
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.websocket.FakeWebSocketClient
import com.machikoro.client.ui.start.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyScreenViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateUsesPlaceholderValues() = runTest {
        val viewModel = LobbyScreenViewModel(FakeWebSocketClient(), FakeSessionStateHolder())

        advanceUntilIdle()

        assertEquals(ConnectionStatus.IDLE, viewModel.state.value.connectionStatus)
        assertEquals(LobbyStatus.WAITING_FOR_PLAYERS, viewModel.state.value.lobbyStatus)
        assertEquals(emptyList<String>(), viewModel.state.value.playerList)
        assertEquals(4, viewModel.state.value.maxPlayers)
        assertFalse(viewModel.state.value.isHost)
        assertNull(viewModel.state.value.loggedInAs)
        assertFalse(viewModel.isReady.value)
    }

    @Test
    fun playersFlowUpdatesPlayerList() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder())

        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(id = "1", displayName = "alice", coins = 3),
                PlayerCoinState(id = "2", displayName = "bob", coins = 5),
            )
        )
        advanceUntilIdle()

        assertEquals(listOf("alice", "bob"), viewModel.state.value.playerList)
        assertEquals(LobbyStatus.READY, viewModel.state.value.lobbyStatus)
    }

    @Test
    fun lobbyStatusIsWaitingWhenLessThanTwoPlayers() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder())

        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(id = "1", displayName = "alice", coins = 3),
            )
        )
        advanceUntilIdle()

        assertEquals(listOf("alice"), viewModel.state.value.playerList)
        assertEquals(LobbyStatus.WAITING_FOR_PLAYERS, viewModel.state.value.lobbyStatus)
    }

    @Test
    fun clientStatusUpdatesAreReflectedInLobbyState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder())

        fakeClient.emitConnectionStatus(ConnectionStatus.CONNECTING)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.CONNECTING, viewModel.state.value.connectionStatus)

        fakeClient.emitConnectionStatus(ConnectionStatus.CONNECTED)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.CONNECTED, viewModel.state.value.connectionStatus)
    }

    @Test
    fun sessionUpdatesAreReflectedInLoggedInAs() = runTest {
        val sessionHolder = FakeSessionStateHolder()
        val viewModel = LobbyScreenViewModel(FakeWebSocketClient(), sessionHolder)

        sessionHolder.signIn(token = "uuid-123", username = "alice")
        advanceUntilIdle()
        assertEquals("alice", viewModel.state.value.loggedInAs)

        sessionHolder.signOut()
        advanceUntilIdle()
        assertNull(viewModel.state.value.loggedInAs)
    }

    @Test
    fun onReadyToggleTogglesReadyState() = runTest {
        val viewModel = LobbyScreenViewModel(FakeWebSocketClient(), FakeSessionStateHolder())

        assertFalse(viewModel.isReady.value)

        viewModel.onReadyToggle()
        assertTrue(viewModel.isReady.value)

        viewModel.onReadyToggle()
        assertFalse(viewModel.isReady.value)
    }

    /*@Test
    fun onStartGameDelegatesToWebSocketClientWhenHostAndEnoughPlayers() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder())

        // This test only works if you can set isHost=true in state.
        // If isHost is not updated from backend yet, skip this test for now.
    }*/

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