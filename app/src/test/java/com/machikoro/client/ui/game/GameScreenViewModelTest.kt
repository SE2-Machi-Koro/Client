package com.machikoro.client.ui.game

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.network.websocket.WebSocketClient
import com.machikoro.client.ui.start.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameScreenViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateUsesInitialValues() = runTest {
        val viewModel = GameScreenViewModel(FakeWebSocketClient())

        advanceUntilIdle()

        assertEquals(GamePhase.NONE, viewModel.state.value.gamePhase)
        assertEquals(ConnectionStatus.IDLE, viewModel.state.value.connectionStatus)
    }

    @Test
    fun connectionStatusUpdatesAreReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = GameScreenViewModel(fakeClient)

        fakeClient.emitConnectionStatus(ConnectionStatus.CONNECTING)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.CONNECTING, viewModel.state.value.connectionStatus)

        fakeClient.emitConnectionStatus(ConnectionStatus.CONNECTED)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.CONNECTED, viewModel.state.value.connectionStatus)

        fakeClient.emitConnectionStatus(ConnectionStatus.ERROR)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.ERROR, viewModel.state.value.connectionStatus)
    }

    @Test
    fun gamePhaseUpdatesAreReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = GameScreenViewModel(fakeClient)

        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        advanceUntilIdle()
        assertEquals(GamePhase.ROLL_DICE, viewModel.state.value.gamePhase)

        fakeClient.emitGamePhase(GamePhase.RESOLVE_EFFECTS)
        advanceUntilIdle()
        assertEquals(GamePhase.RESOLVE_EFFECTS, viewModel.state.value.gamePhase)

        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        advanceUntilIdle()
        assertEquals(GamePhase.BUY_OR_BUILD, viewModel.state.value.gamePhase)

        fakeClient.emitGamePhase(GamePhase.END_TURN)
        advanceUntilIdle()
        assertEquals(GamePhase.END_TURN, viewModel.state.value.gamePhase)
    }

    @Test
    fun connectionStatusAndGamePhaseUpdateIndependently() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = GameScreenViewModel(fakeClient)

        fakeClient.emitConnectionStatus(ConnectionStatus.CONNECTED)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        advanceUntilIdle()

        assertEquals(ConnectionStatus.CONNECTED, viewModel.state.value.connectionStatus)
        assertEquals(GamePhase.ROLL_DICE, viewModel.state.value.gamePhase)

        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.CONNECTED, viewModel.state.value.connectionStatus)
        assertEquals(GamePhase.BUY_OR_BUILD, viewModel.state.value.gamePhase)

        fakeClient.emitConnectionStatus(ConnectionStatus.DISCONNECTED)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.DISCONNECTED, viewModel.state.value.connectionStatus)
        assertEquals(GamePhase.BUY_OR_BUILD, viewModel.state.value.gamePhase)
    }

    private class FakeWebSocketClient : WebSocketClient {
        override val connectionStatus: StateFlow<ConnectionStatus>
            get() = mutableConnectionStatus

        override val gamePhase: StateFlow<GamePhase>
            get() = mutableGamePhase

        private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
        private val mutableGamePhase = MutableStateFlow(GamePhase.NONE)

        override fun connect() = Unit

        override fun disconnect() = Unit

        fun emitConnectionStatus(status: ConnectionStatus) {
            mutableConnectionStatus.value = status
        }

        fun emitGamePhase(phase: GamePhase) {
            mutableGamePhase.value = phase
        }
    }
}
