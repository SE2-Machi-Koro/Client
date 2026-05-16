package com.machikoro.client.ui.game

import com.machikoro.client.R
import com.machikoro.client.domain.model.shop.ShopCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ShopImageResolverTest {
    @Test
    fun everyDefaultShopItemResolvesToDrawableOrFallback() {
        val fallback = ShopImageResolver.drawableFor(ShopImageResolver.FALLBACK_IMAGE_KEY)

        ShopCatalog.defaultItems.forEach { item ->
            val resolved = ShopImageResolver.drawableFor(item.imageKey)

            assertNotEquals("Missing drawable id for ${item.imageKey}", 0, resolved)
            if (item.imageKey != ShopImageResolver.FALLBACK_IMAGE_KEY &&
                item.imageKey != "card_business_center"
            ) {
                assertNotEquals("Unexpected fallback for ${item.imageKey}", fallback, resolved)
            }
        }
    }

    @Test
    fun unknownImageKeyUsesSafeFallback() {
        assertEquals(
            R.drawable.card_bakery_image,
            ShopImageResolver.drawableFor("missing_card_art")
        )
    }
}
