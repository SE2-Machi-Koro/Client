package com.machikoro.client.network.websocket

import com.machikoro.client.network.error.ClientError
import org.json.JSONObject

object WsErrorParser {

    fun parse(json: JSONObject): ClientError.WebSocket {
        val payload = json.optJSONObject("payload")
        val serverCode = payload?.optString("errorCode").orEmpty()
            .ifBlank { payload?.optString("code").orEmpty() }
            .ifBlank { "UNKNOWN" }
        val userMessage = json.optString("content")
            .ifBlank { payload?.optString("message").orEmpty() }
            .ifBlank { ClientError.UNKNOWN_USER_MESSAGE }
        return ClientError.WebSocket(serverCode = serverCode, userMessage = userMessage)
    }

    fun parseStompErrorBody(body: String): ClientError.WebSocket {
        val trimmed = body.trim()
        return if (trimmed.startsWith("{")) {
            runCatching { parse(JSONObject(trimmed)) }.getOrElse {
                ClientError.WebSocket(
                    serverCode = "STOMP_ERROR",
                    userMessage = ClientError.UNKNOWN_USER_MESSAGE,
                )
            }
        } else {
            ClientError.WebSocket(
                serverCode = "STOMP_ERROR",
                userMessage = trimmed.ifBlank { ClientError.UNKNOWN_USER_MESSAGE },
            )
        }
    }
}
