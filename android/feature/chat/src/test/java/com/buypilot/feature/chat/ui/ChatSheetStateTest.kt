package com.buypilot.feature.chat.ui

import androidx.compose.runtime.saveable.SaverScope
import com.buypilot.core.model.FinalDecisionPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSheetStateTest {
    @Test
    fun decisionEvidenceSheetSaverKeepsSourceNodeKey() {
        val content = ChatSheetContent.DecisionEvidence(
            payload = FinalDecisionPayload(summary = "优先选这款"),
            sourceNodeKey = "final_decision_1",
        )
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }

        val saved = with(ChatSheetContentSaver) { scope.save(content) }
        val restored = saved?.let { ChatSheetContentSaver.restore(it) }

        assertTrue(restored is ChatSheetContent.DecisionEvidence)
        val evidence = restored as ChatSheetContent.DecisionEvidence
        assertEquals("优先选这款", evidence.payload.summary)
        assertEquals("final_decision_1", evidence.sourceNodeKey)
    }
}
