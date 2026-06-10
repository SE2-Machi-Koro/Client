package com.machikoro.client.network

import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RetrofitErrorParserTest {

    @Test
    fun jsonErrorBodyPreservesStatusCodeErrorCodeAndMessage() {
        val body = """{"errorCode":"USER_NOT_FOUND","message":"No such user"}"""
            .toResponseBody("application/json".toMediaType())
        val error = HttpException(Response.error<Unit>(404, body))

        val result = error.toClientError("fallback") as ClientError.Http

        assertEquals(404, result.statusCode)
        assertEquals("USER_NOT_FOUND", result.serverErrorCode)
        assertEquals("No such user", result.message)
    }

    @Test
    fun jsonErrorBodyWithNoMessageFallsBackToErrorCode() {
        val body = """{"errorCode":"CONFLICT"}"""
            .toResponseBody("application/json".toMediaType())
        val error = HttpException(Response.error<Unit>(409, body))

        val result = error.toClientError("fallback") as ClientError.Http

        assertEquals("CONFLICT", result.message)
        assertEquals("CONFLICT", result.serverErrorCode)
    }

    @Test
    fun plainTextErrorBodyIsUsedAsMessage() {
        val body = "Username already taken"
            .toResponseBody("text/plain".toMediaType())
        val error = HttpException(Response.error<Unit>(400, body))

        val result = error.toClientError("fallback") as ClientError.Http

        assertEquals(400, result.statusCode)
        assertNull(result.serverErrorCode)
        assertEquals("Username already taken", result.message)
    }

    @Test
    fun emptyErrorBodyUsesFallbackWithStatusCode() {
        val body = "".toResponseBody("text/plain".toMediaType())
        val error = HttpException(Response.error<Unit>(500, body))

        val result = error.toClientError("Server error") as ClientError.Http

        assertEquals(500, result.statusCode)
        assertNull(result.serverErrorCode)
        assertEquals("Server error (HTTP 500)", result.message)
    }

    @Test
    fun ioExceptionProducesNetworkError() {
        val error = IOException("connect timed out")

        val result = error.toClientError("fallback") as ClientError.Network

        assertEquals("connect timed out", result.message)
    }

    @Test
    fun ioExceptionWithNullMessageProducesDefaultNetworkMessage() {
        val error = IOException()

        val result = error.toClientError("fallback") as ClientError.Network

        assertEquals("Could not reach server", result.message)
    }

    @Test
    fun unknownExceptionProducesUnknownError() {
        val error = RuntimeException("something exploded")

        val result = error.toClientError("fallback") as ClientError.Unknown

        assertEquals("something exploded", result.message)
    }

    @Test
    fun cancellationExceptionIsRethrown() {
        val cancel = CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            cancel.toClientError("fallback")
        }
    }

    @Test
    fun malformedJsonErrorBodyFallsBackToRawBodyAsMessage() {
        val raw = """{"errorCode":"""
        val body = raw.toResponseBody("application/json".toMediaType())
        val error = HttpException(Response.error<Unit>(400, body))

        val result = error.toClientError("fallback") as ClientError.Http

        assertEquals(400, result.statusCode)
        assertNull(result.serverErrorCode)
        assertEquals(raw, result.message)
    }

    @Test
    fun jsonErrorBodyWithUnknownKeysStillParses() {
        val body = """{"errorCode":"LOBBY_FULL","message":"Lobby is full","timestamp":"2026-06-10","path":"/lobby"}"""
            .toResponseBody("application/json".toMediaType())
        val error = HttpException(Response.error<Unit>(409, body))

        val result = error.toClientError("fallback") as ClientError.Http

        assertEquals("LOBBY_FULL", result.serverErrorCode)
        assertEquals("Lobby is full", result.message)
    }

    @Test
    fun jsonErrorBodyWithoutErrorCodeOrMessageUsesFallbackWithStatusCode() {
        val body = """{}""".toResponseBody("application/json".toMediaType())
        val error = HttpException(Response.error<Unit>(400, body))

        val result = error.toClientError("Request failed") as ClientError.Http

        assertNull(result.serverErrorCode)
        assertEquals("Request failed (HTTP 400)", result.message)
    }

    @Test
    fun unknownExceptionWithNullMessageUsesDefaultFallbackMessage() {
        val error = RuntimeException()

        val result = error.toClientError() as ClientError.Unknown

        assertEquals("An error occurred", result.message)
    }

    @Test
    fun toUserMessageReturnsMessageForHttpError() {
        val error = ClientError.Http(statusCode = 401, serverErrorCode = null, message = "Invalid credentials")

        assertEquals("Invalid credentials", error.toUserMessage())
    }

    @Test
    fun toUserMessagePrefixesNetworkError() {
        val error = ClientError.Network("connect timed out")

        assertEquals("Network error: connect timed out", error.toUserMessage())
    }

    @Test
    fun toUserMessageReturnsMessageForUnknownError() {
        val error = ClientError.Unknown("something exploded")

        assertEquals("something exploded", error.toUserMessage())
    }
}
