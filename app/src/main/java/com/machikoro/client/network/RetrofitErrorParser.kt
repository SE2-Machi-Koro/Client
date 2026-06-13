package com.machikoro.client.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

@Serializable
private data class ServerErrorBody(
    val errorCode: String? = null,
    val message: String? = null,
)

private val json = Json { ignoreUnknownKeys = true }

fun Throwable.toClientError(fallbackMessage: String = "An error occurred"): ClientError {
    if (this is CancellationException) throw this
    return when (this) {
        is HttpException -> parseHttpError(fallbackMessage)
        is IOException -> ClientError.Network(message ?: "Could not reach server")
        else -> ClientError.Unknown(message ?: fallbackMessage)
    }
}

private fun HttpException.parseHttpError(fallbackMessage: String): ClientError.Http {
    val rawBody = response()?.errorBody()?.string()
    if (rawBody.isNullOrBlank()) {
        return ClientError.Http(code(), null, "$fallbackMessage (HTTP ${code()})")
    }
    val parsed = tryParseJson(rawBody)
    return if (parsed != null) {
        ClientError.Http(
            statusCode = code(),
            serverErrorCode = parsed.errorCode,
            message = parsed.message ?: parsed.errorCode ?: "$fallbackMessage (HTTP ${code()})",
        )
    } else {
        ClientError.Http(
            statusCode = code(),
            serverErrorCode = null,
            message = rawBody,
        )
    }
}

private fun tryParseJson(raw: String): ServerErrorBody? = try {
    json.decodeFromString<ServerErrorBody>(raw)
} catch (_: Exception) {
    null
}
