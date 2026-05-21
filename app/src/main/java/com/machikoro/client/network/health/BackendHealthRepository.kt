package com.machikoro.client.network.health

import com.machikoro.client.domain.model.state.BackendHealth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

open class BackendHealthRepository(
    private val api: HealthApi,
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
) {
    open fun observe(): Flow<BackendHealth> = flow {
        while (currentCoroutineContext().isActive) {
            emit(probe())
            delay(pollIntervalMs)
        }
    }

    private suspend fun probe(): BackendHealth = try {
        val response = api.checkHealth()
        if (response.isSuccessful && response.body()?.status == "UP") {
            BackendHealth.UP
        } else {
            BackendHealth.DOWN
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        BackendHealth.DOWN
    }

    companion object {
        private const val DEFAULT_POLL_INTERVAL_MS = 5_000L
    }
}
