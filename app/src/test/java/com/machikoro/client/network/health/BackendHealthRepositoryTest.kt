package com.machikoro.client.network.health

import com.machikoro.client.domain.model.state.BackendHealth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BackendHealthRepositoryTest {

    @Test
    fun `observe emits UP on 2xx with status UP body`() = runTest {
        val api: HealthApi = mock()
        whenever(api.checkHealth()).thenReturn(Response.success(HealthResponse("UP")))
        val repo = BackendHealthRepository(api, pollIntervalMs = 100L)

        val result = repo.observe().first()

        assertEquals(BackendHealth.UP, result)
    }

    @Test
    fun `observe emits DOWN on non-2xx response`() = runTest {
        val api: HealthApi = mock()
        whenever(api.checkHealth()).thenReturn(
            Response.error(503, "".toResponseBody("application/json".toMediaType()))
        )
        val repo = BackendHealthRepository(api, pollIntervalMs = 100L)

        val result = repo.observe().first()

        assertEquals(BackendHealth.DOWN, result)
    }

    @Test
    fun `observe emits DOWN when body status is not UP`() = runTest {
        val api: HealthApi = mock()
        whenever(api.checkHealth()).thenReturn(Response.success(HealthResponse("DOWN")))
        val repo = BackendHealthRepository(api, pollIntervalMs = 100L)

        val result = repo.observe().first()

        assertEquals(BackendHealth.DOWN, result)
    }

    @Test
    fun `observe emits DOWN when api throws IOException`() = runTest {
        val api: HealthApi = mock()
        whenever(api.checkHealth()).thenAnswer { throw IOException("timeout") }
        val repo = BackendHealthRepository(api, pollIntervalMs = 100L)

        val result = repo.observe().first()

        assertEquals(BackendHealth.DOWN, result)
    }

    @Test
    fun `observe emits at poll interval cadence`() = runTest {
        val api: HealthApi = mock()
        whenever(api.checkHealth()).thenReturn(Response.success(HealthResponse("UP")))
        val repo = BackendHealthRepository(api, pollIntervalMs = 1_000L)

        val emissions = repo.observe().take(3).toList()

        assertEquals(listOf(BackendHealth.UP, BackendHealth.UP, BackendHealth.UP), emissions)
        verify(api, times(3)).checkHealth()
    }
}
