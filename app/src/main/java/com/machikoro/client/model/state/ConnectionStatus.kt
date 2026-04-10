package com.machikoro.client.model.state

enum class ConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}

fun ConnectionStatus.toDisplayText(): String = when (this) {
    ConnectionStatus.IDLE -> "idle"
    ConnectionStatus.CONNECTING -> "connecting"
    ConnectionStatus.CONNECTED -> "connected"
    ConnectionStatus.DISCONNECTED -> "disconnected"
    ConnectionStatus.ERROR -> "connection error"
}
