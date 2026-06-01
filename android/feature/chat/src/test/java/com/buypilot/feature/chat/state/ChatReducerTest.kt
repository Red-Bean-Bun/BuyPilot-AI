package com.buypilot.feature.chat.state

import com.buypilot.core.model.AgentEventType
import com.buypilot.core.model.AgentPayload
import com.buypilot.core.model.AgentUiEnvelope
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.CriteriaPayload
import com.buypilot.core.model.DonePayload
import com.buypilot.core.model.ErrorPayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.TextDeltaPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.ErrorNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ThinkingNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReducerTest {
    @Test
    fun fillsSessionIdFromFirstEventAndUpsertsNode() {
        val first = envelope(
            event = AgentEventType.Thinking,
            nodeId = "thinking_turn_1",
            payload = ThinkingPayload(stage = "understanding", message = "A"),
        )
        val second = first.copy(payload = ThinkingPayload(stage = "searching", message = "B"))

        val state = ChatReducer.reduce(ChatReducer.reduce(ChatUiState(), first), second)

        assertEquals("sess_1", state.sessionId)
        assertEquals(1, state.nodes.size)
        assertEquals("B", (state.nodes.last() as ThinkingNode).payload.message)
    }

    @Test
    fun consecutiveThinkingInSameTurnReplacesUnpairedPreviousThinking() {
        val firstThinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "searching", message = "正在检索匹配商品"),
            ),
        )

        val state = ChatReducer.reduce(
            firstThinking,
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "decision", message = "正在结合你的反馈生成最终建议"),
            ),
        )

        assertEquals(1, state.nodes.filterIsInstance<ThinkingNode>().size)
        assertEquals("正在结合你的反馈生成最终建议", (state.nodes.single() as ThinkingNode).payload.message)
    }

    @Test
    fun appendsTextDeltaIntoSameNode() {
        val state = listOf("hello ", "world").fold(ChatUiState()) { acc, delta ->
            ChatReducer.reduce(
                acc,
                envelope(
                    event = AgentEventType.TextDelta,
                    nodeId = "ignored_node",
                    payload = TextDeltaPayload(messageId = "msg_1", delta = delta),
                ),
            )
        }

        val node = state.nodes.single() as AiStreamNode
        assertEquals("hello world", node.content)
        assertEquals("msg_1", node.messageId)
        assertEquals("msg_1", node.key)
        assertEquals("msg_1", state.streamingTextKey)
        assertEquals("hello world".length, state.streamingTextLength)
    }

    @Test
    fun repeatedMessageIdAcrossTurnsCreatesSeparateTextNodes() {
        val firstTurn = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "assistant_intro_turn_1",
                turnId = "turn_1",
                payload = TextDeltaPayload(messageId = "assistant_intro", delta = "第一轮建议。", done = true),
            ),
        )

        val secondTurn = ChatReducer.reduce(
            firstTurn,
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "assistant_intro_turn_2",
                turnId = "turn_2",
                payload = TextDeltaPayload(messageId = "assistant_intro", delta = "第二轮建议。", done = true),
            ),
        )

        val nodes = secondTurn.nodes.filterIsInstance<AiStreamNode>()
        assertEquals(2, nodes.size)
        assertEquals(listOf("第一轮建议。", "第二轮建议。"), nodes.map { it.content })
        assertEquals(listOf("assistant_intro", "assistant_intro_turn_2"), nodes.map { it.key })
    }

    @Test
    fun doneMarksTurnTextNodesDoneForVisualTypewriterFlush() {
        val withText = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "intro_turn_1",
                payload = TextDeltaPayload(messageId = "intro_turn_1", delta = "hello", done = false),
            ),
        )

        val state = ChatReducer.reduce(
            withText,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                payload = DonePayload(),
            ),
        )

        assertTrue(state.nodes.filterIsInstance<AiStreamNode>().single().done)
        assertFalse(state.isStreaming)
    }

    @Test
    fun doneWithoutTurnIdMarksCurrentStreamingTextDone() {
        val withText = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "standalone_intro",
                turnId = "",
                payload = TextDeltaPayload(delta = "hello", done = false),
            ),
        )

        val state = ChatReducer.reduce(
            withText,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done",
                turnId = "",
                payload = DonePayload(),
            ),
        )

        assertTrue(state.nodes.filterIsInstance<AiStreamNode>().single().done)
        assertFalse(state.isStreaming)
    }

    @Test
    fun blankMessageIdUsesNodeIdAsSeparateTextNodeKey() {
        val first = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "intro_turn_1",
                payload = TextDeltaPayload(delta = "第一段"),
            ),
        )
        val state = ChatReducer.reduce(
            first,
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "followup_turn_1",
                payload = TextDeltaPayload(delta = "第二段"),
            ),
        )

        val nodes = state.nodes.filterIsInstance<AiStreamNode>()
        assertEquals(2, nodes.size)
        assertEquals(listOf("intro_turn_1", "followup_turn_1"), nodes.map { it.key })
        assertEquals(listOf("第一段", "第二段"), nodes.map { it.content })
    }

    @Test
    fun addUserMessageStoresScrollAnchorAndResetsStreamingTextAnchor() {
        val state = ChatReducer.addUserMessage(
            state = ChatUiState(streamingTextKey = "old_msg", streamingTextLength = 12),
            key = "user_1",
            content = "推荐适合油皮的洗面奶",
        )

        assertEquals("user_1", state.lastUserMessageKey)
        assertEquals("推荐适合油皮的洗面奶", state.lastUserMessage)
        assertEquals("推荐适合油皮的洗面奶", state.lastRetryableRequest?.message)
        assertEquals(null, state.lastRetryableRequest?.imageUrl)
        assertFalse(state.lastRetryableRequest?.fromEditResubmit ?: true)
        assertEquals(null, state.streamingTextKey)
        assertEquals(0, state.streamingTextLength)
    }

    @Test
    fun addUserMessageStoresEditedRetryRequest() {
        val state = ChatReducer.addUserMessage(
            state = ChatUiState(),
            key = "user_edited",
            content = "预算降到150，不要香精",
            imageUrl = "mock://image",
            fromEditResubmit = true,
        )

        assertEquals("预算降到150，不要香精", state.lastRetryableRequest?.message)
        assertEquals("mock://image", state.lastRetryableRequest?.imageUrl)
        assertTrue(state.lastRetryableRequest?.fromEditResubmit == true)
    }

    @Test
    fun clarificationClearsThinkingAndShowsOnlyClarificationCardWhenNoTextDeltaArrived() {
        val thinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "understanding", message = "正在理解您的需求"),
            ),
        )

        val state = ChatReducer.reduce(
            thinking,
            envelope(
                event = AgentEventType.Clarification,
                nodeId = "clarify_turn_1",
                payload = ClarificationPayload(question = "请问你的肤质是？"),
            ),
        )

        assertEquals(1, state.nodes.size)
        assertFalse(state.nodes.any { it is ThinkingNode })
        assertFalse(state.nodes.any { it is AiStreamNode })
        val card = state.nodes.single() as ClarificationNode
        assertEquals("clarify_turn_1", card.key)
        assertEquals("", card.anchorMessageKey)
        assertEquals("请问你的肤质是？", card.payload.question)
        assertEquals(null, state.streamingTextKey)
        assertEquals(0, state.streamingTextLength)
    }

    @Test
    fun clarificationKeepsPriorStreamingAnalysisTextFromEventSource() {
        val withText = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "assistant_intro_turn_1",
                payload = TextDeltaPayload(
                    messageId = "assistant_intro_turn_1",
                    delta = "我先问一个关键信息。",
                    done = true,
                ),
            ),
        )

        val state = ChatReducer.reduce(
            withText,
            envelope(
                event = AgentEventType.Clarification,
                nodeId = "clarify_turn_1",
                payload = ClarificationPayload(question = "请问你的肤质是？"),
            ),
        )

        assertEquals(1, state.nodes.filterIsInstance<AiStreamNode>().size)
        assertEquals("我先问一个关键信息。", (state.nodes[0] as AiStreamNode).content)
        assertTrue(state.nodes[1] is ClarificationNode)
    }

    @Test
    fun textDeltaKeepsTransientThinkingForInlineMorphAndAppendsIntoAiStreamNode() {
        val thinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "searching", message = "正在检索匹配商品"),
            ),
        )

        val state = listOf("敏感肌面霜，", "我会先避开酒精和香精。").fold(thinking) { acc, delta ->
            ChatReducer.reduce(
                acc,
                envelope(
                    event = AgentEventType.TextDelta,
                    nodeId = "assistant_intro_turn_1",
                    payload = TextDeltaPayload(messageId = "assistant_intro_turn_1", delta = delta),
                ),
            )
        }

        val node = state.nodes.filterIsInstance<AiStreamNode>().single()
        assertEquals("assistant_intro_turn_1", node.key)
        assertEquals("敏感肌面霜，我会先避开酒精和香精。", node.content)
        assertTrue(state.nodes.any { it is ThinkingNode })
    }

    @Test
    fun laterThinkingKeepsPriorSegmentAndMovesLiveMascotNearActiveWork() {
        val intro = listOf("我先看下候选。").fold(ChatUiState()) { acc, delta ->
            ChatReducer.reduce(
                acc,
                envelope(
                    event = AgentEventType.TextDelta,
                    nodeId = "assistant_intro_turn_1",
                    payload = TextDeltaPayload(messageId = "assistant_intro_turn_1", delta = delta),
                ),
            )
        }
        val firstThinking = ChatReducer.reduce(
            intro,
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "searching", message = "正在检索匹配商品"),
            ),
        )
        val secondText = ChatReducer.reduce(
            firstThinking,
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "assistant_reason_turn_1",
                payload = TextDeltaPayload(messageId = "assistant_reason_turn_1", delta = "这里有三款比较接近。"),
            ),
        )

        val state = ChatReducer.reduce(
            secondText,
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "decision", message = "正在结合反馈"),
            ),
        )

        assertEquals(2, state.nodes.filterIsInstance<ThinkingNode>().size)
        assertTrue(state.nodes.last() is ThinkingNode)
        assertEquals("正在结合反馈", (state.nodes.last() as ThinkingNode).payload.message)
    }

    @Test
    fun doneRemovesTransientThinkingAndClosesStreaming() {
        val thinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "understanding", message = "正在理解您的需求"),
            ),
        )

        val state = ChatReducer.reduce(
            thinking,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                payload = DonePayload(),
            ),
        )

        assertTrue(state.nodes.isEmpty())
        assertFalse(state.isStreaming)
        assertEquals(ChatInputState.Idle, state.inputState)
    }

    @Test
    fun groupsProductCardsByDeckId() {
        val first = product(rank = 2, productId = "p2")
        val second = product(rank = 1, productId = "p1")

        val state = ChatReducer.reduce(ChatReducer.reduce(ChatUiState(), first), second)
        val deck = state.nodes.single() as ProductDeckNode

        assertEquals("deck_1", deck.deckId)
        assertEquals(listOf("p1", "p2"), deck.products.map { it.product.productId })
    }

    @Test
    fun doneAndErrorCloseStreaming() {
        val streaming = ChatUiState(isStreaming = true, inputState = ChatInputState.Streaming)
        val clarifying = ChatUiState(isStreaming = true, inputState = ChatInputState.Clarifying)
        val done = ChatReducer.reduce(
            streaming,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                payload = DonePayload(),
            ),
        )
        val clarificationDone = ChatReducer.reduce(
            clarifying,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                payload = DonePayload(),
            ),
        )
        val error = ChatReducer.reduce(
            streaming,
            envelope(
                event = AgentEventType.Error,
                nodeId = "error_turn_1",
                payload = ErrorPayload(code = "LLM_TIMEOUT", message = "timeout"),
            ),
        )

        assertFalse(done.isStreaming)
        assertEquals(ChatInputState.Idle, done.inputState)
        assertFalse(clarificationDone.isStreaming)
        assertEquals(ChatInputState.Clarifying, clarificationDone.inputState)
        assertFalse(error.isStreaming)
        assertEquals(ChatInputState.Error, error.inputState)
        assertTrue(error.lastError!!.contains("timeout"))
    }

    @Test
    fun errorPayloadAddsTimelineErrorAndKeepsRetryRequest() {
        val withUser = ChatReducer.addUserMessage(
            state = ChatUiState(),
            key = "user_1",
            content = "模拟错误，推荐油皮洁面",
        )

        val state = ChatReducer.reduce(
            withUser,
            envelope(
                event = AgentEventType.Error,
                nodeId = "error_turn_1",
                payload = ErrorPayload(
                    code = "NETWORK_ERROR",
                    message = "network down",
                    retryable = true,
                ),
            ),
        )

        val node = state.nodes.last() as ErrorNode
        assertEquals(ChatInputState.Error, state.inputState)
        assertFalse(state.isStreaming)
        assertEquals("模拟错误，推荐油皮洁面", state.lastRetryableRequest?.message)
        assertEquals("NETWORK_ERROR", node.code)
        assertTrue(node.retryable)
    }

    @Test
    fun clarificationKeepsStreamingUntilDone() {
        val state = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Clarification,
                nodeId = "clarify_turn_1",
                payload = ClarificationPayload(
                    question = "Q",
                    requiredSlots = listOf("skin_type"),
                    suggestedOptions = listOf("油性"),
                ),
            ),
        )

        assertTrue(state.isStreaming)
        assertEquals(ChatInputState.Clarifying, state.inputState)
    }

    @Test
    fun doneKeepsClarificationCardWithoutReducerInjectedText() {
        val thinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "understanding", message = "正在理解您的需求"),
            ),
        )
        val clarifying = ChatReducer.reduce(
            thinking,
            envelope(
                event = AgentEventType.Clarification,
                nodeId = "clarify_turn_1",
                payload = ClarificationPayload(question = "请问你的肤质是？"),
            ),
        )

        val state = ChatReducer.reduce(
            clarifying,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                payload = DonePayload(),
            ),
        )

        assertFalse(state.nodes.any { it is ThinkingNode })
        assertFalse(state.nodes.any { it is AiStreamNode })
        assertEquals(1, state.nodes.size)
        assertTrue(state.nodes.single() is ClarificationNode)
        assertFalse(state.isStreaming)
        assertEquals(ChatInputState.Clarifying, state.inputState)
    }

    @Test
    fun criteriaCardKeepsThinkingForUiExitAnimationAndStoresPayloadWithoutReducerInjectedText() {
        val thinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "criteria", message = "正在生成购买标准"),
            ),
        )
        val payload = CriteriaCardPayload(
            criteria = CriteriaPayload(
                criteriaId = "criteria_1",
                category = "美妆护肤",
                summary = "油皮洁面，200 元以内",
            ),
        )

        val state = ChatReducer.reduce(
            thinking,
            envelope(
                event = AgentEventType.CriteriaCard,
                nodeId = "criteria_1",
                payload = payload,
            ),
        )

        assertFalse(state.nodes.any { it is AiStreamNode })
        assertTrue(state.nodes.first() is ThinkingNode)
        val node = state.nodes.single { it is CriteriaNode } as CriteriaNode
        assertEquals(payload, node.payload)
    }

    @Test
    fun genericUnderstandingThinkingHeartbeatKeepsMascotNodeWithoutUiCopy() {
        val state = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "understanding", message = "正在理解您的需求..."),
            ),
        )

        assertEquals("sess_1", state.sessionId)
        assertTrue(state.isStreaming)
        assertTrue(state.nodes.single() is ThinkingNode)
    }

    @Test
    fun criteriaCardKeepsPriorStreamingAnalysisTextFromEventSource() {
        val withText = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "assistant_intro_turn_1",
                payload = TextDeltaPayload(
                    messageId = "assistant_intro_turn_1",
                    delta = "我会先整理标准。",
                    done = true,
                ),
            ),
        )
        val payload = CriteriaCardPayload(
            criteria = CriteriaPayload(
                criteriaId = "criteria_1",
                category = "美妆护肤",
                summary = "油皮洁面，200 元以内",
            ),
        )

        val state = ChatReducer.reduce(
            withText,
            envelope(
                event = AgentEventType.CriteriaCard,
                nodeId = "criteria_1",
                payload = payload,
            ),
        )

        assertEquals(1, state.nodes.filterIsInstance<AiStreamNode>().size)
        assertEquals("我会先整理标准。", (state.nodes[0] as AiStreamNode).content)
        assertTrue(state.nodes[1] is CriteriaNode)
    }

    @Test
    fun finalDecisionClearsThinkingAndStoresPayloadWithoutTextNode() {
        val thinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "understanding", message = "正在理解您的需求"),
            ),
        )
        val payload = FinalDecisionPayload(winnerProductId = "p1", summary = "首选 p1")

        val state = ChatReducer.reduce(
            thinking,
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_turn_1",
                payload = payload,
            ),
        )

        assertFalse(state.nodes.any { it is AiStreamNode })
        assertFalse(state.nodes.any { it is ThinkingNode })
        val node = state.nodes.single { it is FinalDecisionNode } as FinalDecisionNode
        assertEquals(payload, node.payload)
    }

    @Test
    fun swipeStateSelectsSwipesAndUndoWithoutMutatingDeckProducts() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
            product(rank = 3, productId = "p3"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }

        val selected = ChatReducer.selectProduct(deckState, deckId = "deck_1", productId = "p2")
        val swiped = ChatReducer.swipeProduct(
            state = selected,
            deckId = "deck_1",
            productId = "p2",
            feedbackType = "like",
            action = "like",
        )
        val undone = ChatReducer.undoSwipe(swiped, deckId = "deck_1")

        assertEquals("p2", selected.productSwipeStates["deck_1"]?.currentProductId)
        assertEquals(listOf("p2"), swiped.productSwipeStates["deck_1"]?.swipedProductIds)
        assertEquals("p1", swiped.productSwipeStates["deck_1"]?.currentProductId)
        assertEquals("p2", undone.productSwipeStates["deck_1"]?.currentProductId)
        assertTrue(undone.productSwipeStates["deck_1"]?.swipedProductIds.orEmpty().isEmpty())

        val deck = undone.nodes.single { it is ProductDeckNode } as ProductDeckNode
        assertEquals(listOf("p1", "p2", "p3"), deck.products.map { it.product.productId })
    }

    @Test
    fun productCardsDoNotAwaitConvergenceBeforeBackendDone() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }

        val firstSwipe = ChatReducer.swipeProduct(
            state = deckState,
            deckId = "deck_1",
            productId = "p1",
            feedbackType = "like",
            action = "like",
        )
        val secondSwipe = ChatReducer.swipeProduct(
            state = firstSwipe,
            deckId = "deck_1",
            productId = "p2",
            feedbackType = "not_interested",
            action = "not_interested",
        )

        assertFalse("deck_1" in deckState.awaitingConvergenceDeckIds)
        assertFalse("deck_1" in firstSwipe.awaitingConvergenceDeckIds)
        assertFalse("deck_1" in secondSwipe.awaitingConvergenceDeckIds)
        assertEquals(null, deckState.latestConvergeableDeckId)
        assertFalse("deck_1" in secondSwipe.pendingDecisions)
    }

    @Test
    fun lateFinalDecisionIsIgnoredAfterAllProductsAreSwiped() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val swipedAll = listOf("p1", "p2").fold(deckState) { acc, productId ->
            ChatReducer.swipeProduct(
                state = acc,
                deckId = "deck_1",
                productId = productId,
                feedbackType = if (productId == "p1") "like" else "not_interested",
                action = if (productId == "p1") "like" else "not_interested",
            )
        }

        val lateDecision = ChatReducer.reduce(
            swipedAll,
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_turn_1",
                payload = FinalDecisionPayload(
                    winnerProductId = "p1",
                    summary = "迟到的自动结论不应该展示。",
                    decisionStatus = "needs_more_signal",
                    confidence = "low",
                    nextStep = "continue_current_deck",
                ),
            ),
        )

        assertFalse(lateDecision.nodes.any { it is FinalDecisionNode })
        assertFalse("deck_1" in lateDecision.awaitingConvergenceDeckIds)
        assertFalse("deck_1" in lateDecision.pendingDecisions)
    }

    @Test
    fun deckScopedFinalDecisionFromLaterTurnIsIgnoredAfterAllProductsAreSwiped() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val swipedAll = listOf("p1", "p2").fold(deckState) { acc, productId ->
            ChatReducer.swipeProduct(
                state = acc,
                deckId = "deck_1",
                productId = productId,
                feedbackType = if (productId == "p1") "like" else "not_interested",
                action = if (productId == "p1") "like" else "not_interested",
            )
        }

        val lateDecision = ChatReducer.reduce(
            swipedAll,
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_converge_turn",
                turnId = "converge_turn_2",
                deckId = "deck_1",
                payload = FinalDecisionPayload(
                    winnerProductId = "p1",
                    summary = "另一个 turn 回来的迟到结论也不应该展示。",
                    decisionStatus = "needs_more_signal",
                    confidence = "low",
                    nextStep = "continue_current_deck",
                ),
            ),
        )

        assertFalse(lateDecision.nodes.any { it is FinalDecisionNode })
        assertFalse("deck_1" in lateDecision.awaitingConvergenceDeckIds)
        assertFalse("deck_1" in lateDecision.pendingDecisions)
    }

    @Test
    fun currentDeckScopedFinalDecisionDisplaysAfterAllProductsAreSwiped() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val swipedAll = listOf("p1", "p2").fold(deckState) { acc, productId ->
            ChatReducer.swipeProduct(
                state = acc,
                deckId = "deck_1",
                productId = productId,
                feedbackType = if (productId == "p1") "like" else "not_interested",
                action = if (productId == "p1") "like" else "not_interested",
            )
        }.copy(
            currentTurnId = "converge_turn_2",
            isStreaming = true,
            awaitingConvergenceDeckIds = setOf("deck_1"),
            latestConvergeableDeckId = "deck_1",
            activeConvergenceDeckId = "deck_1",
        )

        val finalDecision = ChatReducer.reduce(
            swipedAll,
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_converge_turn",
                turnId = "converge_turn_2",
                deckId = "deck_1",
                payload = FinalDecisionPayload(
                    winnerProductId = "p1",
                    summary = "滑完后的当前收敛结论应该展示。",
                    decisionStatus = "selected",
                    confidence = "high",
                    nextStep = "completed",
                ),
            ),
        )

        assertTrue(finalDecision.nodes.any { it is FinalDecisionNode })
        assertEquals("deck_1", (finalDecision.nodes.last { it is FinalDecisionNode } as FinalDecisionNode).deckId)
        assertFalse("deck_1" in finalDecision.awaitingConvergenceDeckIds)
        assertFalse("deck_1" in finalDecision.pendingDecisions)
    }

    @Test
    fun fullyHandledDeckCanStartExplicitConvergenceAndIgnoreReturnedCards() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val awaitableDeckState = ChatReducer.reduce(
            deckState,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                deckId = "deck_1",
                payload = DonePayload(deckId = "deck_1", finishReason = "awaiting_product_feedback"),
            ),
        )
        val swipedAll = listOf("p1", "p2").fold(awaitableDeckState) { acc, productId ->
            ChatReducer.swipeProduct(
                state = acc,
                deckId = "deck_1",
                productId = productId,
                feedbackType = if (productId == "p1") "like" else "not_interested",
                action = if (productId == "p1") "like" else "not_interested",
            )
        }

        val converging = ChatReducer.convergeDeck(
            state = swipedAll.copy(currentTurnId = "converge_turn", isStreaming = true),
            deckId = "deck_1",
            usePendingDecision = false,
            allowFullyHandled = true,
        )
        val ignoredProduct = ChatReducer.reduce(
            converging,
            product(rank = 1, productId = "p_new", turnId = "converge_turn"),
        )
        val ignoredCriteria = ChatReducer.reduce(
            ignoredProduct,
            criteria(
                nodeId = "criteria_converge",
                turnId = "converge_turn",
                category = "美妆护肤",
            ),
        )
        val finalDecision = ChatReducer.reduce(
            ignoredCriteria,
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_converge_turn",
                turnId = "converge_turn",
                deckId = "deck_1",
                payload = FinalDecisionPayload(
                    winnerProductId = "p1",
                    summary = "滑完后的收敛结论应该展示。",
                    decisionStatus = "selected",
                    confidence = "high",
                ),
            ),
        )

        assertEquals("deck_1", converging.activeConvergenceDeckId)
        assertFalse(ignoredCriteria.nodes.any { it is CriteriaNode && it.key == "criteria_converge" })
        assertFalse(
            ignoredCriteria.nodes
                .filterIsInstance<ProductDeckNode>()
                .any { deck -> deck.products.any { it.product.productId == "p_new" } },
        )
        assertTrue(finalDecision.nodes.any { it is FinalDecisionNode })
        assertEquals(null, finalDecision.activeConvergenceDeckId)
    }

    @Test
    fun convergenceDoneWithoutDecisionKeepsActiveDeckForContractError() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val converging = ChatReducer.convergeDeck(
            state = deckState.copy(currentTurnId = "converge_turn", isStreaming = true),
            deckId = "deck_1",
            usePendingDecision = false,
            allowFullyHandled = true,
        )

        val done = ChatReducer.reduce(
            converging,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_converge",
                turnId = "converge_turn",
                deckId = "deck_1",
                payload = DonePayload(deckId = "deck_1", finishReason = "awaiting_product_feedback"),
            ),
        )

        assertEquals("deck_1", done.activeConvergenceDeckId)
        assertFalse(done.isStreaming)
    }

    @Test
    fun mismatchedBeautyCriteriaAndFollowupAreRemovedFromDigitalProductTurn() {
        val withBeautyCriteria = ChatReducer.reduce(
            ChatUiState(),
            criteria(
                nodeId = "criteria_bad",
                turnId = "turn_phone",
                category = "美妆护肤",
            ),
        )
        val withBeautyFollowup = ChatReducer.reduce(
            withBeautyCriteria,
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "followup_turn_phone",
                turnId = "turn_phone",
                payload = TextDeltaPayload(
                    messageId = "followup_turn_phone",
                    delta = "你先看看这几款候选。如果想调整筛选范围，可以直接说「再温和一点」「不要酒精」「预算再低一点」，也可以点筛选卡修改。",
                    done = true,
                ),
            ),
        )

        val withDigitalProduct = ChatReducer.reduce(
            withBeautyFollowup,
            product(rank = 1, productId = "p_phone_1", turnId = "turn_phone", category = "数码电子"),
        )

        assertTrue(withBeautyFollowup.nodes.any { it is CriteriaNode && it.key == "criteria_bad" })
        assertTrue(withBeautyFollowup.nodes.any { it is AiStreamNode && it.key == "followup_turn_phone" })
        assertFalse(withDigitalProduct.nodes.any { it is CriteriaNode && it.key == "criteria_bad" })
        assertFalse(withDigitalProduct.nodes.any { it is AiStreamNode && it.key == "followup_turn_phone" })
    }

    @Test
    fun lateDoneDoesNotReopenConvergenceAfterAllProductsAreSwiped() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val swipedAll = listOf("p1", "p2").fold(deckState) { acc, productId ->
            ChatReducer.swipeProduct(
                state = acc,
                deckId = "deck_1",
                productId = productId,
                feedbackType = if (productId == "p1") "like" else "not_interested",
                action = if (productId == "p1") "like" else "not_interested",
            )
        }

        val done = ChatReducer.reduce(
            swipedAll,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                deckId = "deck_1",
                payload = DonePayload(deckId = "deck_1", finishReason = "awaiting_product_feedback"),
            ),
        )

        assertFalse("deck_1" in done.awaitingConvergenceDeckIds)
        assertFalse("deck_1" in done.pendingDecisions)
    }

    @Test
    fun lateProductCardDoesNotReopenConvergenceAfterAllProductsAreSwiped() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val swipedAll = listOf("p1", "p2").fold(deckState) { acc, productId ->
            ChatReducer.swipeProduct(
                state = acc,
                deckId = "deck_1",
                productId = productId,
                feedbackType = if (productId == "p1") "like" else "not_interested",
                action = if (productId == "p1") "like" else "not_interested",
            )
        }

        val lateProduct = ChatReducer.reduce(
            swipedAll,
            product(rank = 2, productId = "p2"),
        )

        assertFalse("deck_1" in lateProduct.awaitingConvergenceDeckIds)
        assertFalse("deck_1" in lateProduct.pendingDecisions)
    }

    @Test
    fun selectProductDoesNotReopenAlreadyHandledProductInSwipeDeck() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
            product(rank = 3, productId = "p3"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val selected = ChatReducer.selectProduct(deckState, deckId = "deck_1", productId = "p1")
        val swiped = ChatReducer.swipeProduct(
            state = selected,
            deckId = "deck_1",
            productId = "p1",
            feedbackType = "like",
            action = "like",
        )

        val reselectedFromOldRoute = ChatReducer.selectProduct(
            state = swiped,
            deckId = "deck_1",
            productId = "p1",
        )

        assertEquals(listOf("p1"), reselectedFromOldRoute.productSwipeStates["deck_1"]?.swipedProductIds)
        assertEquals("p2", reselectedFromOldRoute.productSwipeStates["deck_1"]?.currentProductId)
    }

    @Test
    fun convergeDeckDoesNotShowCachedDecisionAfterAllProductsAreSwiped() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val earlyDecision = ChatReducer.reduce(
            deckState,
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_turn_1",
                payload = FinalDecisionPayload(
                    winnerProductId = "p1",
                    summary = "这个缓存结论不应该在滑完后出现。",
                    decisionStatus = "needs_more_signal",
                    confidence = "low",
                    nextStep = "continue_current_deck",
                ),
            ),
        )
        val swipedAll = listOf("p1", "p2").fold(earlyDecision) { acc, productId ->
            ChatReducer.swipeProduct(
                state = acc,
                deckId = "deck_1",
                productId = productId,
                feedbackType = if (productId == "p1") "like" else "not_interested",
                action = if (productId == "p1") "like" else "not_interested",
            )
        }

        val converged = ChatReducer.convergeDeck(swipedAll, "deck_1")

        assertFalse(converged.nodes.any { it is FinalDecisionNode })
        assertFalse("deck_1" in converged.awaitingConvergenceDeckIds)
        assertFalse("deck_1" in converged.pendingDecisions)
    }

    @Test
    fun multiProductDeckAwaitsConvergenceAndCachesInitialDecisionBeforeDone() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val awaitableDeckState = ChatReducer.reduce(
            deckState,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                deckId = "deck_1",
                payload = DonePayload(deckId = "deck_1", finishReason = "awaiting_product_feedback"),
            ),
        )

        val earlyDecision = ChatReducer.reduce(
            awaitableDeckState,
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_turn_1",
                payload = FinalDecisionPayload(
                    winnerProductId = "p1",
                    summary = "当前先把 p1 作为首选候选；继续反馈后我会再收敛。",
                    decisionStatus = "needs_more_signal",
                    confidence = "low",
                    nextStep = "continue_current_deck",
                ),
            ),
        )
        val converged = ChatReducer.convergeDeck(earlyDecision, "deck_1")

        assertFalse("deck_1" in deckState.awaitingConvergenceDeckIds)
        assertTrue("deck_1" in awaitableDeckState.awaitingConvergenceDeckIds)
        assertEquals("deck_1", awaitableDeckState.latestConvergeableDeckId)
        assertFalse(earlyDecision.nodes.any { it is FinalDecisionNode })
        assertTrue("deck_1" in earlyDecision.pendingDecisions)
        assertFalse("deck_1" in converged.awaitingConvergenceDeckIds)
        assertTrue(converged.nodes.any { it is FinalDecisionNode })
        assertEquals("deck_1", (converged.nodes.last { it is FinalDecisionNode } as FinalDecisionNode).deckId)
    }

    @Test
    fun cachedPendingDecisionCanBePresentedAsNewAssistantTurn() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val awaitableDeckState = ChatReducer.reduce(
            deckState,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                deckId = "deck_1",
                payload = DonePayload(deckId = "deck_1", finishReason = "awaiting_product_feedback"),
            ),
        )
        val earlyDecision = ChatReducer.reduce(
            awaitableDeckState,
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_turn_1",
                payload = FinalDecisionPayload(
                    winnerProductId = "p1",
                    summary = "当前先把 p1 作为首选候选；继续反馈后我会再收敛。",
                    decisionStatus = "needs_more_signal",
                    confidence = "low",
                    nextStep = "continue_current_deck",
                ),
            ),
        )

        val converged = ChatReducer.convergeDeck(
            state = earlyDecision.copy(currentTurnId = "converge_turn_1"),
            deckId = "deck_1",
            presentationTurnId = "converge_turn_1",
        )

        val decision = converged.nodes.last { it is FinalDecisionNode } as FinalDecisionNode
        assertEquals("converge_turn_1", decision.turnId)
        assertEquals("converge_turn_1", converged.currentTurnId)
    }

    @Test
    fun convergenceCanDiscardCachedInitialDecisionWhenFeedbackRequiresBackendRecompute() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val awaitableDeckState = ChatReducer.reduce(
            deckState,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                deckId = "deck_1",
                payload = DonePayload(deckId = "deck_1", finishReason = "awaiting_product_feedback"),
            ),
        )
        val earlyDecision = ChatReducer.reduce(
            awaitableDeckState,
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_turn_1",
                payload = FinalDecisionPayload(
                    winnerProductId = "p1",
                    summary = "当前先把 p1 作为首选候选；继续反馈后我会再收敛。",
                    decisionStatus = "needs_more_signal",
                    confidence = "low",
                    nextStep = "continue_current_deck",
                ),
            ),
        )

        val converging = ChatReducer.convergeDeck(
            earlyDecision,
            deckId = "deck_1",
            usePendingDecision = false,
        )

        assertFalse("deck_1" in converging.awaitingConvergenceDeckIds)
        assertFalse("deck_1" in converging.pendingDecisions)
        assertFalse(converging.nodes.any { it is FinalDecisionNode })
    }

    @Test
    fun doneKeepsMultiProductDeckAwaitingConvergence() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }

        val done = ChatReducer.reduce(
            deckState,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                deckId = "deck_1",
                payload = DonePayload(deckId = "deck_1", finishReason = "awaiting_product_feedback"),
            ),
        )

        assertTrue("deck_1" in done.awaitingConvergenceDeckIds)
        assertEquals("deck_1", done.latestConvergeableDeckId)
    }

    @Test
    fun singleProductDeckDoesNotAwaitConvergenceAfterDone() {
        val deckState = ChatReducer.reduce(
            ChatUiState(),
            product(rank = 1, productId = "p1"),
        )
        val done = ChatReducer.reduce(
            deckState,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                deckId = "deck_1",
                payload = DonePayload(deckId = "deck_1", finishReason = "awaiting_product_feedback"),
            ),
        )

        assertFalse("deck_1" in deckState.awaitingConvergenceDeckIds)
        assertFalse("deck_1" in done.awaitingConvergenceDeckIds)
        assertEquals(null, done.latestConvergeableDeckId)
    }

    @Test
    fun awaitingCriteriaAdjustmentFinishReasonMarksAdjustmentState() {
        val state = ChatReducer.reduce(
            ChatUiState(isStreaming = true, inputState = ChatInputState.Streaming),
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                payload = DonePayload(finishReason = "awaiting_criteria_adjustment"),
            ),
        )

        assertTrue(state.awaitingCriteriaAdjustment)
        assertFalse(state.isStreaming)
        assertEquals(ChatInputState.Idle, state.inputState)
    }

    @Test
    fun newUserMessageClearsCriteriaAdjustmentState() {
        val state = ChatReducer.addUserMessage(
            state = ChatUiState(
                awaitingCriteriaAdjustment = true,
                awaitingConvergenceDeckIds = setOf("deck_1"),
                latestConvergeableDeckId = "deck_1",
            ),
            key = "user_1",
            content = "预算放宽到 500",
        )

        assertFalse(state.awaitingCriteriaAdjustment)
        assertTrue(state.awaitingConvergenceDeckIds.isEmpty())
        assertEquals(null, state.latestConvergeableDeckId)
    }

    @Test
    fun singleProductFinalDecisionDisplaysImmediately() {
        val deckState = ChatReducer.reduce(
            ChatUiState(),
            product(rank = 1, productId = "p1"),
        )

        val state = ChatReducer.reduce(
            deckState,
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_turn_1",
                payload = FinalDecisionPayload(winnerProductId = "p1", summary = "唯一候选适配 p1"),
            ),
        )

        assertTrue(state.nodes.any { it is FinalDecisionNode })
        assertTrue(state.pendingDecisions.isEmpty())
    }

    @Test
    fun viewDetailMarksViewedWithoutAdvancingSwipeDeck() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }

        val viewed = ChatReducer.swipeProduct(
            state = deckState,
            deckId = "deck_1",
            productId = "p1",
            feedbackType = "view_detail",
            action = "view_detail",
        )

        val swipeState = viewed.productSwipeStates["deck_1"]
        assertEquals(listOf("p1"), swipeState?.viewedProductIds)
        assertTrue(swipeState?.swipedProductIds.orEmpty().isEmpty())
        assertEquals("p1", swipeState?.currentProductId)
    }

    @Test
    fun viewingHandledProductDoesNotMoveSwipeDeckBackwards() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
            product(rank = 3, productId = "p3"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val liked = ChatReducer.swipeProduct(
            state = deckState,
            deckId = "deck_1",
            productId = "p1",
            feedbackType = "like",
            action = "like",
        )

        val viewedHandled = ChatReducer.swipeProduct(
            state = liked,
            deckId = "deck_1",
            productId = "p1",
            feedbackType = "view_detail",
            action = "view_detail",
        )

        val swipeState = viewedHandled.productSwipeStates["deck_1"]
        assertEquals(listOf("p1"), swipeState?.swipedProductIds)
        assertEquals(listOf("p1"), swipeState?.viewedProductIds)
        assertEquals("p2", swipeState?.currentProductId)
    }

    @Test
    fun repeatedProductChoiceReplacesPreviousChoiceForSameProduct() {
        val deckState = listOf(
            product(rank = 1, productId = "p1"),
            product(rank = 2, productId = "p2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }

        val liked = ChatReducer.swipeProduct(
            state = deckState,
            deckId = "deck_1",
            productId = "p1",
            feedbackType = "like",
            action = "like",
        )
        val dismissed = ChatReducer.swipeProduct(
            state = liked,
            deckId = "deck_1",
            productId = "p1",
            feedbackType = "not_interested",
            action = "not_interested",
        )

        val swipeState = dismissed.productSwipeStates["deck_1"]
        assertEquals(listOf("p1"), swipeState?.swipedProductIds)
        assertEquals(1, swipeState?.undoStack?.size)
        assertEquals("not_interested", swipeState?.undoStack?.single()?.feedbackType)
    }

    private fun product(
        rank: Int,
        productId: String,
        turnId: String = "turn_1",
        category: String = "",
    ) = envelope(
        event = AgentEventType.ProductCard,
        nodeId = "product_$productId",
        turnId = turnId,
        deckId = "deck_1",
        payload = ProductCardPayload(
            rank = rank,
            product = ProductPayload(productId = productId, name = productId, category = category),
            reason = "reason",
        ),
    )

    private fun criteria(
        nodeId: String,
        turnId: String,
        category: String,
    ) = envelope(
        event = AgentEventType.CriteriaCard,
        nodeId = nodeId,
        turnId = turnId,
        payload = CriteriaCardPayload(
            criteria = CriteriaPayload(
                criteriaId = nodeId,
                category = category,
                summary = category,
            ),
        ),
    )

    private fun envelope(
        event: AgentEventType,
        nodeId: String,
        turnId: String = "turn_1",
        deckId: String? = null,
        payload: AgentPayload,
    ) = AgentUiEnvelope(
        event = event,
        sessionId = "sess_1",
        turnId = turnId,
        seq = 1,
        eventId = "$turnId:1",
        nodeId = nodeId,
        deckId = deckId,
        displayMode = null,
        payload = payload,
    )
}
