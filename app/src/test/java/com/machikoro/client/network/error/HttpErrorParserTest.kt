package com.machikoro.client.network.error

import java.io.IOException
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class HttpErrorParserTest {
    @Test
    fun jsonHttpExceptionPreservesServerContract() {
        val errorBody = """
            {"code":"INVALID_CREDENTIALS","message":"Invalid username or password","timestamp":1714000000000}
        """.trimIndent().toResponseBody(JSON_MEDIA_TYPE.toMediaType())

        val error = HttpErrorParser.fromThrowable(
            HttpException(Response.error<Unit>(401, errorBody)),
            fallbackMessage = "Login failed. Please try again.",
        )

        assertTrue(error is ClientError.Api)
        val apiError = error as ClientError.Api
        assertEquals(401, apiError.statusCode)
        assertEquals("INVALID_CREDENTIALS", apiError.serverCode)
        assertEquals("Invalid username or password", apiError.userMessage)
        assertEquals(1714000000000, apiError.timestampMillis)
    }

    @Test
    fun validationCodeCreatesValidationError() {
        val errorBody = """
            {"code":"VALIDATION_FAILED","message":"Username must not be blank","timestamp":1714000000001}
        """.trimIndent().toResponseBody(JSON_MEDIA_TYPE.toMediaType())

        val error = HttpErrorParser.fromResponse(
            Response.error<Unit>(400, errorBody),
            fallbackMessage = "Request failed. Please try again.",
        )

        assertTrue(error is ClientError.Validation)
        val validation = error as ClientError.Validation
        assertEquals(400, validation.statusCode)
        assertEquals("VALIDATION_FAILED", validation.serverCode)
        assertEquals("Username must not be blank", validation.userMessage)
        assertEquals(1714000000001, validation.timestampMillis)
    }

    @Test
    fun plainTextBodyIsPreservedForLegacyServerResponses() {
        val errorBody = "Username 'alice' is already taken".toResponseBody(TEXT_MEDIA_TYPE.toMediaType())

        val error = HttpErrorParser.fromResponse(
            Response.error<Unit>(400, errorBody),
            fallbackMessage = "Registration failed. Please try again.",
        )

        assertTrue(error is ClientError.Api)
        val apiError = error as ClientError.Api
        assertEquals(400, apiError.statusCode)
        assertEquals("HTTP_400", apiError.serverCode)
        assertEquals("Username 'alice' is already taken", apiError.userMessage)
        assertEquals(ClientError.UNKNOWN_TIMESTAMP, apiError.timestampMillis)
    }

    @Test
    fun emptyBodyUsesFallbackMessageAndStatusCode() {
        val error = HttpErrorParser.fromResponse(
            Response.error<Unit>(500, "".toResponseBody(TEXT_MEDIA_TYPE.toMediaType())),
            fallbackMessage = "Request failed. Please try again.",
        )

        assertTrue(error is ClientError.Api)
        val apiError = error as ClientError.Api
        assertEquals(500, apiError.statusCode)
        assertEquals("HTTP_500", apiError.serverCode)
        assertEquals("Request failed. Please try again.", apiError.userMessage)
    }

    @Test
    fun malformedJsonBodyUsesFallbackMessageWithoutLeakingPayload() {
        val error = HttpErrorParser.fromResponse(
            Response.error<Unit>(502, """{"error":"gateway"}""".toResponseBody(JSON_MEDIA_TYPE.toMediaType())),
            fallbackMessage = "Request failed. Please try again.",
        )

        assertEquals("Request failed. Please try again.", error.userMessage)
    }

    @Test
    fun ioExceptionCreatesNetworkError() {
        val error = HttpErrorParser.fromThrowable(IOException("connect timed out"))

        assertEquals(ClientError.Network("connect timed out"), error)
        assertEquals(ClientError.NETWORK_USER_MESSAGE, error.userMessage)
    }

    @Test
    fun unknownExceptionUsesProvidedFallbackMessage() {
        val error = HttpErrorParser.fromThrowable(
            IllegalStateException("unexpected state"),
            fallbackMessage = "Action failed. Please try again.",
        )

        assertTrue(error is ClientError.Unknown)
        assertEquals("Action failed. Please try again.", error.userMessage)
        assertEquals("Unexpected failure: unexpected state", error.diagnosticMessage)
    }

    @Test
    fun cancellationExceptionIsRethrown() {
        val cancellation = CancellationException("cancelled")

        val thrown = assertThrows(CancellationException::class.java) {
            HttpErrorParser.fromThrowable(cancellation)
        }

        assertSame(cancellation, thrown)
    }

    private companion object {
        const val JSON_MEDIA_TYPE = "application/json"
        const val TEXT_MEDIA_TYPE = "text/plain"
    }
}
