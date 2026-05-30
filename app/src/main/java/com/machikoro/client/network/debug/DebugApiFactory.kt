package com.machikoro.client.network.debug

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object DebugApiFactory {
    private val json = Json { ignoreUnknownKeys = true }

    // Attach Bearer token so admin-protected debug endpoints are reachable
    fun create(baseUrl: String, tokenProvider: () -> String?): DebugApi {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = tokenProvider()?.let { token ->
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } ?: chain.request()
                chain.proceed(req)
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