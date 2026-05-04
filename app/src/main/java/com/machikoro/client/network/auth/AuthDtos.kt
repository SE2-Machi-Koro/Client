package com.machikoro.client.network.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
)

@Serializable
data class RegisterResponse(
    val id: Int,
    val username: String,
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val sessionToken: String,
    val username: String,
)

@Serializable
data class LogoutRequest(
    val sessionToken: String,
)
