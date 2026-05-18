package com.machikoro.client.network.debug

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object DebugApiFactory {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(baseUrl: String): DebugApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DebugApi::class.java)
}