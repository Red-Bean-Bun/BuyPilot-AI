package com.buypilot.core.common.sse

object SseFrameParser {
    fun parseFrames(raw: String): List<SseFrame> {
        return raw
            .replace("\r\n", "\n")
            .split("\n\n")
            .mapNotNull(::parseFrame)
    }

    fun parseFrame(block: String): SseFrame? {
        var event: String? = null
        var id: String? = null
        val dataLines = mutableListOf<String>()

        block.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                when {
                    line.startsWith("event:") -> event = line.substringAfter("event:").trim()
                    line.startsWith("id:") -> id = line.substringAfter("id:").trim()
                    line.startsWith("data:") -> dataLines += line.substringAfter("data:").trimStart()
                }
            }

        if (event == null && dataLines.isEmpty()) return null
        return SseFrame(
            event = event,
            data = dataLines.joinToString(separator = "\n"),
            id = id,
        )
    }
}
