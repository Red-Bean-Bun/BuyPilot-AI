package com.buypilot.core.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalDecisionPayloadTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun parsesScoreBreakdownWhenBackendSendsIt() {
        val payload = json.decodeFromString<FinalDecisionPayload>(
            """
            {
              "winner_product_id": "p_1",
              "summary": "优先选这一款",
              "score_breakdown": {
                "retrieval": 0.92,
                "criteria_match": 0.86,
                "risk_penalty": 0.12
              }
            }
            """.trimIndent(),
        )

        assertEquals("p_1", payload.winnerProductId)
        assertEquals(0.92, payload.scoreBreakdown.getValue("retrieval"), 0.0001)
        assertEquals(0.86, payload.scoreBreakdown.getValue("criteria_match"), 0.0001)
        assertEquals(0.12, payload.scoreBreakdown.getValue("risk_penalty"), 0.0001)
    }

    @Test
    fun missingScoreBreakdownDefaultsToEmptyMap() {
        val payload = json.decodeFromString<FinalDecisionPayload>(
            """{"summary":"优先选这一款"}""",
        )

        assertTrue(payload.scoreBreakdown.isEmpty())
    }
}
