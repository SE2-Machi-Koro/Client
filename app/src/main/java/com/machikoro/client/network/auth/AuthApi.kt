package com.machikoro.client.network.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    /**
     * Server returns 200 with empty body on success (and on unknown-token no-op).
     * Wrapped in Response<Unit> so we don't ask the JSON converter to parse "".
     */
    @POST("/auth/logout")
    suspend fun logout(@Body body: LogoutRequest): Response<Unit>
}
