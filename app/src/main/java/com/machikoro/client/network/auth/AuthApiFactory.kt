package com.machikoro.client.network.auth

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object AuthApiFactory {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun create(baseUrl: String): AuthApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
}
