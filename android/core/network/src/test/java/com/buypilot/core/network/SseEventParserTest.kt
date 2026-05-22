package com.buypilot.core.network

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.common.sse.SseFrameParser
import com.buypilot.core.model.AgentEventType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SseEventParserTest {
    private val parser = SseEventParser(AppJson.instance)

    @Test
    fun parsesBudgetBeautyExample() {
        val events = parseExample("demo_budget_beauty.sse")

        assertEquals(10, events.size)
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
        assertEquals("clarify_turn_demo_clarify_001", events[1].nodeId)
    }

    @Test
    fun parsesErrorExample() {
        val events = parseExample("demo_error.sse")

        assertEquals(AgentEventType.Error, events[1].event)
        assertEquals("error_turn_demo_error_001", events[1].nodeId)
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
