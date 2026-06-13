package com.machikoro.client.network

sealed class ClientError(override val message: String) : Exception(message) {
    data class Http(
        val statusCode: Int,
        val serverErrorCode: String?,
        override val message: String,
    ) : ClientError(message)

    data class Network(override val message: String) : ClientError(message)

    data class Unknown(override val message: String) : ClientError(message)
}
