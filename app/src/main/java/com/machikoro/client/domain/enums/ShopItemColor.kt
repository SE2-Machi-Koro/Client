package com.machikoro.client.domain.enums

/**
 * UI color category for shop items.
 *
 * This is display-only state. The backend uses CardType/LandmarkType and does
 * not receive this value in purchase requests.
 */
enum class ShopItemColor {
    BLUE,
    GREEN,
    RED,
    PURPLE,
    LANDMARK
}

fun ShopItemColor.triggersForResolvingEffects(
    ownerPlayerId: Int,
    activePlayerId: Int?,
): Boolean = when (this) {
    ShopItemColor.RED -> activePlayerId != null && ownerPlayerId != activePlayerId
    ShopItemColor.BLUE -> true
    ShopItemColor.GREEN -> ownerPlayerId == activePlayerId
    ShopItemColor.PURPLE -> ownerPlayerId == activePlayerId
    ShopItemColor.LANDMARK -> false
}