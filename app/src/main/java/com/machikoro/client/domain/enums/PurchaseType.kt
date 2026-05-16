package com.machikoro.client.domain.enums

/**
 * Type of buy/build request sent to the server during BUY_OR_BUILD.
 *
 * The enum names intentionally match the server PurchaseType values because
 * OkHttpWebSocketClient serializes them by name in the WebSocket payload.
 */
enum class PurchaseType {
    ESTABLISHMENT,
    LANDMARK
}
