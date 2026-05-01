package com.machikoro.client.network.auth

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse
}
