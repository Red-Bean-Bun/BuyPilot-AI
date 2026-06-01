package com.buypilot.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatViewModelCartCommandTest {
    @Test
    fun addToCartCommandUsesOnlyExplicitProductIdAndOneItemQuantity() {
        val command = buildAddToCartCommand(
            productId = "p_digital_017",
            rank = 2,
            productName = "Apple iPhone 17 Pro Max",
        )

        assertEquals("把商品ID p_digital_017 加入购物车，数量1件", command)
        assertTrue(command.contains("商品ID p_digital_017"))
        assertFalse(command.contains("第2个商品"))
        assertFalse(command.contains("Apple iPhone 17 Pro Max"))
        assertFalse(command.contains("这个商品"))
    }

    @Test
    fun addToCartIntentResolvesUniqueBrandMention() {
        val target = resolveAddToCartTarget(
            message = "把 OPPO 加入购物车",
            candidates = listOf(
                ProductCartCandidate(
                    deckId = "deck_1",
                    productId = "p_iphone",
                    rank = 1,
                    productName = "Apple iPhone 16 Pro",
                    brand = "Apple",
                    subCategory = "手机",
                    category = "数码电子",
                ),
                ProductCartCandidate(
                    deckId = "deck_1",
                    productId = "p_oppo",
                    rank = 2,
                    productName = "OPPO Find X8",
                    brand = "OPPO",
                    subCategory = "手机",
                    category = "数码电子",
                ),
            ),
        )

        assertEquals("p_oppo", target?.productId)
    }

    @Test
    fun addToCartIntentDoesNotGuessWhenTargetIsAmbiguous() {
        val target = resolveAddToCartTarget(
            message = "把 pro 加入购物车",
            candidates = listOf(
                ProductCartCandidate(
                    deckId = "deck_1",
                    productId = "p_iphone",
                    rank = 1,
                    productName = "Apple iPhone 16 Pro",
                    brand = "Apple",
                    subCategory = "手机",
                    category = "数码电子",
                ),
                ProductCartCandidate(
                    deckId = "deck_1",
                    productId = "p_xiaomi",
                    rank = 2,
                    productName = "Xiaomi 15 Pro",
                    brand = "Xiaomi",
                    subCategory = "手机",
                    category = "数码电子",
                ),
            ),
        )

        assertEquals(null, target)
    }

    @Test
    fun addToCartIntentCanResolveOrdinal() {
        val target = resolveAddToCartTarget(
            message = "把第二个加入购物车",
            candidates = listOf(
                ProductCartCandidate(
                    deckId = "deck_1",
                    productId = "p_iphone",
                    rank = 1,
                    productName = "Apple iPhone 16 Pro",
                    brand = "Apple",
                    subCategory = "手机",
                    category = "数码电子",
                ),
                ProductCartCandidate(
                    deckId = "deck_1",
                    productId = "p_oppo",
                    rank = 2,
                    productName = "OPPO Find X8",
                    brand = "OPPO",
                    subCategory = "手机",
                    category = "数码电子",
                ),
            ),
        )

        assertEquals("p_oppo", target?.productId)
    }

    @Test
    fun addToCartIntentUsesFallbackForDeicticReferenceOnly() {
        val fallback = ProductCartCandidate(
            deckId = "deck_1",
            productId = "p_current",
            rank = 2,
            productName = "OPPO Find X8",
            brand = "OPPO",
            subCategory = "手机",
            category = "数码电子",
        )

        val deicticTarget = resolveAddToCartTarget(
            message = "把这个加入购物车",
            candidates = emptyList(),
            fallbackCandidate = fallback,
        )
        val explicitMiss = resolveAddToCartTarget(
            message = "把 vivo 加入购物车",
            candidates = emptyList(),
            fallbackCandidate = fallback,
        )
        val genericMiss = resolveAddToCartTarget(
            message = "把手机加入购物车",
            candidates = emptyList(),
            fallbackCandidate = fallback,
        )

        assertEquals("p_current", deicticTarget?.productId)
        assertEquals(null, explicitMiss)
        assertEquals(null, genericMiss)
    }
}
