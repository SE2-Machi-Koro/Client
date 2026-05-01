package com.machikoro.client.ui.start

import com.machikoro.client.domain.session.Session
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.auth.AuthApi
import com.machikoro.client.network.auth.LoginRequest
import com.machikoro.client.network.auth.LoginResponse
import com.machikoro.client.network.auth.LogoutRequest
import com.machikoro.client.network.auth.RegisterRequest
import com.machikoro.client.network.auth.RegisterResponse
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
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LogoutViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun submitCallsApiAndClearsSession() = runTest {
        val sessionHolder = FakeSessionStateHolder().apply {
            signIn(token = "uuid-123", username = "alice")
        }
        val api = FakeAuthApi()
        val viewModel = LogoutViewModel(api, sessionHolder)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf(LogoutRequest("uuid-123")), api.logoutCalls)
        assertNull(sessionHolder.session.value)
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun submitIsNoOpWhenNoSession() = runTest {
        val sessionHolder = FakeSessionStateHolder()
        val api = FakeAuthApi()
        val viewModel = LogoutViewModel(api, sessionHolder)

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(api.logoutCalls.isEmpty())
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun submitClearsLocalSessionEvenWhenNetworkCallFails() = runTest {
        val sessionHolder = FakeSessionStateHolder().apply {
            signIn(token = "uuid-123", username = "alice")
        }
        val api = FakeAuthApi(logoutHandler = { throw IOException("connection refused") })
        val viewModel = LogoutViewModel(api, sessionHolder)

        viewModel.submit()
        advanceUntilIdle()

        // The API attempt is recorded — proves we tried — but the failure must
        // not strand the user. Local session is cleared regardless.
        assertEquals(listOf(LogoutRequest("uuid-123")), api.logoutCalls)
        assertNull(sessionHolder.session.value)
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun secondSubmitWhileInFlightIsIgnored() = runTest {
        val sessionHolder = FakeSessionStateHolder().apply {
            signIn(token = "uuid-123", username = "alice")
        }
        val api = FakeAuthApi()
        val viewModel = LogoutViewModel(api, sessionHolder)

        viewModel.submit()
        // Don't advance — the first submit hasn't completed.
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, api.logoutCalls.size)
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

    private class FakeAuthApi(
        private val logoutHandler: (LogoutRequest) -> Response<Unit> = { Response.success(Unit) },
    ) : AuthApi {
        val logoutCalls = mutableListOf<LogoutRequest>()

        override suspend fun register(body: RegisterRequest): RegisterResponse =
            RegisterResponse(id = 0, username = body.username)

        override suspend fun login(body: LoginRequest): LoginResponse =
            LoginResponse(sessionToken = "stub-token", username = body.username)

        override suspend fun logout(body: LogoutRequest): Response<Unit> {
            logoutCalls.add(body)
            return logoutHandler(body)
        }
    }
}
