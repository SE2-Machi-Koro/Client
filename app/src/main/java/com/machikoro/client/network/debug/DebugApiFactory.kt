package com.machikoro.client.network.debug

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object DebugApiFactory {
    private val json = Json { ignoreUnknownKeys = true }

    // Exposed for testing — adds Bearer header when token is present
    internal fun withAuth(request: Request, token: String?): Request =
        if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else request

    // Attach Bearer token so admin-protected debug endpoints are reachable
    fun create(baseUrl: String, tokenProvider: () -> String?): DebugApi {
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
            .create(DebugApi::class.java)
    }
}