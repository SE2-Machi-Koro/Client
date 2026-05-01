package com.machikoro.client.ui.start

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartScreenViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateUsesPlaceholderValues() = runTest {
        val viewModel = StartScreenViewModel(FakeWebSocketClient())

        advanceUntilIdle()

        assertEquals("Machi Koro Client", viewModel.state.value.title)
        assertEquals(ConnectionStatus.IDLE, viewModel.state.value.connectionStatus)
    }

    @Test
    fun clientStatusUpdatesAreReflectedInScreenState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = StartScreenViewModel(fakeClient)

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

        override fun connect() = Unit

        override fun disconnect() = Unit

        fun emit(status: ConnectionStatus) {
            mutableConnectionStatus.value = status
        }
    }
}
