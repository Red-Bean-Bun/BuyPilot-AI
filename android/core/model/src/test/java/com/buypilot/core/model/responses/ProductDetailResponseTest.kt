package com.buypilot.core.model.responses

import com.buypilot.core.common.json.AppJson
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductDetailResponseTest {
    @Test
    fun decodesProductDetailKnowledgeFields() {
        val response = AppJson.instance.decodeFromString<ProductDetailResponse>(
            """
            {
              "product": {
                "product_id": "p_digital_001",
                "name": "BuyPilot Phone",
                "brand": "BuyPilot",
                "category": "数码电子",
                "price": 3999,
                "sku_options": [
                  {
                    "price": 4299,
                    "properties": {
                      "颜色": "黑色",
                      "存储": "256G"
                    }
                  }
                ]
              },
              "marketing_description": "适合需要轻薄机身和长续航的用户。",
              "highlights": ["长续航", "轻薄机身"],
              "faqs": [
                {
                  "question": "支持快充吗？",
                  "answer": "支持。"
                }
              ],
              "reviews": [
                {
                  "nickname": "用户A",
                  "rating": 5,
                  "content": "续航很稳。"
                }
              ],
              "unknown_backend_field": "ignored"
            }
            """.trimIndent(),
        )

        assertEquals("p_digital_001", response.product.productId)
        assertEquals("BuyPilot Phone", response.product.name)
        assertEquals(3999.0, response.product.price ?: 0.0, 0.001)
        assertEquals("适合需要轻薄机身和长续航的用户。", response.marketingDescription)
        assertEquals(listOf("长续航", "轻薄机身"), response.highlights)
        assertEquals("支持快充吗？", response.faqs.single().question)
        assertEquals("支持。", response.faqs.single().answer)
        assertEquals("用户A", response.reviews.single().nickname)
        assertEquals(5, response.reviews.single().rating)
        assertTrue(response.product.skuOptions?.isNotEmpty() == true)
    }
}
