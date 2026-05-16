package com.machikoro.client.ui.home

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.shop.PurchaseType
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
    fun createLobbyConnectsAndSendsCreateLobbyRequestAfterWebSocketConnects() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        viewModel.createLobby()

        assertTrue(fakeClient.connectCalled)
        assertFalse(fakeClient.sendCreateLobbyCalled)

        fakeClient.mutableConnectionStatus.value = ConnectionStatus.CONNECTED
        advanceUntilIdle()

        assertTrue(fakeClient.sendCreateLobbyCalled)
    }

    @Test
    fun createLobbyDoesNotSendCreateLobbyRequestWhenWebSocketFails() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        viewModel.createLobby()

        assertTrue(fakeClient.connectCalled)
        assertFalse(fakeClient.sendCreateLobbyCalled)

        fakeClient.mutableConnectionStatus.value = ConnectionStatus.ERROR
        advanceUntilIdle()

        assertFalse(fakeClient.sendCreateLobbyCalled)
    }

    @Test
    fun createLobbySendsCreateLobbyRequestWhenWebSocketIsConnected() {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        fakeClient.mutableConnectionStatus.value = ConnectionStatus.CONNECTED

        viewModel.createLobby()

        assertFalse(fakeClient.connectCalled)
        assertTrue(fakeClient.sendCreateLobbyCalled)
    }

    @Test
    fun startGameDelegatesToWebSocketClient() {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        viewModel.startGame()

        assertTrue(fakeClient.sendGameStartCalled)
    }

    @Test
    fun clearLobbyCodeClearsCurrentLobbyCode() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        fakeClient.mutableLobbyCode.value = "ABC123"

        assertEquals("ABC123", viewModel.lobbyCode.value)

        viewModel.clearLobbyCode()

        assertNull(viewModel.lobbyCode.value)
    }

    private class FakeWebSocketClient : WebSocketClient {
        val mutableConnectionStatus =
            MutableStateFlow(ConnectionStatus.IDLE)
        override val connectionStatus: StateFlow<ConnectionStatus> =
            mutableConnectionStatus
        override val gamePhase: StateFlow<GamePhase> =
            MutableStateFlow(GamePhase.NONE)
        override val diceResult: StateFlow<List<Int>?> =
            MutableStateFlow(null)
        override val activePlayerId: StateFlow<Int?> = // NEU
            MutableStateFlow(null)
        override val players: StateFlow<List<PlayerCoinState>> =
            MutableStateFlow(emptyList())
        val mutableLobbyCode = MutableStateFlow<String?>(null)
        override val lobbyCode: StateFlow<String?> = mutableLobbyCode
        val mutableActiveGameId = MutableStateFlow<Int?>(null)
        override val activeGameId: StateFlow<Int?> = mutableActiveGameId
        val mutableIsLobbyHost = MutableStateFlow(false)
        override val isLobbyHost: StateFlow<Boolean> = mutableIsLobbyHost
        override val gameStatus: StateFlow<GameStatus?> =
            MutableStateFlow(null)
        override val roundNumber: StateFlow<Int?> =
            MutableStateFlow(null)
        override val playerLandmarks: StateFlow<Map<Int, List<PlayerLandmarkState>>> =
            MutableStateFlow(emptyMap())
        override val marketplace: StateFlow<Map<CardType, Int>> =
            MutableStateFlow(emptyMap())

        override val authRejections: SharedFlow<Unit> = MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

        var connectCalled = false
        var disconnectCalled = false
        var sendGameStartCalled = false
        var sendCreateLobbyCalled = false

        override fun connect() { connectCalled = true }
        override fun disconnect() { disconnectCalled = true }
        override fun rollDice(diceCount: Int) = Unit
        override fun sendGameStart() { sendGameStartCalled = true }
        override fun sendCreateLobby() { sendCreateLobbyCalled = true }
        override fun sendPurchase(
            gameId: Int,
            purchaseType: PurchaseType,
            cardType: String?,
            landmarkType: String?
        ) = Unit
        override fun clearLobbyCode() { mutableLobbyCode.value = null }
    }
}

class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
