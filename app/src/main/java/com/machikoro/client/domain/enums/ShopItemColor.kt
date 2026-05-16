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
