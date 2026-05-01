package com.machikoro.client.domain.session

data class Session(
    val sessionToken: String,
    val username: String,
)
