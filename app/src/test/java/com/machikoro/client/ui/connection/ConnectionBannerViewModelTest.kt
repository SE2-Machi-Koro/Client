package com.machikoro.client.ui.connection

import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.session.Session
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.websocket.FakeWebSocketClient
import com.machikoro.client.ui.start.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionBannerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateIsHidden() = runTest {
        val viewModel = newViewModel()
        assertEquals(ConnectionBannerState.Hidden, viewModel.state.value)
    }

    @Test
    fun firstConnectingDoesNotShowBanner() = runTest {
        val client = FakeWebSocketClient()
        val viewModel = newViewModel(client = client)
        client.emitConnectionStatus(ConnectionStatus.CONNECTING)
        advanceUntilIdle()
        assertEquals(ConnectionBannerState.Hidden, viewModel.state.value)
    }

    @Test
    fun disconnectedWithoutPriorConnectedStaysHidden() = runTest {
        val client = FakeWebSocketClient()
        val viewModel = newViewModel(client = client)
        client.emitConnectionStatus(ConnectionStatus.DISCONNECTED)
        advanceUntilIdle()
        assertEquals(ConnectionBannerState.Hidden, viewModel.state.value)
    }

    @Test
    fun firstConnectedLeavesStateHidden() = runTest {
        val client = FakeWebSocketClient()
        val viewModel = newViewModel(client = client)
        client.emitConnectionStatus(ConnectionStatus.CONNECTING)
        client.emitConnectionStatus(ConnectionStatus.CONNECTED)
        advanceUntilIdle()
        assertEquals(ConnectionBannerState.Hidden, viewModel.state.value)
    }

    @Test
    fun dropAfterConnectedTransitionsToDisconnected() = runTest {
        val client = FakeWebSocketClient()
        val viewModel = newViewModel(client = client)
        client.emitConnectionStatus(ConnectionStatus.CONNECTED)
        advanceUntilIdle()
        client.emitConnectionStatus(ConnectionStatus.DISCONNECTED)
        advanceUntilIdle()
        assertEquals(ConnectionBannerState.Disconnected, viewModel.state.value)
    }

    @Test
    fun errorAfterConnectedTransitionsToDisconnected() = runTest {
        val client = FakeWebSocketClient()
        val viewModel = newViewModel(client = client)
        client.emitConnectionStatus(ConnectionStatus.CONNECTED)
        advanceUntilIdle()
        client.emitConnectionStatus(ConnectionStatus.ERROR)
        advanceUntilIdle()
        assertEquals(ConnectionBannerState.Disconnected, viewModel.state.value)
    }

    @Test
    fun reconnectShowsRecoveryAndReturnsToHidden() = runTest {
        val client = FakeWebSocketClient()
        val viewModel = newViewModel(client = client, reconnectedDisplayMs = 3_000L)
        client.emitConnectionStatus(ConnectionStatus.CONNECTED)
        advanceUntilIdle()
        client.emitConnectionStatus(ConnectionStatus.DISCONNECTED)
        advanceUntilIdle()
        client.emitConnectionStatus(ConnectionStatus.CONNECTED)
        runCurrent()
        assertEquals(ConnectionBannerState.Reconnected, viewModel.state.value)
        advanceTimeBy(3_001L)
        runCurrent()
        assertEquals(ConnectionBannerState.Hidden, viewModel.state.value)
    }

    @Test
    fun dropDuringRecoveryWindowCancelsTimerAndShowsDisconnected() = runTest {
        val client = FakeWebSocketClient()
        val viewModel = newViewModel(client = client, reconnectedDisplayMs = 3_000L)
        client.emitConnectionStatus(ConnectionStatus.CONNECTED)
        advanceUntilIdle()
        client.emitConnectionStatus(ConnectionStatus.DISCONNECTED)
        advanceUntilIdle()
        client.emitConnectionStatus(ConnectionStatus.CONNECTED)
        runCurrent()
        assertEquals(ConnectionBannerState.Reconnected, viewModel.state.value)
        client.emitConnectionStatus(ConnectionStatus.ERROR)
        advanceUntilIdle()
        assertEquals(ConnectionBannerState.Disconnected, viewModel.state.value)
    }

    @Test
    fun signOutResetsAndConnectingDoesNotFlashDisconnected() = runTest {
        val client = FakeWebSocketClient()
        val session = FakeSessionStateHolder().apply { signIn("t1", "u1", 1) }
        val viewModel = newViewModel(client = client, session = session)
        client.emitConnectionStatus(ConnectionStatus.CONNECTED)
        advanceUntilIdle()
        client.emitConnectionStatus(ConnectionStatus.DISCONNECTED)
        advanceUntilIdle()
        assertEquals(ConnectionBannerState.Disconnected, viewModel.state.value)
        session.signOut()
        advanceUntilIdle()
        assertEquals(ConnectionBannerState.Hidden, viewModel.state.value)
        session.signIn("t2", "u2", 2)
        client.emitConnectionStatus(ConnectionStatus.CONNECTING)
        advanceUntilIdle()
        assertEquals(ConnectionBannerState.Hidden, viewModel.state.value)
    }

    private fun newViewModel(
        client: FakeWebSocketClient = FakeWebSocketClient(),
        // Default to a signed-in session so the VM's logout-reset doesn't clear
        // hasEverConnectedThisSession out from under tests that assert the
        // state-machine. Tests that exercise sign-out pass their own holder.
        session: SessionStateHolder = FakeSessionStateHolder().apply { signIn("t", "u", 1) },
        reconnectedDisplayMs: Long = 3_000L,
    ) = ConnectionBannerViewModel(
        webSocketClient = client,
        sessionStateHolder = session,
        reconnectedDisplayMs = reconnectedDisplayMs,
    )

    private class FakeSessionStateHolder : SessionStateHolder {
        private val mutableSession = MutableStateFlow<Session?>(null)
        override val session: StateFlow<Session?> = mutableSession.asStateFlow()
        override fun signIn(token: String, username: String, userId: Int) {
            mutableSession.value = Session(token, username, userId)
        }
        override fun signOut() { mutableSession.value = null }
    }
}
