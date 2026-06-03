package com.buypilot.feature.chat.ui

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductSkuOptionsTest {
    @Test
    fun skuOptionsUsePropertiesAsReadableLabelAndPrice() {
        val options = listOf(
            buildJsonObject {
                put("price", JsonPrimitive(5299.0))
                putJsonObject("properties") {
                    put("颜色", "深空黑")
                    put("存储", "256G")
                }
            },
        ).toSkuUiOptions()

        assertEquals(1, options.size)
        assertEquals("深空黑 · 256G", options.single().label)
        assertEquals(5299.0, options.single().price ?: 0.0, 0.001)
    }

    @Test
    fun malformedSkuOptionsFallBackWithoutCrashing() {
        val options = listOf(
            buildJsonObject {
                put("price", JsonPrimitive("699"))
            },
            buildJsonObject {
                putJsonObject("properties") {
                    put("版本", "")
                }
            },
        ).toSkuUiOptions()

        assertEquals(2, options.size)
        assertEquals("默认规格", options[0].label)
        assertEquals(699.0, options[0].price ?: 0.0, 0.001)
        assertEquals("默认规格", options[1].label)
        assertEquals(null, options[1].price)
    }
}
