package com.buypilot.core.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CartActionPayloadTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun checkoutPreviewDeserializesWithoutCrash() {
        val payload = json.decodeFromString<CartActionPayload>(
            """
            {
              "action": "checkout_preview",
              "status": "success",
              "cart": {
                "items": [
                  {"product_id": "p1", "name": "商品A", "price": 52.0, "quantity": 1}
                ],
                "total_items": 1,
                "total_price": 52.0
              }
            }
            """.trimIndent(),
        )
        assertEquals("checkout_preview", payload.action)
        assertEquals("success", payload.status)
        assertEquals(1, payload.cart?.totalItems)
        assertEquals(52.0, payload.cart?.totalPrice ?: 0.0, 0.01)
    }

    @Test
    fun checkoutConfirmDeserializesWithoutCrash() {
        val payload = json.decodeFromString<CartActionPayload>(
            """{"action": "checkout_confirm", "status": "success"}""",
        )
        assertEquals("checkout_confirm", payload.action)
        assertEquals("success", payload.status)
        assertNull(payload.cart)
    }

    @Test
    fun checkoutCancelDeserializesWithoutCrash() {
        val payload = json.decodeFromString<CartActionPayload>(
            """{"action": "checkout_cancel", "status": "success"}""",
        )
        assertEquals("checkout_cancel", payload.action)
    }

    @Test
    fun checkoutFailedStatusDeserializesWithEmptyCart() {
        val payload = json.decodeFromString<CartActionPayload>(
            """{"action": "checkout_preview", "status": "failed", "cart": {"items": [], "total_items": 0, "total_price": 0.0}}""",
        )
        assertEquals("failed", payload.status)
        assertEquals(0, payload.cart?.totalItems ?: -1)
    }

    @Test
    fun unknownActionDeserializesGracefully() {
        val payload = json.decodeFromString<CartActionPayload>(
            """{"action": "some_future_action", "status": "success"}""",
        )
        assertEquals("some_future_action", payload.action)
    }
}
