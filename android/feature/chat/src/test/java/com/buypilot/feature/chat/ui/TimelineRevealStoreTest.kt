package com.buypilot.feature.chat.ui

import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ProductSwipeState
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.model.UserMessageNode
import com.buypilot.feature.chat.state.ChatUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineRevealStoreTest {
    @Test
    fun updateTextProgressKeepsLatestProgressWithoutSnapshottingEveryCharacter() {
        val store = TimelineRevealStore()

        store.updateTextProgress(key = "text_1", visible = 1, total = 100)
        store.updateTextProgress(key = "text_1", visible = 2, total = 100)

        assertEquals(1, store.textRevealProgressByKey["text_1"]?.visibleLength)
        assertEquals(2, store.textRevealProgress("text_1")?.visibleLength)

        store.updateTextProgress(key = "text_1", visible = 25, total = 100)

        assertEquals(25, store.textRevealProgressByKey["text_1"]?.visibleLength)
        assertEquals(25, store.textRevealProgress("text_1")?.visibleLength)
    }

    @Test
    fun pruneToKeysDropsRemovedNodesButKeepsVisibleProgressForCurrentNodes() {
        val store = TimelineRevealStore()

        store.markTimelineItemEntered("turn_1")
        store.markTimelineItemEntered("old_turn")
        store.markStructuredNodeStarted("deck_1")
        store.markStructuredNodeStarted("old_deck")
        store.markStructuredNodeEntered("deck_1")
        store.markStructuredNodeEntered("old_deck")
        store.markTextCompleted("text_1")
        store.markTextCompleted("old_text")
        store.updateTextProgress(key = "text_1", visible = 7, total = 100)
        store.updateTextProgress(key = "old_text", visible = 25, total = 100)

        store.pruneToKeys(
            timelineItemKeys = setOf("turn_1"),
            nodeKeys = setOf("text_1", "deck_1"),
        )

        assertTrue(store.hasEnteredTimelineItem("turn_1"))
        assertFalse(store.hasEnteredTimelineItem("old_turn"))
        assertTrue(store.hasStartedStructuredNode("deck_1"))
        assertFalse(store.hasStartedStructuredNode("old_deck"))
        assertTrue(store.hasEnteredStructuredNode("deck_1"))
        assertFalse(store.hasEnteredStructuredNode("old_deck"))
        assertTrue(store.hasCompletedText("text_1"))
        assertFalse(store.hasCompletedText("old_text"))
        assertEquals(7, store.textRevealProgress("text_1")?.visibleLength)
        assertEquals(null, store.textRevealProgress("old_text"))
    }

    @Test
    fun textAfterThinkingStartsWithHandoffWhilePreviousThinkingExits() {
        val thinking = thinking("thinking_search")
        val text = text("intro_text")
        val state = listOf(thinking, text).visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )

        assertFalse(thinking.key in state.visibleNodeKeys)
        assertTrue(text.key in state.visibleNodeKeys)
        assertTrue(text.key in state.textHandoffKeys)
    }

    @Test
    fun laterThinkingWaitsUntilPreviousTextCompletes() {
        val text = text("intro_text")
        val thinking = thinking("thinking_decision")
        val nodes = listOf(text, thinking)

        val beforeTextDone = nodes.visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )
        val afterTextDone = nodes.visibleTurnNodeKeys(
            completedTextKeys = setOf(text.key),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )

        assertFalse(thinking.key in beforeTextDone.visibleNodeKeys)
        assertTrue(thinking.key in afterTextDone.visibleNodeKeys)
    }

    @Test
    fun followingTextHidesIntermediateThinkingAndStartsWithHandoff() {
        val intro = text("intro_text")
        val thinking = thinking("thinking_decision")
        val nextText = text("decision_text")
        val nodes = listOf(intro, thinking, nextText)

        val state = nodes.visibleTurnNodeKeys(
            completedTextKeys = setOf(intro.key),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )

        assertFalse(thinking.key in state.visibleNodeKeys)
        assertTrue(nextText.key in state.visibleNodeKeys)
        assertTrue(nextText.key in state.textHandoffKeys)
    }

    @Test
    fun clarificationCardWaitsForQuestionRevealBeforeFollowingNodes() {
        val clarification = clarification("clarify_category")
        val deck = deck("deck_after_clarification")
        val nodes = listOf<ChatUiNode>(clarification, deck)
        val questionKey = "${clarification.key}_question"

        val beforeQuestionDone = nodes.visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )
        val afterQuestionBeforeCardEntered = nodes.visibleTurnNodeKeys(
            completedTextKeys = setOf(questionKey),
            textRevealProgress = mapOf(questionKey to TextRevealProgress(visibleLength = 12, totalLength = 12)),
            enteredStructuredKeys = emptySet(),
        )
        val afterCardEntered = nodes.visibleTurnNodeKeys(
            completedTextKeys = setOf(questionKey),
            textRevealProgress = mapOf(questionKey to TextRevealProgress(visibleLength = 12, totalLength = 12)),
            enteredStructuredKeys = setOf(clarification.key),
        )

        assertTrue(clarification.key in beforeQuestionDone.visibleNodeKeys)
        assertFalse(deck.key in beforeQuestionDone.visibleNodeKeys)
        assertTrue(clarification.key in afterQuestionBeforeCardEntered.visibleNodeKeys)
        assertFalse(deck.key in afterQuestionBeforeCardEntered.visibleNodeKeys)
        assertTrue(deck.key in afterCardEntered.visibleNodeKeys)
    }

    @Test
    fun productDeckCanEnterAfterIntroGateAndFinalCardWaitsForDeckAndTextCompletion() {
        val text = text("intro_text")
        val deck = deck("deck_1")
        val final = finalDecision("final_decision")
        val nodes = listOf<ChatUiNode>(text, deck, final)

        val beforeIntroGate = nodes.visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = mapOf(text.key to TextRevealProgress(visibleLength = 4, totalLength = 100)),
            enteredStructuredKeys = emptySet(),
        )
        val afterIntroGate = nodes.visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = mapOf(text.key to TextRevealProgress(visibleLength = 24, totalLength = 100)),
            enteredStructuredKeys = emptySet(),
        )
        val afterDeckEnteredBeforeTextDone = nodes.visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = mapOf(text.key to TextRevealProgress(visibleLength = 24, totalLength = 100)),
            enteredStructuredKeys = setOf(deck.key),
        )
        val afterDeckEnteredAndTextDone = nodes.visibleTurnNodeKeys(
            completedTextKeys = setOf(text.key),
            textRevealProgress = mapOf(text.key to TextRevealProgress(visibleLength = 100, totalLength = 100)),
            enteredStructuredKeys = setOf(deck.key),
        )

        assertFalse(deck.key in beforeIntroGate.visibleNodeKeys)
        assertTrue(deck.key in afterIntroGate.visibleNodeKeys)
        assertFalse(final.key in afterIntroGate.visibleNodeKeys)
        assertFalse(final.key in afterDeckEnteredBeforeTextDone.visibleNodeKeys)
        assertTrue(final.key in afterDeckEnteredAndTextDone.visibleNodeKeys)
    }

    @Test
    fun settledTurnShowsFinalCardWithoutReplayingTextAfterNavigationReturn() {
        val text = text("intro_text").copy(done = true)
        val deck = deck("deck_1")
        val final = finalDecision("final_decision")
        val nodes = listOf<ChatUiNode>(text, deck, final)

        val state = nodes.visibleTurnNodeKeys(
            completedTextKeys = nodes.settledTextRevealKeys(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = nodes.settledStructuredNodeKeys(),
        )

        assertTrue(text.key in state.visibleNodeKeys)
        assertTrue(deck.key in state.visibleNodeKeys)
        assertTrue(final.key in state.visibleNodeKeys)
        assertTrue(state.textHandoffKeys.isEmpty())
    }

    @Test
    fun multiProductDeckDoesNotAutoConvergeAfterAllCandidatesAreHandled() {
        val products = listOf(
            product("p1"),
            product("p2"),
            product("p3"),
        )

        assertFalse(
            shouldAutoConvergeProductDeck(
                products = products,
                remainingProducts = products.take(1),
            ),
        )
        assertFalse(
            shouldAutoConvergeProductDeck(
                products = products,
                remainingProducts = emptyList(),
            ),
        )
        assertFalse(
            shouldAutoConvergeProductDeck(
                products = listOf(product("single")),
                remainingProducts = emptyList(),
            ),
        )
    }

    @Test
    fun productCarouselPrefersFirstUnhandledCandidateAfterFeedback() {
        val products = listOf(product("p1"), product("p2"), product("p3"))

        assertEquals(
            1,
            preferredProductCarouselPage(
                products = products,
                swipeState = ProductSwipeState(
                    currentProductId = "p2",
                    swipedProductIds = listOf("p1"),
                ),
            ),
        )
        assertEquals(
            1,
            preferredProductCarouselPage(
                products = products,
                swipeState = ProductSwipeState(
                    currentProductId = "p1",
                    swipedProductIds = listOf("p1"),
                ),
            ),
        )
    }

    @Test
    fun productCarouselOpensCardsAsDetailWhenProductIdExists() {
        assertTrue(
            shouldOpenProductCardAsDetail(
                productId = "p2",
                singleCandidate = false,
                handledProductIds = setOf("p1"),
                deckConverged = false,
            ),
        )
        assertTrue(
            shouldOpenProductCardAsDetail(
                productId = "p1",
                singleCandidate = false,
                handledProductIds = setOf("p1"),
                deckConverged = false,
            ),
        )
        assertTrue(
            shouldOpenProductCardAsDetail(
                productId = "p2",
                singleCandidate = false,
                handledProductIds = setOf("p1"),
                deckConverged = true,
            ),
        )
        assertFalse(
            shouldOpenProductCardAsDetail(
                productId = "",
                singleCandidate = true,
                handledProductIds = emptySet(),
                deckConverged = true,
            ),
        )
    }

    @Test
    fun productDetailBecomesReadOnlyAfterDeckConverges() {
        val deck = deck("deck_1")
        val final = finalDecision("final_decision")

        assertFalse(ChatUiState(nodes = listOf(final, deck)).hasConvergedDecisionForDeck("deck_1"))
        assertTrue(ChatUiState(nodes = listOf(deck, final)).hasConvergedDecisionForDeck("deck_1"))
    }

    @Test
    fun unsettledVisualIgnoresThinkingAndSettlesAfterTextAndStructuredNodesEntered() {
        val text = text("intro_text")
        val thinking = thinking("thinking_decision")
        val deck = deck("deck_1")
        val nodes = listOf<ChatUiNode>(thinking, text, deck)

        assertTrue(
            nodes.hasUnsettledTurnVisual(
                completedTextKeys = emptySet(),
                enteredStructuredKeys = emptySet(),
            ),
        )
        assertFalse(
            nodes.hasUnsettledTurnVisual(
                completedTextKeys = setOf(text.key),
                enteredStructuredKeys = setOf(deck.key),
            ),
        )
    }

    @Test
    fun splitAssistantGroupsForSameTurnStillUseUniqueLazyKeys() {
        val nodes = listOf<ChatUiNode>(
            text("intro_text"),
            UserMessageNode(key = "user_follow_up", content = "再便宜一点"),
            finalDecision("decision_after_follow_up"),
        )

        val assistantKeys = nodes.toTimelineRenderItems()
            .filterIsInstance<AssistantTurnTimelineItem>()
            .map { it.key }

        assertEquals(2, assistantKeys.size)
        assertEquals(assistantKeys.size, assistantKeys.toSet().size)
    }

    @Test
    fun productDeckHistoryCompactsOldTurnWhileNewTurnIsStreaming() {
        val previousDeck = deck("deck_previous", turnId = "turn_1")
        val currentDeck = deck("deck_current", turnId = "turn_2")

        assertTrue(
            shouldCompactProductDeckHistory(
                node = previousDeck,
                deckConverged = false,
                isStreaming = true,
                currentTurnId = "turn_2",
                latestProductDeckKey = currentDeck.key,
            ),
        )
        assertFalse(
            shouldCompactProductDeckHistory(
                node = currentDeck,
                deckConverged = false,
                isStreaming = true,
                currentTurnId = "turn_2",
                latestProductDeckKey = currentDeck.key,
            ),
        )
    }

    @Test
    fun productDeckHistoryKeepsOnlyLatestDeckExpandedAfterStreamingSettles() {
        val previousDeck = deck("deck_previous", turnId = "turn_1")
        val latestDeck = deck("deck_latest", turnId = "turn_2")

        assertTrue(
            shouldCompactProductDeckHistory(
                node = previousDeck,
                deckConverged = false,
                isStreaming = false,
                currentTurnId = null,
                latestProductDeckKey = latestDeck.key,
            ),
        )
        assertFalse(
            shouldCompactProductDeckHistory(
                node = latestDeck,
                deckConverged = false,
                isStreaming = false,
                currentTurnId = null,
                latestProductDeckKey = latestDeck.key,
            ),
        )
    }

    @Test
    fun productDeckHistoryCompactsLatestDeckAfterDecisionAppears() {
        val latestDeck = deck("deck_latest", turnId = "turn_1")

        assertTrue(
            shouldCompactProductDeckHistory(
                node = latestDeck,
                deckConverged = true,
                isStreaming = false,
                currentTurnId = null,
                latestProductDeckKey = latestDeck.key,
            ),
        )
        assertTrue(
            shouldCompactProductDeckHistory(
                node = latestDeck,
                deckConverged = true,
                isStreaming = true,
                currentTurnId = latestDeck.turnId,
                latestProductDeckKey = latestDeck.key,
            ),
        )
    }

    @Test
    fun nativeMarkdownRendererKeepsReadableListTextWithoutAndroidView() {
        val rendered = """
            针对**油性肌肤**，我先按下面几个条件筛了一轮：

            1. **控油清洁**：优先选择清洁力稳定的洁面。
            2. `预算`：控制在 200 元以内。
        """.trimIndent().toNativeMarkdownAnnotatedString()

        assertEquals(
            """
                针对油性肌肤，我先按下面几个条件筛了一轮：
                1. 控油清洁：优先选择清洁力稳定的洁面。
                2. 预算：控制在 200 元以内。
            """.trimIndent(),
            rendered.text,
        )
        assertTrue(rendered.spanStyles.isNotEmpty())
    }

    @Test
    fun nativeMarkdownRendererCachesRenderedAnnotatedStringsAcrossCompositions() {
        val content = """
            1. **控油清洁**：优先选择清洁力稳定的洁面。
            2. `预算`：控制在 200 元以内。
        """.trimIndent()

        NativeMarkdownRenderer.clearForTest()
        val first = NativeMarkdownRenderer.render(content)
        val second = NativeMarkdownRenderer.render(content)

        assertSame(first, second)
    }

    @Test
    fun decisionSummaryDividerUsesAndroidMarkdownRenderer() {
        val content = "---\n\n优先选小米 17 Ultra。"

        assertTrue(content.needsFinalMarkdownRender())
        assertTrue(content.requiresAndroidMarkdownRender())
    }

    @Test
    fun clarificationManualPromptFollowsMissingSlotContext() {
        val category = clarificationManualPromptFor(
            ClarificationPayload(
                question = "你想买哪一类商品？",
                requiredSlots = listOf("category"),
            ),
        )
        val budget = clarificationManualPromptFor(
            ClarificationPayload(
                question = "这类商品价格跨度比较大，你的预算或价位范围大概是多少？",
                requiredSlots = listOf("budget"),
            ),
        )

        assertEquals("直接输入想买的品类", category)
        assertEquals("直接输入预算范围", budget)
    }

    private fun thinking(key: String) = ThinkingNode(
        key = key,
        payload = ThinkingPayload(stage = key, message = "thinking"),
        turnId = "turn_1",
    )

    private fun text(key: String) = AiStreamNode(
        key = key,
        messageId = key,
        content = "这里是一段正在展开的文字",
        done = false,
        turnId = "turn_1",
    )

    private fun clarification(key: String) = ClarificationNode(
        key = key,
        payload = ClarificationPayload(question = "你想买哪一类商品？", requiredSlots = listOf("category")),
        turnId = "turn_1",
    )

    private fun deck(key: String, turnId: String = "turn_1") = ProductDeckNode(
        key = key,
        deckId = key,
        products = listOf(
            ProductCardPayload(product = ProductPayload(productId = "p1", name = "product")),
        ),
        turnId = turnId,
    )

    private fun finalDecision(key: String) = FinalDecisionNode(
        key = key,
        payload = FinalDecisionPayload(summary = "done"),
        turnId = "turn_1",
    )

    private fun product(productId: String) = ProductCardPayload(
        product = ProductPayload(productId = productId, name = productId),
    )
}
