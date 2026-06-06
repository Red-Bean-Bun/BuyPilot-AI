package com.buypilot.feature.chat.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsManagerTest {

    @Test
    fun extractSentenceReturnsNullWhenNoSentenceEnd() {
        val (sentence, remaining) = extractSentenceForTts("这是一段没有结尾的文字")
        assertNull(sentence)
        assertEquals("这是一段没有结尾的文字", remaining)
    }

    @Test
    fun extractSentenceSplitsAtChinesePeriod() {
        val (sentence, remaining) = extractSentenceForTts("第一句话。第二句话还在输入")
        assertEquals("第一句话。", sentence)
        assertEquals("第二句话还在输入", remaining)
    }

    @Test
    fun extractSentenceSplitsAtQuestionMark() {
        val (sentence, remaining) = extractSentenceForTts("请问你的肤质是？不确定也没关系")
        assertEquals("请问你的肤质是？", sentence)
        assertEquals("不确定也没关系", remaining)
    }

    @Test
    fun extractSentenceSplitsAtNewline() {
        val (sentence, remaining) = extractSentenceForTts("第一行\n第二行内容")
        assertEquals("第一行", sentence)
        assertEquals("第二行内容", remaining)
    }

    @Test
    fun extractSentenceReturnsNullForEmptyBuffer() {
        val (sentence, remaining) = extractSentenceForTts("")
        assertNull(sentence)
        assertEquals("", remaining)
    }

    @Test
    fun stripMarkdownRemovesBoldAndLinks() {
        val result = "**油性肌肤**适用，[查看详情](https://example.com)".stripMarkdown()
        assertEquals("油性肌肤适用，查看详情", result)
    }

    @Test
    fun stripMarkdownRemovesHeadingPrefix() {
        val result = "### 推荐理由".stripMarkdown()
        assertEquals("推荐理由", result)
    }

    @Test
    fun defaultTtsEnabledIsFalse() {
        val state = com.buypilot.feature.chat.state.ChatUiState()
        assertEquals(false, state.ttsEnabled)
    }
}
