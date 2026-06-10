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

fun ClientError.toUserMessage(): String = when (this) {
    is ClientError.Http -> message
    is ClientError.Network -> "Network error: $message"
    is ClientError.Unknown -> message
}
