package com.machikoro.client.domain.model.state

// Local UI state for the first purchase flow; issue #39 should replace
// optimistic SUCCESS handling with server success/error feedback.
enum class PurchaseState {
    IDLE,
    PENDING,
    SUCCESS
}
