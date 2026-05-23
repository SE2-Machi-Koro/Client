package com.machikoro.client.network.health

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET

interface HealthApi {
    @GET("/actuator/health")
    suspend fun checkHealth(): Response<HealthResponse>
}

@Serializable
data class HealthResponse(
    val status: String? = null,
)
