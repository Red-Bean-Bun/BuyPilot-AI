package com.buypilot.core.network

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.common.sse.SseFrameParser
import com.buypilot.core.common.sse.SseFrame
import com.buypilot.core.model.AgentEventType
import com.buypilot.core.model.CompareCardPayload
import com.buypilot.core.model.ProductCardPayload
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SseEventParserTest {
    private val parser = SseEventParser(AppJson.instance)

    @Test
    fun parsesBudgetBeautyExample() {
        val events = parseExample("demo_budget_beauty.sse")

        assertEquals(13, events.size)
        assertEquals(AgentEventType.Thinking, events.first().event)
        assertEquals(AgentEventType.Done, events.last().event)
        assertTrue(events.any { it.event == AgentEventType.ProductCard && it.deckId != null })
        assertEquals("demo_001", events.first().sessionId)
    }

    @Test
    fun parsesClarificationExample() {
        val events = parseExample("demo_clarification.sse")

        assertEquals(
            listOf(
                AgentEventType.Thinking,
                AgentEventType.Clarification,
                AgentEventType.Done,
            ),
            events.map { it.event },
        )
        assertEquals("clarification_clarify_001", events[1].nodeId)
    }

    @Test
    fun parsesErrorExample() {
        val events = parseExample("demo_error.sse")

        assertEquals(AgentEventType.Error, events[1].event)
        assertEquals("error_error_001", events[1].nodeId)
    }

    @Test
    fun productCardWithoutDeckIdFallsBackToTurnDeckAndKeepsRelativeImageUrl() {
        val event = parser.parse(
            SseFrame(
                event = "product_card",
                data = """
                    {
                      "event": "product_card",
                      "session_id": "sess_1",
                      "turn_id": "turn_abc",
                      "seq": 1,
                      "payload": {
                        "rank": 1,
                        "product": {
                          "product_id": "p1",
                          "name": "测试商品",
                          "image_url": "/assets/products/1_美妆护肤/images/p_beauty_001_live.jpg",
                          "category": "美妆护肤"
                        },
                        "reason": "匹配需求",
                        "risk_notes": [],
                        "evidence": [],
                        "actions": []
                      }
                    }
                """.trimIndent(),
            ),
            fallbackSeq = 1,
        )

        assertEquals("deck_turn_abc", event.deckId)
        assertEquals("swipe_deck_item", event.displayMode)
        val payload = event.payload as ProductCardPayload
        assertEquals("/assets/products/1_美妆护肤/images/p_beauty_001_live.jpg", payload.product.imageUrl)
    }

    @Test
    fun parsesCompareCardEvent() {
        val event = parser.parse(
            SseFrame(
                event = "compare_card",
                data = """
                    {
                      "event": "compare_card",
                      "session_id": "sess_1",
                      "turn_id": "turn_cmp",
                      "seq": 2,
                      "compare_id": "cmp_1",
                      "source_deck_id": "deck_1",
                      "mode": "exploratory",
                      "products": [
                        {"product_id": "p1", "name": "商品一", "category": "数码电子"},
                        {"product_id": "p2", "name": "商品二", "category": "数码电子"}
                      ],
                      "axes": [
                        {
                          "name": "影像",
                          "values": [
                            {"product_id": "p1", "score": 86, "detail": "长焦更强"},
                            {"product_id": "p2", "score": 72, "detail": "日常够用"}
                          ]
                        }
                      ],
                      "winner_product_id": "p1",
                      "winner_reason": "更适合拍照",
                      "tradeoffs": ["p1 影像更强"],
                      "risk_notes": [],
                      "confidence": "medium"
                    }
                """.trimIndent(),
            ),
            fallbackSeq = 2,
        )

        assertEquals(AgentEventType.CompareCard, event.event)
        assertEquals("summary_card", event.displayMode)
        assertEquals("compare_turn_cmp", event.nodeId)
        val payload = event.payload as CompareCardPayload
        assertEquals("cmp_1", payload.compareId)
        assertEquals("p1", payload.winnerProductId)
        assertEquals("影像", payload.axes.single().name)
        assertEquals(86.0, payload.axes.single().values.first().score)
    }

    private fun parseExample(fileName: String) =
        SseFrameParser.parseFrames(findContractsFile("examples/$fileName").readText())
            .mapIndexed { index, frame -> parser.parse(frame, fallbackSeq = index + 1) }

    private fun findContractsFile(relativePath: String): File {
        val userDir = System.getProperty("user.dir") ?: "."
        var current = File(userDir).canonicalFile
        while (true) {
            val candidate = File(current, "contracts/$relativePath").canonicalFile
            if (candidate.exists()) return candidate
            current = current.parentFile?.canonicalFile ?: break
        }
        error("Could not locate contracts/$relativePath from $userDir")
    }
}
