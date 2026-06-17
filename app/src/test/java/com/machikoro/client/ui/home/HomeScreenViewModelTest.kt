package com.machikoro.client.ui.home

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.state.ChatMessageState
import com.machikoro.client.network.error.ClientError
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
import kotlinx.coroutines.test.advanceTimeBy
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
    fun createLobbyAllowsRetryAfterConfirmationTimeout() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        fakeClient.mutableConnectionStatus.value = ConnectionStatus.CONNECTED

        viewModel.createLobby()
        assertEquals(1, fakeClient.sendCreateLobbyCallCount)

        viewModel.createLobby()
        assertEquals(1, fakeClient.sendCreateLobbyCallCount)

        advanceTimeBy(11_000L)
        advanceUntilIdle()

        viewModel.createLobby()
        assertEquals(2, fakeClient.sendCreateLobbyCallCount)
    }

    @Test
    fun joinLobbySendsJoinRequestWhenWebSocketIsConnected() {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        fakeClient.mutableConnectionStatus.value = ConnectionStatus.CONNECTED
        viewModel.onJoinLobbyCodeChange("abc123")

        viewModel.joinLobby()

        assertTrue(fakeClient.sendJoinLobbyCalled)
        assertEquals("ABC123", fakeClient.joinedLobbyCode)
    }

    @Test
    fun joinLobbyIgnoresRapidDuplicateClicksWhenWebSocketIsConnected() {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        fakeClient.mutableConnectionStatus.value = ConnectionStatus.CONNECTED
        viewModel.onJoinLobbyCodeChange("abc123")

        viewModel.joinLobby()
        viewModel.joinLobby()

        assertEquals(1, fakeClient.sendJoinLobbyCallCount)
    }

    @Test
    fun joinLobbyAllowsRetryAfterConfirmationTimeout() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        fakeClient.mutableConnectionStatus.value = ConnectionStatus.CONNECTED
        viewModel.onJoinLobbyCodeChange("abc123")

        viewModel.joinLobby()
        assertEquals(1, fakeClient.sendJoinLobbyCallCount)

        viewModel.joinLobby()
        assertEquals(1, fakeClient.sendJoinLobbyCallCount)

        advanceTimeBy(11_000L)
        advanceUntilIdle()

        viewModel.joinLobby()
        assertEquals(2, fakeClient.sendJoinLobbyCallCount)
    }

    @Test
    fun joinLobbyAllowsRetryAfterServerRejectsJoin() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = HomeViewModel(fakeClient)

        fakeClient.mutableConnectionStatus.value = ConnectionStatus.CONNECTED
        viewModel.onJoinLobbyCodeChange("abc123")

        viewModel.joinLobby()
        assertEquals(1, fakeClient.sendJoinLobbyCallCount)

        viewModel.setJoinLobbyError("Lobby code is invalid")
        viewModel.joinLobby()

        assertEquals(2, fakeClient.sendJoinLobbyCallCount)
    }

    @Test
    fun setJoinLobbyErrorRaisesErrorFlag() {
        val viewModel = HomeViewModel(FakeWebSocketClient())
        assertFalse(viewModel.joinLobbyError.value)

        viewModel.setJoinLobbyError("Lobby code is invalid")

        assertTrue(viewModel.joinLobbyError.value)
    }

    @Test
    fun changingJoinLobbyCodeClearsErrorFlag() {
        val viewModel = HomeViewModel(FakeWebSocketClient())
        viewModel.setJoinLobbyError("Lobby code is invalid")
        assertTrue(viewModel.joinLobbyError.value)

        viewModel.onJoinLobbyCodeChange("XYZ789")

        assertFalse(viewModel.joinLobbyError.value)
        assertEquals("XYZ789", viewModel.joinLobbyCode.value)
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
        override val diceRollTick: StateFlow<Long> = MutableStateFlow(0L)
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
        override val winnerId: StateFlow<Int?> =
            MutableStateFlow(null)
        override val gameStatus: StateFlow<GameStatus?> =
            MutableStateFlow(null)
        override val roundNumber: StateFlow<Int?> =
            MutableStateFlow(null)
        override val playerCards: StateFlow<Map<Int, List<PlayerCardState>>> =
            MutableStateFlow(emptyMap())
        override val playerLandmarks: StateFlow<Map<Int, List<PlayerLandmarkState>>> =
            MutableStateFlow(emptyMap())
        override val marketplace: StateFlow<Map<CardType, Int>> =
            MutableStateFlow(emptyMap())
        override val shopItems: StateFlow<List<ShopItem>> =
            MutableStateFlow(emptyList())
        override val purchaseEvents: SharedFlow<PurchaseEvent> =
            MutableSharedFlow(extraBufferCapacity = 1)

        override val authRejections: SharedFlow<Unit> = MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        override val lobbyJoinErrors: SharedFlow<ClientError.WebSocket> = MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        override val hostLeftLobby: SharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        override val chatMessages: SharedFlow<ChatMessageState> = MutableSharedFlow(extraBufferCapacity = 1)
        var connectCalled = false
        var disconnectCalled = false
        var sendGameStartCalled = false
        var sendCreateLobbyCalled = false
        var sendCreateLobbyCallCount = 0
        var sendJoinLobbyCalled = false
        var sendJoinLobbyCallCount = 0
        var joinedLobbyCode: String? = null

        override fun sendJoinLobby(lobbyCode: String) {
            sendJoinLobbyCalled = true
            sendJoinLobbyCallCount++
            joinedLobbyCode = lobbyCode
        }

        val mutableLobbyEntered = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        override val lobbyEntered: SharedFlow<Unit> = mutableLobbyEntered
        override val accusationResults: SharedFlow<com.machikoro.client.domain.model.state.AccusationResult> =
            MutableSharedFlow(extraBufferCapacity = 1)
        override val accusationErrors: SharedFlow<String> = MutableSharedFlow(extraBufferCapacity = 1)
        override fun connect() { connectCalled = true }
        override fun disconnect() { disconnectCalled = true }
        override fun rollDice(diceCount: Int) = Unit
        override fun rerollDice(diceCount: Int) = Unit
        override fun advancePhase(gameId: Int) = Unit
        override fun resolveEffects(gameId: Int) = Unit
        override fun endTurn(gameId: Int) = Unit
        override fun reportCheat(gameId: Int) = Unit
        override fun accuse(gameId: Int, accusedPlayerId: Int) = Unit
        override fun sendGameStart() { sendGameStartCalled = true }
        override fun sendCreateLobby() {
            sendCreateLobbyCalled = true
            sendCreateLobbyCallCount++
        }
        override fun sendPurchase(
            gameId: Int,
            purchaseType: PurchaseType,
            cardType: String?,
            landmarkType: String?
        ) = Unit
        override fun clearLobbyCode() { mutableLobbyCode.value = null }
        override fun clearGameState() = Unit
        override fun sendLeaveLobby(gameId: Int) {}
        override fun sendReadyToggle(isReady: Boolean) = Unit
        override fun sendChatMessage(gameId: Int, message: String) {}
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
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
