package com.machikoro.client.network.websocket

import com.machikoro.client.network.error.ClientError
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class WsErrorParserTest {

    @Test
    fun parseMapsErrorCodeAndContentToClientError() {
        val json = JSONObject("""{"type":"ERROR","content":"Lobby is full","payload":{"errorCode":"LOBBY_FULL"}}""")

        val error = WsErrorParser.parse(json)

        assertEquals("LOBBY_FULL", error.serverCode)
        assertEquals("Lobby is full", error.userMessage)
    }

    @Test
    fun parseUsesPayloadMessageWhenContentIsBlank() {
        val json = JSONObject("""{"type":"ERROR","content":"","payload":{"errorCode":"GAME_STARTED","message":"Game already started"}}""")

        val error = WsErrorParser.parse(json)

        assertEquals("GAME_STARTED", error.serverCode)
        assertEquals("Game already started", error.userMessage)
    }

    @Test
    fun parseUsesUnknownFallbackWhenNoMessageFound() {
        val json = JSONObject("""{"type":"ERROR","payload":{"errorCode":"GAME_FINISHED"}}""")

        val error = WsErrorParser.parse(json)

        assertEquals("GAME_FINISHED", error.serverCode)
        assertEquals(ClientError.UNKNOWN_USER_MESSAGE, error.userMessage)
    }

    @Test
    fun parseUsesUnknownServerCodeWhenErrorCodeIsAbsent() {
        val json = JSONObject("""{"type":"ERROR","content":"Something went wrong","payload":{}}""")

        val error = WsErrorParser.parse(json)

        assertEquals("UNKNOWN", error.serverCode)
        assertEquals("Something went wrong", error.userMessage)
    }

    @Test
    fun parseUsesFallbackCodeFieldWhenErrorCodeIsAbsent() {
        val json = JSONObject("""{"type":"ERROR","content":"Duplicate","payload":{"code":"DUPLICATE_PURPLE_ESTABLISHMENT","message":"Duplicate"}}""")

        val error = WsErrorParser.parse(json)

        assertEquals("DUPLICATE_PURPLE_ESTABLISHMENT", error.serverCode)
    }

    @Test
    fun parseHandlesMissingPayload() {
        val json = JSONObject("""{"type":"ERROR","content":"Unexpected failure"}""")

        val error = WsErrorParser.parse(json)

        assertEquals("UNKNOWN", error.serverCode)
        assertEquals("Unexpected failure", error.userMessage)
    }

    @Test
    fun parseReturnsClientErrorWebSocketType() {
        val json = JSONObject("""{"type":"ERROR","content":"Err","payload":{"errorCode":"X"}}""")

        val error = WsErrorParser.parse(json)

        assertEquals(ClientError.WebSocket::class, error::class)
    }

    @Test
    fun parseStompErrorBodyParsesPlainTextBody() {
        val error = WsErrorParser.parseStompErrorBody("Something went wrong")

        assertEquals("STOMP_ERROR", error.serverCode)
        assertEquals("Something went wrong", error.userMessage)
    }

    @Test
    fun parseStompErrorBodyParsesJsonBody() {
        val body = """{"type":"ERROR","content":"Auth failed","payload":{"errorCode":"AUTH_FAILED"}}"""

        val error = WsErrorParser.parseStompErrorBody(body)

        assertEquals("AUTH_FAILED", error.serverCode)
        assertEquals("Auth failed", error.userMessage)
    }

    @Test
    fun parseStompErrorBodyUsesFallbackForBlankBody() {
        val error = WsErrorParser.parseStompErrorBody("   ")

        assertEquals("STOMP_ERROR", error.serverCode)
        assertEquals(ClientError.UNKNOWN_USER_MESSAGE, error.userMessage)
    }

    @Test
    fun parseStompErrorBodyHandlesMalformedJson() {
        val error = WsErrorParser.parseStompErrorBody("{not valid json}")

        assertEquals("STOMP_ERROR", error.serverCode)
        assertEquals("{not valid json}", error.userMessage)
    }
}
