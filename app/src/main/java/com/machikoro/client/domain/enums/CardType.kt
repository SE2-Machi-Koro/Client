package com.machikoro.client.domain.enums

/**
 * Mirrors the server's `CardType` — the establishment cards stocked in the
 * marketplace. Order matches the server enum so JSON round-trips by name.
 */
enum class CardType {
    WHEAT_FIELD,
    RANCH,
    FOREST,
    MINE,
    APPLE_ORCHARD,
    BAKERY,
    CONVENIENCE_STORE,
    CHEESE_FACTORY,
    FURNITURE_FACTORY,
    FRUIT_AND_VEGETABLE_MARKET,
    CAFE,
    FAMILY_RESTAURANT,
    STADIUM,
    TV_STATION,
    BUSINESS_CENTER,
}
