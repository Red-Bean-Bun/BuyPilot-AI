package com.buypilot.feature.chat.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/** Sentence-end boundary pattern shared across calls — compiled once. */
private val SentenceEndRegex = Regex("""[。？！?!\n]+""")

/** Markdown link pattern: [text](url) → text. */
private val MarkdownLinkRegex = Regex("""\[([^\]]+)]\([^)]+\)""")

/** Markdown image pattern: ![alt](url) → removed. */
private val MarkdownImageRegex = Regex("""!\[[^\]]*]\([^)]+\)""")

/** Markdown heading prefix: # … → removed. */
private val MarkdownHeadingRegex = Regex("""^#{1,6}\s*""", RegexOption.MULTILINE)

/**
 * Lightweight wrapper around Android system TTS.
 * No external SDK or API key required.
 * Lifecycle: create when ViewModel is created, shutdown in onCleared().
 */
class TtsManager(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var initialized = false
    private val utteranceCounter = AtomicInteger(0)

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.CHINESE)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.setLanguage(Locale.getDefault())
                    }
                    engine.setSpeechRate(1.0f)
                    initialized = true
                }
            } else {
                Log.w(TAG, "TTS init failed status=$status")
            }
        }
    }

    fun speak(text: String) {
        if (!initialized || text.isBlank()) return
        val cleaned = text.stripMarkdown().trim()
        if (cleaned.isBlank()) return
        tts?.speak(cleaned, TextToSpeech.QUEUE_ADD, null, "buypilot_${utteranceCounter.incrementAndGet()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        initialized = false
    }

    companion object {
        private const val TAG = "BuyPilotTts"
    }
}

/**
 * Strip markdown formatting for cleaner TTS output.
 */
internal fun String.stripMarkdown(): String =
    replace("**", "")
        .replace("*", "")
        .replace("`", "")
        .replace(MarkdownLinkRegex, "$1")
        .replace(MarkdownImageRegex, "")
        .replace(MarkdownHeadingRegex, "")

/**
 * Detect sentence boundaries for incremental TTS during streaming text_delta.
 * Returns the sentence to speak (if any) and the remaining buffer.
 */
internal fun extractSentenceForTts(buffer: String): Pair<String?, String> {
    val match = SentenceEndRegex.find(buffer) ?: return null to buffer
    val endIndex = match.range.last + 1
    val sentence = buffer.substring(0, endIndex).trim()
    val remaining = buffer.substring(endIndex).trimStart()
    return if (sentence.isNotBlank()) sentence to remaining else null to remaining
}
