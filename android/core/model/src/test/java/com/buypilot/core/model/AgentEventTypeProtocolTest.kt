package com.buypilot.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * SSE protocol consistency guard (Kotlin side).
 *
 * Verifies that AgentEventType enum entries match the event types
 * defined in contracts/sse-events.schema.json.
 *
 * This is the Kotlin-side equivalent of the Python import-time guard
 * in backend/src/types/sse_events.py — if this test fails, the build
 * fails, preventing a misaligned APK from being built.
 *
 * Rust analogy: this is the borrow checker for the SSE protocol.
 */
class AgentEventTypeProtocolTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun kotlinEnumMatchesSchemaEventTypes() {
        val schemaFile = findContractsFile("sse-events.schema.json")
        val schemaText = schemaFile.readText()
        val schema = json.parseToJsonElement(schemaText).jsonObject

        // Extract event types from Schema $defs
        val defs = schema["\$defs"]?.jsonObject
            ?: fail("Schema missing \$defs") as Nothing

        val schemaEventTypes = mutableSetOf<String>()
        for ((name, defn) in defs) {
            val allOf = defn.jsonObject["allOf"]?.jsonArray ?: continue
            for (part in allOf) {
                val eventConst = part.jsonObject["properties"]
                    ?.jsonObject?.get("event")
                    ?.jsonObject?.get("const")
                    ?.jsonPrimitive?.content
                if (eventConst != null) {
                    schemaEventTypes.add(eventConst)
                }
            }
        }

        // Extract event types from Kotlin enum (exclude "unknown" fallback)
        val kotlinEventTypes = AgentEventType.entries
            .map { it.wireValue }
            .filter { it != "unknown" }
            .toSet()

        // Compare
        val inSchemaNotKotlin = schemaEventTypes - kotlinEventTypes
        val inKotlinNotSchema = kotlinEventTypes - schemaEventTypes

        if (inSchemaNotKotlin.isNotEmpty() || inKotlinNotSchema.isNotEmpty()) {
            val errors = mutableListOf<String>()

            if (inSchemaNotKotlin.isNotEmpty()) {
                errors.add(
                    "Schema defines event types missing from AgentEventType.kt: " +
                        "$inSchemaNotKotlin\n" +
                        "  → Add enum entries to AgentEventType.kt"
                )
            }

            if (inKotlinNotSchema.isNotEmpty()) {
                errors.add(
                    "AgentEventType.kt defines event types not in Schema: " +
                        "$inKotlinNotSchema\n" +
                        "  → Update contracts/sse-events.schema.json (Schema is source of truth)"
                )
            }

            fail(
                "SSE protocol drift detected — build cannot succeed.\n" +
                    errors.joinToString("\n\n") +
                    "\n\nFix the drift, then rebuild."
            )
        }

        assertEquals(
            "Schema and Kotlin event types should have the same count",
            schemaEventTypes.size,
            kotlinEventTypes.size,
        )
    }

    /**
     * Walk up from user.dir to find contracts/<relativePath>.
     * Same pattern as SseEventParserTest.
     */
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
