package com.machikoro.client.domain.model.state

// UI state for one buy/build action in the current BUY_OR_BUILD phase.
enum class PurchaseState {
    IDLE,
    PENDING,
    SUCCESS,
    ERROR
}
