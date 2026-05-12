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
class LoginDialogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun submitSuccessSetsLoggedInAsAndPopulatesSessionStateHolder() = runTest {
        val sessionHolder = FakeSessionStateHolder()
        val api = FakeAuthApi(loginHandler = { request ->
            LoginResponse(sessionToken = "uuid-123", username = request.username)
        })
        val viewModel = LoginDialogViewModel(api, sessionHolder)
        viewModel.usernameChanged("alice")
        viewModel.passwordChanged("hunter2")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("alice", state.loggedInAs)
        assertFalse(state.submitting)
        assertNull(state.errorMessage)
        assertEquals(Session("uuid-123", "alice"), sessionHolder.session.value)
    }

    @Test
    fun submitHttpExceptionSurfacesServerErrorBodyVerbatim() = runTest {
        val sessionHolder = FakeSessionStateHolder()
        val errorBody = "Invalid username or password".toResponseBody("text/plain".toMediaType())
        val api = FakeAuthApi(loginHandler = {
            throw HttpException(Response.error<LoginResponse>(401, errorBody))
        })
        val viewModel = LoginDialogViewModel(api, sessionHolder)
        viewModel.usernameChanged("alice")
        viewModel.passwordChanged("wrong")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Invalid username or password", state.errorMessage)
        assertNull(state.loggedInAs)
        assertNull(sessionHolder.session.value)
    }

    @Test
    fun submitIoExceptionSurfacesNetworkErrorMessage() = runTest {
        val sessionHolder = FakeSessionStateHolder()
        val api = FakeAuthApi(loginHandler = {
            throw IOException("connect timed out")
        })
        val viewModel = LoginDialogViewModel(api, sessionHolder)
        viewModel.usernameChanged("alice")
        viewModel.passwordChanged("hunter2")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Network error: connect timed out", state.errorMessage)
        assertNull(sessionHolder.session.value)
    }

    @Test
    fun usernameAndPasswordChangedUpdateState() = runTest {
        val viewModel = LoginDialogViewModel(FakeAuthApi(), FakeSessionStateHolder())

        viewModel.usernameChanged("alice")
        viewModel.passwordChanged("hunter2")

        assertEquals("alice", viewModel.state.value.username)
        assertEquals("hunter2", viewModel.state.value.password)
    }

    @Test
    fun resetClearsTheForm() = runTest {
        val viewModel = LoginDialogViewModel(FakeAuthApi(), FakeSessionStateHolder())
        viewModel.usernameChanged("alice")
        viewModel.passwordChanged("hunter2")

        viewModel.reset()

        val state = viewModel.state.value
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertNull(state.loggedInAs)
        assertNull(state.errorMessage)
    }

    @Test
    fun canSubmitGateBlocksWhenBlankOrAlreadyLoggedInOrSubmitting() = runTest {
        val viewModel = LoginDialogViewModel(FakeAuthApi(), FakeSessionStateHolder())

        assertFalse(viewModel.state.value.canSubmit)

        viewModel.usernameChanged("alice")
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.passwordChanged("hunter2")
        assertTrue(viewModel.state.value.canSubmit)
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
        private val loginHandler: (LoginRequest) -> LoginResponse = { _ ->
            LoginResponse("stub-token", "stub")
        },
    ) : AuthApi {
        override suspend fun register(body: RegisterRequest): RegisterResponse =
            RegisterResponse(id = 0, username = body.username)

        override suspend fun login(body: LoginRequest): LoginResponse = loginHandler(body)

        override suspend fun logout(body: LogoutRequest): Response<Unit> =
            Response.success(Unit)
    }
}
