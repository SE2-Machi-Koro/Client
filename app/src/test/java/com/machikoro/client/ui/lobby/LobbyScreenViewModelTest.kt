package com.machikoro.client.ui.lobby

import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.LobbyStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.session.Session
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.debug.DebugApi
import com.machikoro.client.network.debug.EndGameRequest
import com.machikoro.client.network.debug.FillLobbyRequest
import com.machikoro.client.network.debug.ResetLobbyRequest
import com.machikoro.client.network.websocket.FakeWebSocketClient
import com.machikoro.client.ui.start.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyScreenViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateUsesPlaceholderValues() = runTest {
        val viewModel = LobbyScreenViewModel(FakeWebSocketClient(), FakeSessionStateHolder(), FakeDebugApi())
        advanceUntilIdle()
        assertEquals(ConnectionStatus.IDLE, viewModel.state.value.connectionStatus)
        assertEquals(LobbyStatus.WAITING_FOR_PLAYERS, viewModel.state.value.lobbyStatus)
        assertEquals(emptyList<PlayerCoinState>(), viewModel.state.value.playerList)
        assertEquals(4, viewModel.state.value.maxPlayers)
        assertFalse(viewModel.state.value.isHost)
        assertNull(viewModel.state.value.loggedInAs)
        assertFalse(viewModel.state.value.isReady)
    }

    @Test
    fun playersFlowUpdatesPlayerList() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), FakeDebugApi())
        fakeClient.emitPlayers(listOf(
            PlayerCoinState(id = "1", displayName = "alice", coins = 3),
            PlayerCoinState(id = "2", displayName = "bob", coins = 5),
        ))
        advanceUntilIdle()
        assertEquals(
            listOf(
                PlayerCoinState(id = "1", displayName = "alice", coins = 3),
                PlayerCoinState(id = "2", displayName = "bob", coins = 5),
            ),
            viewModel.state.value.playerList
        )
        assertEquals(LobbyStatus.READY, viewModel.state.value.lobbyStatus)
    }

    @Test
    fun lobbyStatusIsWaitingWhenLessThanTwoPlayers() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), FakeDebugApi())
        fakeClient.emitPlayers(listOf(PlayerCoinState(id = "1", displayName = "alice", coins = 3)))
        advanceUntilIdle()
        assertEquals(listOf(PlayerCoinState(id = "1", displayName = "alice", coins = 3)), viewModel.state.value.playerList)
        assertEquals(LobbyStatus.WAITING_FOR_PLAYERS, viewModel.state.value.lobbyStatus)
    }

    @Test
    fun clientStatusUpdatesAreReflectedInLobbyState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), FakeDebugApi())
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
        val viewModel = LobbyScreenViewModel(FakeWebSocketClient(), sessionHolder, FakeDebugApi())
        sessionHolder.signIn(token = "uuid-123", username = "alice", userId = 1)
        advanceUntilIdle()
        assertEquals("alice", viewModel.state.value.loggedInAs)
        sessionHolder.signOut()
        advanceUntilIdle()
        assertNull(viewModel.state.value.loggedInAs)
    }

    @Test
    fun onReadyToggleTogglesReadyState() = runTest {
        val viewModel = LobbyScreenViewModel(FakeWebSocketClient(), FakeSessionStateHolder(), FakeDebugApi())
        assertFalse(viewModel.state.value.isReady)
        viewModel.onReadyToggle()
        assertTrue(viewModel.state.value.isReady)
        viewModel.onReadyToggle()
        assertFalse(viewModel.state.value.isReady)
    }

    @Test
    fun onStartGameDelegatesToWebSocketClientWhenHostAndEnoughPlayers() = runTest {
        val fakeClient = FakeWebSocketClient()
        val sessionHolder = FakeSessionStateHolder()
        val viewModel = LobbyScreenViewModel(fakeClient, sessionHolder, FakeDebugApi())
        sessionHolder.signIn(token = "uuid-123", username = "alice", userId = 1)
        fakeClient.emitIsLobbyHost(true)
        fakeClient.emitPlayers(listOf(
            PlayerCoinState(id = "1", displayName = "alice", coins = 3),
            PlayerCoinState(id = "2", displayName = "bob", coins = 5),
        ))
        advanceUntilIdle()
        viewModel.onStartGame()
        assertTrue(fakeClient.gameStartSent)
    }

    @Test
    fun onStartGameDoesNotSendWhenUserIsNotHost() = runTest {
        val fakeClient = FakeWebSocketClient()
        val sessionHolder = FakeSessionStateHolder()
        val viewModel = LobbyScreenViewModel(fakeClient, sessionHolder, FakeDebugApi())
        sessionHolder.signIn(token = "uuid-123", username = "bob", userId = 2)
        fakeClient.emitIsLobbyHost(false)
        fakeClient.emitPlayers(listOf(
            PlayerCoinState(id = "1", displayName = "alice", coins = 3),
            PlayerCoinState(id = "2", displayName = "bob", coins = 5),
        ))
        advanceUntilIdle()
        viewModel.onStartGame()
        assertFalse(fakeClient.gameStartSent)
    }

    @Test
    fun onStartGameWithHostAndEnoughPlayersAndActiveGameIdSendsGameStart() = runTest {
        val fakeClient = FakeWebSocketClient()
        val sessionHolder = FakeSessionStateHolder()
        val viewModel = LobbyScreenViewModel(fakeClient, sessionHolder, FakeDebugApi())
        sessionHolder.signIn(token = "uuid-123", username = "alice", userId = 1)
        fakeClient.emitIsLobbyHost(true)
        fakeClient.emitActiveGameId(42)
        fakeClient.emitPlayers(listOf(
            PlayerCoinState(id = "1", displayName = "alice", coins = 3),
            PlayerCoinState(id = "2", displayName = "bob", coins = 5),
        ))
        advanceUntilIdle()
        viewModel.onStartGame()
        assertTrue(fakeClient.gameStartSent)
    }

    @Test
    fun fillWithDummies_callsApiWithCurrentLobbyCode() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), fakeDebugApi)
        fakeClient.emitLobbyCode("ABC123")
        advanceUntilIdle()
        viewModel.fillWithDummies()
        advanceUntilIdle()
        assertEquals(1, fakeDebugApi.fillLobbyCallCount)
        assertEquals("ABC123", fakeDebugApi.lastFillRequest?.lobbyCode)
    }

    @Test
    fun fillWithDummies_doesNothingWhenNoLobbyCode() = runTest {
        val fakeDebugApi = FakeDebugApi()
        val viewModel = LobbyScreenViewModel(FakeWebSocketClient(), FakeSessionStateHolder(), fakeDebugApi)
        advanceUntilIdle()
        viewModel.fillWithDummies()
        advanceUntilIdle()
        assertEquals(0, fakeDebugApi.fillLobbyCallCount)
    }

    @Test
    fun fillWithDummies_handlesApiErrorGracefully() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi(shouldThrow = true)
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), fakeDebugApi)
        fakeClient.emitLobbyCode("ABC123")
        advanceUntilIdle()
        // Should not throw even when the API call fails
        viewModel.fillWithDummies()
        advanceUntilIdle()
        assertEquals(1, fakeDebugApi.fillLobbyCallCount)
    }

    @Test
    fun fillWithDummies_usesLatestLobbyCodeAfterCodeChanges() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), fakeDebugApi)
        fakeClient.emitLobbyCode("FIRST")
        advanceUntilIdle()
        fakeClient.emitLobbyCode("SECOND")
        advanceUntilIdle()
        viewModel.fillWithDummies()
        advanceUntilIdle()
        assertEquals("SECOND", fakeDebugApi.lastFillRequest?.lobbyCode)
    }

    @Test
    fun fillWithDummies_passesCountToApi() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), fakeDebugApi)
        fakeClient.emitLobbyCode("ABC123")
        advanceUntilIdle()
        viewModel.fillWithDummies(count = 2)
        advanceUntilIdle()
        assertEquals(2, fakeDebugApi.lastFillRequest?.count)
    }

    @Test
    fun fillWithDummies_sendsNullCountWhenNotSpecified() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), fakeDebugApi)
        fakeClient.emitLobbyCode("ABC123")
        advanceUntilIdle()
        viewModel.fillWithDummies()
        advanceUntilIdle()
        assertEquals(null, fakeDebugApi.lastFillRequest?.count)
    }

    @Test
    fun resetLobby_callsApiWithCurrentLobbyCode() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), fakeDebugApi)
        fakeClient.emitLobbyCode("ABC123")
        advanceUntilIdle()
        viewModel.resetLobby()
        advanceUntilIdle()
        assertEquals(1, fakeDebugApi.resetLobbyCallCount)
        assertEquals("ABC123", fakeDebugApi.lastResetRequest?.lobbyCode)
    }

    @Test
    fun resetLobby_doesNothingWhenNoLobbyCode() = runTest {
        val fakeDebugApi = FakeDebugApi()
        val viewModel = LobbyScreenViewModel(FakeWebSocketClient(), FakeSessionStateHolder(), fakeDebugApi)
        advanceUntilIdle()
        viewModel.resetLobby()
        advanceUntilIdle()
        assertEquals(0, fakeDebugApi.resetLobbyCallCount)
    }

    @Test
    fun isReadyResetsToFalseWhenNewLobbyCodeArrives() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), FakeDebugApi())
        viewModel.onReadyToggle()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isReady)
        fakeClient.emitLobbyCode("NEW123")
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isReady)
    }

    @Test
    fun resetLobby_handlesApiErrorGracefully() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi(shouldThrow = true)
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), fakeDebugApi)
        fakeClient.emitLobbyCode("ABC123")
        advanceUntilIdle()
        viewModel.resetLobby()
        advanceUntilIdle()
        assertEquals(1, fakeDebugApi.resetLobbyCallCount)
    }

    @Test
    fun fillWithDummies_survivesNetworkFailureWithoutAlteringLobbyState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi(error = IOException("connect timed out"))
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), fakeDebugApi)
        fakeClient.emitLobbyCode("ABC123")
        advanceUntilIdle()
        val stateBefore = viewModel.state.value

        viewModel.fillWithDummies()
        advanceUntilIdle()

        assertEquals(1, fakeDebugApi.fillLobbyCallCount)
        assertEquals(stateBefore, viewModel.state.value)
    }

    @Test
    fun resetLobby_survivesHttpErrorWithJsonBodyWithoutAlteringLobbyState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val errorBody = """{"errorCode":"GAME_NOT_FOUND","message":"Lobby does not exist"}"""
            .toResponseBody("application/json".toMediaType())
        val fakeDebugApi = FakeDebugApi(error = HttpException(Response.error<Unit>(404, errorBody)))
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), fakeDebugApi)
        fakeClient.emitLobbyCode("ABC123")
        advanceUntilIdle()
        val stateBefore = viewModel.state.value

        viewModel.resetLobby()
        advanceUntilIdle()

        assertEquals(1, fakeDebugApi.resetLobbyCallCount)
        assertEquals(stateBefore, viewModel.state.value)
    }

    // === ready-sync tests ===

    @Test
    fun isReadySyncedFromServerRosterWhenPlayersFlowUpdates() = runTest {
        val fakeClient = FakeWebSocketClient()
        val sessionHolder = FakeSessionStateHolder()
        val viewModel = LobbyScreenViewModel(fakeClient, sessionHolder, FakeDebugApi())
        sessionHolder.signIn(token = "t", username = "alice", userId = 1)
        advanceUntilIdle()
        // Server roster says alice is ready
        fakeClient.emitPlayers(listOf(PlayerCoinState(id = "1", displayName = "alice", coins = 3, isReady = true)))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isReady)
    }

    @Test
    fun isReadyNotOverriddenWhenCurrentUserNotInRoster() = runTest {
        val fakeClient = FakeWebSocketClient()
        val sessionHolder = FakeSessionStateHolder()
        val viewModel = LobbyScreenViewModel(fakeClient, sessionHolder, FakeDebugApi())
        sessionHolder.signIn(token = "t", username = "alice", userId = 1)
        advanceUntilIdle()
        // Alice toggled ready locally
        viewModel.onReadyToggle()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isReady)
        // Roster update that does not include alice — local state preserved
        fakeClient.emitPlayers(listOf(PlayerCoinState(id = "2", displayName = "bob", coins = 3, isReady = false)))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isReady)
    }

    @Test
    fun onReadyToggleSendsToggleViaWebSocketClient() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), FakeDebugApi())
        viewModel.onReadyToggle()
        assertEquals(true, fakeClient.lastReadyToggle)
    }

    @Test
    fun onReadyToggleSendsCorrectValueOnSubsequentToggles() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = LobbyScreenViewModel(fakeClient, FakeSessionStateHolder(), FakeDebugApi())
        viewModel.onReadyToggle()
        assertEquals(true, fakeClient.lastReadyToggle)
        viewModel.onReadyToggle()
        assertEquals(false, fakeClient.lastReadyToggle)
    }

    private class FakeDebugApi(
        private val shouldThrow: Boolean = false,
        private val error: Throwable? = null,
    ) : DebugApi {
        var fillLobbyCallCount = 0
            private set
        var lastFillRequest: FillLobbyRequest? = null
            private set
        var resetLobbyCallCount = 0
            private set
        var lastResetRequest: ResetLobbyRequest? = null
            private set

        override suspend fun fillLobby(body: FillLobbyRequest): Response<Unit> {
            fillLobbyCallCount++
            lastFillRequest = body
            error?.let { throw it }
            if (shouldThrow) throw RuntimeException("Simulated network error")
            return Response.success(Unit)
        }

        override suspend fun resetLobby(body: ResetLobbyRequest): Response<Unit> {
            resetLobbyCallCount++
            lastResetRequest = body
            error?.let { throw it }
            if (shouldThrow) throw RuntimeException("Simulated network error")
            return Response.success(Unit)
        }

        override suspend fun purge(): Response<Unit> = Response.success(Unit)

        override suspend fun endGame(body: EndGameRequest): Response<Unit> = Response.success(Unit)
    }

    private class FakeSessionStateHolder : SessionStateHolder {
        private val mutableSession = MutableStateFlow<Session?>(null)
        override val session: StateFlow<Session?> = mutableSession.asStateFlow()

        override fun signIn(token: String, username: String, userId: Int) {
            mutableSession.value = Session(token, username, userId)
        }

        override fun signOut() {
            mutableSession.value = null
        }
    }
}