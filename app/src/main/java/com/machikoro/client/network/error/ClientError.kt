package com.machikoro.client.network.error

sealed interface ClientError {
    val userMessage: String
    val diagnosticMessage: String

    data class Api(
        val statusCode: Int,
        val serverCode: String,
        override val userMessage: String,
        val timestampMillis: Long = UNKNOWN_TIMESTAMP,
    ) : ClientError {
        override val diagnosticMessage: String
            get() = "HTTP $statusCode [$serverCode]: $userMessage"
    }

    data class Validation(
        val statusCode: Int,
        val serverCode: String,
        override val userMessage: String,
        val timestampMillis: Long = UNKNOWN_TIMESTAMP,
    ) : ClientError {
        override val diagnosticMessage: String
            get() = "HTTP $statusCode [$serverCode]: $userMessage"
    }

    data class WebSocket(
        val serverCode: String,
        override val userMessage: String,
        val timestampMillis: Long = UNKNOWN_TIMESTAMP,
        val context: Map<String, String> = emptyMap(),
    ) : ClientError {
        override val diagnosticMessage: String
            get() = "WebSocket [$serverCode]: $userMessage"
    }

    data class Network(
        val causeDescription: String,
        override val userMessage: String = NETWORK_USER_MESSAGE,
    ) : ClientError {
        override val diagnosticMessage: String
            get() = "Network failure: $causeDescription"
    }

    data class Unknown(
        val causeDescription: String,
        override val userMessage: String = UNKNOWN_USER_MESSAGE,
    ) : ClientError {
        override val diagnosticMessage: String
            get() = "Unexpected failure: $causeDescription"
    }

    companion object {
        const val UNKNOWN_TIMESTAMP = -1L
        const val NETWORK_USER_MESSAGE = "Network error. Check your connection and try again."
        const val UNKNOWN_USER_MESSAGE = "Something went wrong. Please try again."
    }
}
