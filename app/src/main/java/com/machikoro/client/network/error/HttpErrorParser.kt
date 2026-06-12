package com.machikoro.client.network.error

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response

object HttpErrorParser {
    private const val VALIDATION_FAILED = "VALIDATION_FAILED"

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun fromThrowable(
        throwable: Throwable,
        fallbackMessage: String = ClientError.UNKNOWN_USER_MESSAGE,
    ): ClientError {
        if (throwable is CancellationException) throw throwable

        return when (throwable) {
            is HttpException -> fromHttpException(throwable, fallbackMessage)
            is IOException -> ClientError.Network(causeDescription = throwable.safeDescription())
            else -> ClientError.Unknown(
                causeDescription = throwable.safeDescription(),
                userMessage = fallbackMessage,
            )
        }
    }

    fun fromResponse(
        response: Response<*>,
        fallbackMessage: String = ClientError.UNKNOWN_USER_MESSAGE,
    ): ClientError {
        require(!response.isSuccessful) {
            "Only unsuccessful HTTP responses can be converted to ClientError."
        }
        return parseHttpFailure(
            statusCode = response.code(),
            errorBody = response.errorBody(),
            fallbackMessage = fallbackMessage,
        )
    }

    private fun fromHttpException(
        exception: HttpException,
        fallbackMessage: String,
    ): ClientError =
        parseHttpFailure(
            statusCode = exception.code(),
            errorBody = exception.response()?.errorBody(),
            fallbackMessage = fallbackMessage,
        )

    private fun parseHttpFailure(
        statusCode: Int,
        errorBody: ResponseBody?,
        fallbackMessage: String,
    ): ClientError =
        when (val parsedBody = parseBody(readBody(errorBody))) {
            ParsedErrorBody.Empty -> apiError(
                statusCode = statusCode,
                serverCode = "HTTP_$statusCode",
                message = fallbackMessage,
                timestampMillis = ClientError.UNKNOWN_TIMESTAMP,
            )
            is ParsedErrorBody.PlainText -> apiError(
                statusCode = statusCode,
                serverCode = "HTTP_$statusCode",
                message = parsedBody.message,
                timestampMillis = ClientError.UNKNOWN_TIMESTAMP,
            )
            is ParsedErrorBody.Server -> apiError(
                statusCode = statusCode,
                serverCode = parsedBody.code.ifBlank { "HTTP_$statusCode" },
                message = parsedBody.message.ifBlank { fallbackMessage },
                timestampMillis = parsedBody.timestampMillis,
            )
        }

    private fun apiError(
        statusCode: Int,
        serverCode: String,
        message: String,
        timestampMillis: Long,
    ): ClientError =
        if (serverCode == VALIDATION_FAILED) {
            ClientError.Validation(
                statusCode = statusCode,
                serverCode = serverCode,
                userMessage = message,
                timestampMillis = timestampMillis,
            )
        } else {
            ClientError.Api(
                statusCode = statusCode,
                serverCode = serverCode,
                userMessage = message,
                timestampMillis = timestampMillis,
            )
        }

    private fun parseBody(rawBody: String): ParsedErrorBody {
        if (rawBody.isBlank()) return ParsedErrorBody.Empty

        val decoded = decodeServerBody(rawBody)
        if (decoded !is ParsedErrorBody.Empty) return decoded

        return if (rawBody.looksLikeJson()) {
            ParsedErrorBody.Empty
        } else {
            ParsedErrorBody.PlainText(rawBody)
        }
    }

    private fun decodeServerBody(rawBody: String): ParsedErrorBody =
        try {
            val body = json.decodeFromString(ServerErrorBody.serializer(), rawBody)
            val serverCode = body.code.ifBlank { body.errorCode }
            if (serverCode.isBlank() && body.message.isBlank()) {
                ParsedErrorBody.Empty
            } else {
                ParsedErrorBody.Server(
                    code = serverCode,
                    message = body.message,
                    timestampMillis = body.timestamp,
                )
            }
        } catch (_: SerializationException) {
            ParsedErrorBody.Empty
        }

    private fun readBody(errorBody: ResponseBody?): String {
        if (errorBody == null) return ""

        return try {
            errorBody.string().trim()
        } catch (_: IOException) {
            ""
        }
    }

    private fun String.looksLikeJson(): Boolean =
        startsWith("{") || startsWith("[")

    private fun Throwable.safeDescription(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName

    @Serializable
    private data class ServerErrorBody(
        val code: String = "",
        val errorCode: String = "",
        val message: String = "",
        val timestamp: Long = ClientError.UNKNOWN_TIMESTAMP,
    )

    private sealed interface ParsedErrorBody {
        data object Empty : ParsedErrorBody
        data class PlainText(val message: String) : ParsedErrorBody
        data class Server(
            val code: String,
            val message: String,
            val timestampMillis: Long,
        ) : ParsedErrorBody
    }
}
