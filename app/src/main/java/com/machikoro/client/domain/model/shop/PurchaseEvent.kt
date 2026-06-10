package com.machikoro.client.domain.model.shop

import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.network.error.ClientError

/**
 * One-shot purchase feedback from the WebSocket layer.
 *
 * The UI keeps a single pending purchase, so a success event only needs the
 * server target that was bought. Failures may not include a target because
 * Spring can reject the purchase before the controller builds a response.
 */
sealed class PurchaseEvent {
    data class Success(
        val purchaseType: PurchaseType,
        val itemType: String
    ) : PurchaseEvent()

    data class Failure(
        val error: ClientError.WebSocket
    ) : PurchaseEvent()
}
