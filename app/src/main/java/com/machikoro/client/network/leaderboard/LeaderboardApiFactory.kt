package com.machikoro.client.network.leaderboard

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object LeaderboardApiFactory {
    private val json = Json { ignoreUnknownKeys = true }

    // Exposed for testing — adds Bearer header when token is present
    internal fun withAuth(request: Request, token: String?): Request =
        if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else request

    // Attach Bearer token so the server's TokenAuthFilter can authenticate the request
    fun create(baseUrl: String, tokenProvider: () -> String?): LeaderboardApi {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(withAuth(chain.request(), tokenProvider()))
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LeaderboardApi::class.java)
    }
}