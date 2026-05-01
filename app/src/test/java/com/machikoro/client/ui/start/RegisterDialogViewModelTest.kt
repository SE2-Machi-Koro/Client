package com.machikoro.client.ui.start

import com.machikoro.client.network.auth.AuthApi
import com.machikoro.client.network.auth.RegisterRequest
import com.machikoro.client.network.auth.RegisterResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class RegisterDialogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun submitSuccessSetsRegisteredUsernameAndClearsSubmitting() = runTest {
        val api = FakeAuthApi(
            response = { request -> RegisterResponse(id = 7, username = request.username) },
        )
        val viewModel = RegisterDialogViewModel(api)
        viewModel.usernameChanged("alice")
        viewModel.passwordChanged("hunter2")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("alice", state.registeredUsername)
        assertFalse(state.submitting)
        assertNull(state.errorMessage)
    }

    @Test
    fun submitHttpExceptionSurfacesServerErrorBody() = runTest {
        val errorBody = "Username 'alice' is already taken".toResponseBody("text/plain".toMediaType())
        val api = FakeAuthApi(
            response = { throw HttpException(Response.error<RegisterResponse>(400, errorBody)) },
        )
        val viewModel = RegisterDialogViewModel(api)
        viewModel.usernameChanged("alice")
        viewModel.passwordChanged("hunter2")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Username 'alice' is already taken", state.errorMessage)
        assertNull(state.registeredUsername)
        assertFalse(state.submitting)
    }

    @Test
    fun submitIoExceptionSurfacesNetworkErrorMessage() = runTest {
        val api = FakeAuthApi(
            response = { throw IOException("connect timed out") },
        )
        val viewModel = RegisterDialogViewModel(api)
        viewModel.usernameChanged("alice")
        viewModel.passwordChanged("hunter2")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Network error: connect timed out", state.errorMessage)
        assertFalse(state.submitting)
    }

    @Test
    fun usernameAndPasswordChangedUpdateState() = runTest {
        val viewModel = RegisterDialogViewModel(FakeAuthApi())

        viewModel.usernameChanged("alice")
        viewModel.passwordChanged("hunter2")

        assertEquals("alice", viewModel.state.value.username)
        assertEquals("hunter2", viewModel.state.value.password)
    }

    @Test
    fun resetClearsTheForm() = runTest {
        val viewModel = RegisterDialogViewModel(FakeAuthApi())
        viewModel.usernameChanged("alice")
        viewModel.passwordChanged("hunter2")

        viewModel.reset()

        val state = viewModel.state.value
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertNull(state.registeredUsername)
        assertNull(state.errorMessage)
    }

    @Test
    fun canSubmitIsFalseWhenFieldsBlankOrAlreadyRegisteredOrSubmitting() = runTest {
        val viewModel = RegisterDialogViewModel(FakeAuthApi())

        // blank fields
        assertFalse(viewModel.state.value.canSubmit)

        // username only
        viewModel.usernameChanged("alice")
        assertFalse(viewModel.state.value.canSubmit)

        // both filled
        viewModel.passwordChanged("hunter2")
        assertTrue(viewModel.state.value.canSubmit)
    }

    private class FakeAuthApi(
        private val response: (RegisterRequest) -> RegisterResponse = { _ ->
            RegisterResponse(id = 1, username = "stub")
        },
    ) : AuthApi {
        override suspend fun register(body: RegisterRequest): RegisterResponse = response(body)
    }
}
