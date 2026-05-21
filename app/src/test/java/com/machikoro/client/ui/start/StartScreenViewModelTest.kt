package com.machikoro.client.ui.start

import com.machikoro.client.domain.model.state.BackendHealth
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.session.Session
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.health.BackendHealthRepository
import com.machikoro.client.network.health.HealthApi
import com.machikoro.client.network.health.HealthResponse
import com.machikoro.client.network.websocket.FakeWebSocketClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class StartScreenViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateUsesPlaceholderValues() = runTest {
        val viewModel = StartScreenViewModel(
            FakeWebSocketClient(),
            FakeSessionStateHolder(),
            FakeBackendHealthRepository(),
        )
        advanceUntilIdle()
        assertEquals("Machi Koro Client", viewModel.state.value.title)
        assertEquals(ConnectionStatus.IDLE, viewModel.state.value.connectionStatus)
        assertNull(viewModel.state.value.loggedInAs)
        assertEquals(BackendHealth.UNKNOWN, viewModel.state.value.backendHealth)
    }

    @Test
    fun clientStatusUpdatesAreReflectedInScreenState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = StartScreenViewModel(
            fakeClient,
            FakeSessionStateHolder(),
            FakeBackendHealthRepository(),
        )
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
    fun sessionUpdatesAreReflectedInLoggedInAs() = runTest {
        val sessionHolder = FakeSessionStateHolder()
        val viewModel = StartScreenViewModel(
            FakeWebSocketClient(),
            sessionHolder,
            FakeBackendHealthRepository(),
        )
        sessionHolder.signIn(token = "uuid-123", username = "alice", userId = 1)
        advanceUntilIdle()
        assertEquals("alice", viewModel.state.value.loggedInAs)
        sessionHolder.signOut()
        advanceUntilIdle()
        assertNull(viewModel.state.value.loggedInAs)
    }

    @Test
    fun backendHealthUpdatesAreReflectedInScreenState() = runTest {
        val healthRepo = FakeBackendHealthRepository()
        val viewModel = StartScreenViewModel(
            FakeWebSocketClient(),
            FakeSessionStateHolder(),
            healthRepo,
        )
        advanceUntilIdle()
        assertEquals(BackendHealth.UNKNOWN, viewModel.state.value.backendHealth)

        healthRepo.emit(BackendHealth.UP)
        advanceUntilIdle()
        assertEquals(BackendHealth.UP, viewModel.state.value.backendHealth)

        healthRepo.emit(BackendHealth.DOWN)
        advanceUntilIdle()
        assertEquals(BackendHealth.DOWN, viewModel.state.value.backendHealth)
    }

    private class FakeSessionStateHolder : SessionStateHolder {
        private val mutableSession = MutableStateFlow<Session?>(null)
        override val session: StateFlow<Session?> = mutableSession.asStateFlow()
        override fun signIn(token: String, username: String, userId: Int) { // NEU
            mutableSession.value = Session(token, username, userId)
        }
        override fun signOut() { mutableSession.value = null }
    }

    private object NoOpHealthApi : HealthApi {
        override suspend fun checkHealth(): Response<HealthResponse> = throw NotImplementedError()
    }

    private class FakeBackendHealthRepository : BackendHealthRepository(
        api = NoOpHealthApi,
        pollIntervalMs = Long.MAX_VALUE,
    ) {
        private val flow = MutableStateFlow(BackendHealth.UNKNOWN)
        override fun observe(): Flow<BackendHealth> = flow
        fun emit(value: BackendHealth) { flow.value = value }
    }
}
