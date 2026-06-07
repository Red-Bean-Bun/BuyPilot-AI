package com.buypilot.feature.chat.state

import com.buypilot.core.model.AgentEventType
import com.buypilot.core.model.AgentPayload
import com.buypilot.core.model.AgentUiEnvelope
import com.buypilot.core.model.CartActionPayload
import com.buypilot.core.model.CartItemPayload
import com.buypilot.core.model.CartSummaryPayload
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.CompareAxisPayload
import com.buypilot.core.model.CompareAxisValuePayload
import com.buypilot.core.model.CompareCardPayload
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.CriteriaPayload
import com.buypilot.core.model.DonePayload
import com.buypilot.core.model.ErrorPayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.DecisionBarrierPayload
import com.buypilot.core.model.PrimaryDirectionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.SearchStrategyPayload
import com.buypilot.core.model.ShoppingStrategyPayload
import com.buypilot.core.model.TextDeltaPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.feature.chat.model.CartActionNode
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.CompareCardNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.ErrorNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.model.UserMessageNode
import com.buypilot.feature.chat.presentation.AssistantTurnTimelineItem
import com.buypilot.feature.chat.presentation.containsNodeKey
import com.buypilot.feature.chat.presentation.toTimelinePresentationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun blankTextDeltaWithoutExistingNodeDoesNotCreateEmptyAssistantBubble() {
        val thinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "understanding", message = "正在理解你的需求"),
            ),
        )

        val state = ChatReducer.reduce(
            thinking,
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "assistant_intro_turn_1",
                payload = TextDeltaPayload(messageId = "assistant_intro_turn_1", delta = "", done = false),
            ),
        )

        assertEquals(1, state.nodes.size)
        assertTrue(state.nodes.single() is ThinkingNode)
        assertEquals(null, state.streamingTextKey)
        assertEquals(0, state.streamingTextLength)
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
    fun repeatedClarificationNodeIdAcrossTurnsCreatesSeparateCards() {
        val firstTurn = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Clarification,
                nodeId = "clarify_missing_slot",
                turnId = "turn_1",
                payload = ClarificationPayload(question = "你想买哪一类商品？"),
            ),
        )

        val secondTurn = ChatReducer.reduce(
            firstTurn,
            envelope(
                event = AgentEventType.Clarification,
                nodeId = "clarify_missing_slot",
                turnId = "turn_2",
                payload = ClarificationPayload(question = "预算大概是多少？"),
            ),
        )

        val nodes = secondTurn.nodes.filterIsInstance<ClarificationNode>()
        assertEquals(2, nodes.size)
        assertEquals(listOf("你想买哪一类商品？", "预算大概是多少？"), nodes.map { it.payload.question })
        assertEquals(listOf("clarify_missing_slot", "clarify_missing_slot_turn_2"), nodes.map { it.key })
    }

    @Test
    fun repeatedFinalDecisionNodeIdAcrossTurnsCreatesSeparateCards() {
        val firstTurn = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_result",
                turnId = "turn_1",
                payload = FinalDecisionPayload(summary = "第一轮结论"),
            ),
        )

        val secondTurn = ChatReducer.reduce(
            firstTurn,
            envelope(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_result",
                turnId = "turn_2",
                payload = FinalDecisionPayload(summary = "第二轮结论"),
            ),
        )

        val nodes = secondTurn.nodes.filterIsInstance<FinalDecisionNode>()
        assertEquals(2, nodes.size)
        assertEquals(listOf("第一轮结论", "第二轮结论"), nodes.map { it.payload.summary })
        assertEquals(listOf("decision_result", "decision_result_turn_2"), nodes.map { it.key })
    }

    @Test
    fun repeatedCartActionNodeIdAcrossTurnsCreatesSeparateReceipts() {
        val firstTurn = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.CartAction,
                nodeId = "cart_action",
                turnId = "turn_1",
                payload = CartActionPayload(action = "add", productId = "p1", status = "success"),
            ),
        )

        val secondTurn = ChatReducer.reduce(
            firstTurn,
            envelope(
                event = AgentEventType.CartAction,
                nodeId = "cart_action",
                turnId = "turn_2",
                payload = CartActionPayload(action = "add", productId = "p2", status = "success"),
            ),
        )

        val nodes = secondTurn.nodes.filterIsInstance<CartActionNode>()
        assertEquals(2, nodes.size)
        assertEquals(listOf("p1", "p2"), nodes.map { it.payload.productId })
        assertEquals(listOf("cart_action", "cart_action_turn_2"), nodes.map { it.key })
    }

    @Test
    fun compareCardCreatesStructuredNodeAndClearsThinking() {
        val thinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_cmp",
                turnId = "turn_cmp",
                payload = ThinkingPayload(stage = "comparing", message = "正在对比候选商品"),
            ),
        )

        val state = ChatReducer.reduce(
            thinking,
            envelope(
                event = AgentEventType.CompareCard,
                nodeId = "compare_turn_cmp",
                turnId = "turn_cmp",
                payload = CompareCardPayload(
                    compareId = "cmp_1",
                    products = listOf(
                        ProductPayload(productId = "p1", name = "商品一", category = "数码电子"),
                        ProductPayload(productId = "p2", name = "商品二", category = "数码电子"),
                    ),
                    axes = listOf(
                        CompareAxisPayload(
                            name = "影像",
                            values = listOf(
                                CompareAxisValuePayload(productId = "p1", score = 86.0),
                                CompareAxisValuePayload(productId = "p2", score = 72.0),
                            ),
                        ),
                    ),
                    winnerProductId = "p1",
                    winnerReason = "更适合拍照",
                ),
            ),
        )

        assertFalse(state.nodes.any { it is ThinkingNode })
        val node = state.nodes.single() as CompareCardNode
        assertEquals("cmp_1", node.payload.compareId)
        assertEquals("p1", node.payload.winnerProductId)
        assertEquals("turn_cmp", node.turnId)
    }

    @Test
    fun compareNarrationTextDeltaStreamsIntoCompareCard() {
        val compareState = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.CompareCard,
                nodeId = "compare_turn_cmp",
                turnId = "turn_cmp",
                payload = CompareCardPayload(
                    compareId = "cmp_1",
                    products = listOf(
                        ProductPayload(productId = "p1", name = "商品一", category = "数码电子"),
                        ProductPayload(productId = "p2", name = "商品二", category = "数码电子"),
                    ),
                ),
            ),
        )

        val streamed = listOf("第一款", "更适合你。").fold(compareState) { acc, delta ->
            ChatReducer.reduce(
                acc,
                envelope(
                    event = AgentEventType.TextDelta,
                    nodeId = "compare_narration_turn_cmp",
                    turnId = "turn_cmp",
                    payload = TextDeltaPayload(
                        messageId = "compare_narration_turn_cmp",
                        delta = delta,
                        done = false,
                    ),
                ),
            )
        }.let { acc ->
            ChatReducer.reduce(
                acc,
                envelope(
                    event = AgentEventType.TextDelta,
                    nodeId = "compare_narration_turn_cmp",
                    turnId = "turn_cmp",
                    payload = TextDeltaPayload(
                        messageId = "compare_narration_turn_cmp",
                        delta = "",
                        done = true,
                    ),
                ),
            )
        }

        val compareNode = streamed.nodes.single() as CompareCardNode
        assertEquals("第一款更适合你。", compareNode.narrationContent)
        assertTrue(compareNode.narrationDone)
        assertFalse(streamed.nodes.any { it is AiStreamNode })
        assertEquals(compareNode.key, streamed.streamingTextKey)
        assertEquals("第一款更适合你。".length, streamed.streamingTextLength)
    }

    @Test
    fun deckBoundCompareCardIsMappedToSourceDeckAndStillRenderableAsTurn() {
        val deckState = listOf(
            product(rank = 1, productId = "p1", turnId = "turn_products"),
            product(rank = 2, productId = "p2", turnId = "turn_products"),
        ).fold(ChatUiState()) { state, event -> ChatReducer.reduce(state, event) }
        val withThinking = ChatReducer.reduce(
            deckState,
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_compare",
                turnId = "turn_cmp",
                payload = ThinkingPayload(stage = "comparing", message = "正在对比候选商品"),
            ),
        )
        val withCompare = ChatReducer.reduce(
            withThinking,
            envelope(
                event = AgentEventType.CompareCard,
                nodeId = "compare_turn_cmp",
                turnId = "turn_cmp",
                payload = CompareCardPayload(
                    compareId = "cmp_1",
                    sourceDeckId = "deck_1",
                    products = listOf(
                        ProductPayload(productId = "p1", name = "商品一", category = "数码电子"),
                        ProductPayload(productId = "p2", name = "商品二", category = "数码电子"),
                    ),
                    axes = listOf(
                        CompareAxisPayload(
                            name = "影像",
                            values = listOf(
                                CompareAxisValuePayload(productId = "p1", score = 86.0),
                                CompareAxisValuePayload(productId = "p2", score = 72.0),
                            ),
                        ),
                    ),
                    winnerProductId = "p1",
                    winnerReason = "更适合拍照",
                ),
            ),
        )
        val state = ChatReducer.reduce(
            withCompare,
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "compare_narration_turn_cmp",
                turnId = "turn_cmp",
                payload = TextDeltaPayload(
                    messageId = "compare_narration_turn_cmp",
                    delta = "这里是后端对比叙述",
                    done = true,
                ),
            ),
        )

        val presentation = state.toTimelinePresentationState()

        assertEquals("cmp_1", presentation.compareCardBySourceDeckId["deck_1"]?.compareId)
        assertTrue(presentation.items.any { item ->
            item is com.buypilot.feature.chat.presentation.AssistantTurnTimelineItem &&
                item.turnId == "turn_cmp"
        })
    }

    @Test
    fun repeatedErrorNodeIdAcrossTurnsCreatesSeparateErrorCards() {
        val firstTurn = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Error,
                nodeId = "stream_error",
                turnId = "turn_1",
                payload = ErrorPayload(code = "FIRST", message = "第一轮错误"),
            ),
        )

        val secondTurn = ChatReducer.reduce(
            firstTurn,
            envelope(
                event = AgentEventType.Error,
                nodeId = "stream_error",
                turnId = "turn_2",
                payload = ErrorPayload(code = "SECOND", message = "第二轮错误"),
            ),
        )

        val nodes = secondTurn.nodes.filterIsInstance<ErrorNode>()
        assertEquals(2, nodes.size)
        assertEquals(listOf("FIRST", "SECOND"), nodes.map { it.code })
        assertEquals(listOf("stream_error", "stream_error_turn_2"), nodes.map { it.key })
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
    fun addUserMessageSupportsImageOnlyInput() {
        val state = ChatReducer.addUserMessage(
            state = ChatUiState(),
            key = "user_image",
            content = "",
            imageUrl = "/uploads/upload_demo.jpg",
        )

        val userNode = state.nodes.single() as UserMessageNode
        assertEquals("", userNode.content)
        assertEquals("/uploads/upload_demo.jpg", userNode.imageUrl)
        assertEquals("/uploads/upload_demo.jpg", state.lastRetryableRequest?.imageUrl)
        assertTrue(state.isStreaming)
    }

    @Test
    fun clarificationKeepsThinkingForUiExitAnimationWhenNoTextDeltaArrived() {
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

        assertEquals(2, state.nodes.size)
        assertTrue(state.nodes.first() is ThinkingNode)
        assertFalse(state.nodes.any { it is AiStreamNode })
        val card = state.nodes.single { it is ClarificationNode } as ClarificationNode
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
    fun repeatedDeckIdAcrossTurnsCreatesSeparateTimelineDecks() {
        val firstTurn = ChatReducer.reduce(
            ChatUiState(),
            product(rank = 1, productId = "p1", turnId = "turn_1"),
        )
        val secondTurn = ChatReducer.reduce(
            firstTurn,
            product(rank = 1, productId = "p2", turnId = "turn_2"),
        )

        val decks = secondTurn.nodes.filterIsInstance<ProductDeckNode>()

        assertEquals(2, decks.size)
        assertEquals(listOf("turn_1", "turn_2"), decks.map { it.turnId })
        assertEquals(listOf("deck_1", "deck_1_turn_2"), decks.map { it.key })
        assertEquals(listOf(listOf("p1"), listOf("p2")), decks.map { deck ->
            deck.products.map { it.product.productId }
        })
    }

    @Test
    fun deckInteractionsUseLatestDeckWhenBackendReusesDeckId() {
        val state = listOf(
            product(rank = 1, productId = "p1", turnId = "turn_1"),
            product(rank = 2, productId = "p2", turnId = "turn_1"),
            product(rank = 1, productId = "p3", turnId = "turn_2"),
            product(rank = 2, productId = "p4", turnId = "turn_2"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }

        val selected = ChatReducer.selectProduct(state, deckId = "deck_1", productId = "p4")
        val swiped = ChatReducer.swipeProduct(
            state = selected,
            deckId = "deck_1",
            productId = "p4",
            feedbackType = "like",
            action = "like",
        )

        assertEquals("p4", selected.productSwipeStates["deck_1"]?.currentProductId)
        assertTrue(swiped.productSwipeStates["deck_1"]?.swipedProductIds.orEmpty().contains("p4"))
    }

    @Test
    fun repeatedDeckIdAcrossTurnsClearsPreviousSwipeAndPendingState() {
        val firstTurn = listOf(
            product(rank = 1, productId = "p1", turnId = "turn_1"),
            product(rank = 2, productId = "p2", turnId = "turn_1"),
        ).fold(ChatUiState()) { acc, envelope -> ChatReducer.reduce(acc, envelope) }
        val swiped = ChatReducer.swipeProduct(
            state = firstTurn.copy(
                awaitingConvergenceDeckIds = setOf("deck_1"),
                latestConvergeableDeckId = "deck_1",
                pendingDecisions = mapOf(
                    "deck_1" to com.buypilot.feature.chat.model.PendingDecision(
                        key = "decision_old",
                        payload = FinalDecisionPayload(summary = "旧缓存结论"),
                        turnId = "turn_1",
                    ),
                ),
            ),
            deckId = "deck_1",
            productId = "p1",
            feedbackType = "like",
            action = "like",
        )

        val secondTurn = ChatReducer.reduce(
            swiped,
            product(rank = 1, productId = "p3", turnId = "turn_2"),
        )

        assertEquals(null, secondTurn.productSwipeStates["deck_1"])
        assertFalse("deck_1" in secondTurn.awaitingConvergenceDeckIds)
        assertEquals(null, secondTurn.latestConvergeableDeckId)
        assertFalse("deck_1" in secondTurn.pendingDecisions)
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
    fun doneKeepsClarificationCardAndPriorThinkingForUiExitAnimationWithoutReducerInjectedText() {
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

        assertFalse(state.nodes.any { it is AiStreamNode })
        assertEquals(2, state.nodes.size)
        assertTrue(state.nodes[0] is ThinkingNode)
        assertTrue(state.nodes[1] is ClarificationNode)
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
    fun scenarioCriteriaCardStoresShoppingStrategyWithoutReducerInjectedText() {
        val payload = CriteriaCardPayload(
            criteria = CriteriaPayload(
                criteriaId = "criteria_scene",
                category = "数码电子",
                summary = "送男朋友生日礼物，对方喜欢电子产品",
            ),
            shoppingStrategy = ShoppingStrategyPayload(
                strategyId = "scene_001",
                sceneType = "gift",
                sceneSummary = "送男朋友礼物",
                userProblem = "用户不确定这个场景下送什么更体面、更不容易踩雷",
                decisionBarrier = DecisionBarrierPayload(
                    barrierType = "fear_wrong_choice",
                    label = "怕送错、怕不够体面",
                    reason = "核心设备容易踩型号偏好",
                    conversionStrategy = "先推荐低偏好依赖的小件",
                ),
                primaryDirection = PrimaryDirectionPayload(
                    title = "低踩雷的黑科技小件",
                    summary = "优先考虑音频配件",
                    why = "有新鲜感，不强依赖具体型号偏好",
                    searchStrategy = SearchStrategyPayload(
                        category = "数码电子",
                        productType = "真无线耳机",
                        useScenario = "日常使用",
                    ),
                    availableInCatalog = true,
                    supportingProductCount = 2,
                ),
                avoidRisks = listOf("不要盲买手机、电脑这类强型号偏好的大件"),
                assumptions = listOf("暂时不知道预算"),
                confidence = "medium",
            ),
        )

        val state = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.CriteriaCard,
                nodeId = "criteria_scene",
                turnId = "turn_scene",
                payload = payload,
            ),
        )

        assertFalse(state.nodes.any { it is AiStreamNode })
        val node = state.nodes.single() as CriteriaNode
        assertEquals("turn_scene", node.turnId)
        assertEquals("gift", node.payload.shoppingStrategy?.sceneType)
        assertEquals("怕送错、怕不够体面", node.payload.shoppingStrategy?.decisionBarrier?.label)
        assertEquals("真无线耳机", node.payload.shoppingStrategy?.primaryDirection?.searchStrategy?.productType)

        val presentation = state.toTimelinePresentationState()
        assertTrue(presentation.revealKeys.contains("criteria_scene"))
        assertTrue(
            presentation.items.any {
                it.containsNodeKey("criteria_scene")
            },
        )
    }

    @Test
    fun scenarioCriteriaCardRendersImmediatelyWhenNoProducts() {
        // 0-hit scenario: criteria_card with shopping_strategy is followed by
        // no-match text and done(awaiting_criteria_adjustment), but no ProductDeckNode.
        // The criteria card must render immediately instead of being deferred forever.
        val strategyPayload = CriteriaCardPayload(
            criteria = CriteriaPayload(
                criteriaId = "criteria_no_match",
                category = "美妆护肤",
                summary = "敏感肌防晒，无酒精",
            ),
            shoppingStrategy = ShoppingStrategyPayload(
                strategyId = "scene_no_match",
                sceneType = "sensitive",
                sceneSummary = "敏感肌防晒需求",
                decisionBarrier = DecisionBarrierPayload(label = "怕刺激"),
                primaryDirection = PrimaryDirectionPayload(
                    title = "物理防晒优先",
                    searchStrategy = SearchStrategyPayload(
                        category = "美妆护肤",
                        productType = "防晒霜",
                        useScenario = "日常通勤",
                    ),
                    availableInCatalog = false,
                    supportingProductCount = 0,
                ),
                avoidRisks = listOf("避开酒精、香精"),
            ),
        )
        val presentation = ChatUiState(
            nodes = listOf(
                CriteriaNode(
                    key = "criteria_no_match",
                    payload = strategyPayload,
                    turnId = "turn_no_match",
                ),
                AiStreamNode(
                    key = "no_match_text",
                    messageId = "no_match_text",
                    content = "当前商品库没有符合要求的防晒霜，试试放宽条件？",
                    done = true,
                    turnId = "turn_no_match",
                ),
            ),
        ).toTimelinePresentationState()

        assertTrue(presentation.revealKeys.contains("criteria_no_match"))
        assertTrue(
            presentation.items.any {
                it.containsNodeKey("criteria_no_match")
            },
        )
    }

    @Test
    fun scenarioCriteriaCardRendersAfterProductDeckInSameAssistantTurn() {
        val strategyPayload = CriteriaCardPayload(
            criteria = CriteriaPayload(
                criteriaId = "criteria_scene",
                category = "数码电子",
                summary = "送男朋友生日礼物，对方喜欢电子产品",
            ),
            shoppingStrategy = ShoppingStrategyPayload(
                strategyId = "scene_001",
                sceneType = "gift",
                sceneSummary = "送男朋友礼物",
                decisionBarrier = DecisionBarrierPayload(label = "怕送错、怕不够体面"),
                primaryDirection = PrimaryDirectionPayload(
                    title = "低踩雷的黑科技小件",
                    searchStrategy = SearchStrategyPayload(
                        category = "数码电子",
                        productType = "真无线耳机",
                        useScenario = "日常使用",
                    ),
                    availableInCatalog = true,
                    supportingProductCount = 2,
                ),
                avoidRisks = listOf("不要盲买手机、电脑这类强型号偏好的大件"),
            ),
        )
        val productPayload = ProductCardPayload(
            rank = 1,
            product = ProductPayload(
                productId = "p1",
                name = "真无线耳机",
                category = "数码电子",
            ),
        )
        val presentation = ChatUiState(
            nodes = listOf(
                AiStreamNode(
                    key = "intro",
                    messageId = "intro",
                    content = "先按低踩雷礼物方向找。",
                    done = true,
                    turnId = "turn_scene",
                ),
                CriteriaNode(
                    key = "criteria_scene",
                    payload = strategyPayload,
                    turnId = "turn_scene",
                ),
                ProductDeckNode(
                    key = "deck_scene",
                    deckId = "deck_scene",
                    products = listOf(productPayload),
                    turnId = "turn_scene",
                ),
                AiStreamNode(
                    key = "analysis",
                    messageId = "analysis",
                    content = "这些候选更适合日常送礼。",
                    done = true,
                    turnId = "turn_scene",
                ),
            ),
        ).toTimelinePresentationState()

        val assistantTurn = presentation.items.single() as AssistantTurnTimelineItem
        assertEquals(
            listOf("intro", "deck_scene", "criteria_scene", "analysis"),
            assistantTurn.nodes.map { it.key },
        )
        assertTrue(presentation.revealKeys.contains("criteria_scene"))
    }

    @Test
    fun newCriteriaHidesPreviousTurnCriteriaAndUsesCurrentTurnKey() {
        val payload = CriteriaCardPayload(
            criteria = CriteriaPayload(
                criteriaId = "criteria_1",
                category = "数码电子",
                summary = "智能手机，3000 元以内",
            ),
        )
        val oldState = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.CriteriaCard,
                nodeId = "criteria_1",
                turnId = "turn_old",
                payload = payload,
            ),
        )

        val nextState = ChatReducer.reduce(
            oldState,
            envelope(
                event = AgentEventType.CriteriaCard,
                nodeId = "criteria_1",
                turnId = "turn_new",
                payload = payload,
            ),
        )

        val criteriaNodes = nextState.nodes.filterIsInstance<CriteriaNode>()
        assertEquals(listOf("criteria_1", "criteria_1_turn_new"), criteriaNodes.map { it.key })
        assertEquals("turn_new", criteriaNodes.last().turnId)
        assertEquals(setOf("criteria_1"), nextState.staleCriteriaNodeKeys)

        val presentation = nextState.toTimelinePresentationState()
        assertFalse(presentation.revealKeys.contains("criteria_1"))
        assertTrue(presentation.revealKeys.contains("criteria_1_turn_new"))
    }

    @Test
    fun sameTurnCriteriaUpdateDoesNotMarkCurrentCriteriaStale() {
        val firstPayload = CriteriaCardPayload(
            criteria = CriteriaPayload(
                criteriaId = "criteria_1",
                category = "数码电子",
                summary = "智能手机",
            ),
        )
        val updatedPayload = CriteriaCardPayload(
            criteria = CriteriaPayload(
                criteriaId = "criteria_1",
                category = "数码电子",
                summary = "智能手机，4000 元以内",
            ),
        )
        val firstState = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.CriteriaCard,
                nodeId = "criteria_1",
                turnId = "turn_1",
                payload = firstPayload,
            ),
        )

        val updatedState = ChatReducer.reduce(
            firstState,
            envelope(
                event = AgentEventType.CriteriaCard,
                nodeId = "criteria_1",
                turnId = "turn_1",
                payload = updatedPayload,
            ),
        )

        val criteriaNodes = updatedState.nodes.filterIsInstance<CriteriaNode>()
        assertEquals(1, criteriaNodes.size)
        assertEquals("智能手机，4000 元以内", criteriaNodes.single().payload.criteria.summary)
        assertTrue(updatedState.staleCriteriaNodeKeys.isEmpty())
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
    fun multiProductDeckCachesInitialDecisionBeforeBackendDoneWithoutShowingIt() {
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
                    summary = "当前先把 p1 作为首选候选；继续反馈后我会再收敛。",
                    decisionStatus = "needs_more_signal",
                    confidence = "low",
                    nextStep = "continue_current_deck",
                ),
            ),
        )
        val done = ChatReducer.reduce(
            earlyDecision,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                deckId = "deck_1",
                payload = DonePayload(deckId = "deck_1", finishReason = "awaiting_product_feedback"),
            ),
        )

        assertFalse(earlyDecision.nodes.any { it is FinalDecisionNode })
        assertTrue("deck_1" in earlyDecision.pendingDecisions)
        assertFalse("deck_1" in earlyDecision.awaitingConvergenceDeckIds)
        assertTrue("deck_1" in done.pendingDecisions)
        assertTrue("deck_1" in done.awaitingConvergenceDeckIds)
        assertFalse(done.nodes.any { it is FinalDecisionNode })
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
    fun mismatchedCriteriaIsRemovedWithoutDroppingAssistantText() {
        val withMismatchedCriteria = ChatReducer.reduce(
            ChatUiState(),
            criteria(
                nodeId = "criteria_bad",
                turnId = "turn_category_mismatch",
                category = "品类A",
            ),
        )
        val withAssistantText = ChatReducer.reduce(
            withMismatchedCriteria,
            envelope(
                event = AgentEventType.TextDelta,
                nodeId = "followup_turn_category_mismatch",
                turnId = "turn_category_mismatch",
                payload = TextDeltaPayload(
                    messageId = "followup_turn_category_mismatch",
                    delta = "我先保留这段真实返回的说明文字，后续只根据结构化字段修正错配卡片。",
                    done = true,
                ),
            ),
        )

        val withProduct = ChatReducer.reduce(
            withAssistantText,
            product(rank = 1, productId = "p_category_b_1", turnId = "turn_category_mismatch", category = "品类B"),
        )

        assertTrue(withAssistantText.nodes.any { it is CriteriaNode && it.key == "criteria_bad" })
        assertTrue(withAssistantText.nodes.any { it is AiStreamNode && it.key == "followup_turn_category_mismatch" })
        assertFalse(withProduct.nodes.any { it is CriteriaNode && it.key == "criteria_bad" })
        assertTrue(withProduct.nodes.any { it is AiStreamNode && it.key == "followup_turn_category_mismatch" })
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

    @Test
    fun cartActionSuccessUpdatesCartAndClearsPendingAddProduct() {
        val state = ChatUiState(
            cartState = ChatCartUiState(
                pendingAddProductIds = setOf("p1", "p2"),
            ),
        )

        val next = ChatReducer.reduce(
            state,
            envelope(
                event = AgentEventType.CartAction,
                nodeId = "cart_turn_1",
                payload = CartActionPayload(
                    action = "add",
                    productId = "p1",
                    status = "success",
                    cart = CartSummaryPayload(
                        items = listOf(CartItemPayload(productId = "p1", name = "Phone", quantity = 1, price = 9999.0)),
                        totalItems = 1,
                        totalPrice = 9999.0,
                    ),
                ),
            ),
        )

        assertEquals(setOf("p2"), next.cartState.pendingAddProductIds)
        assertEquals(1, next.cartState.totalItems)
        assertEquals(9999.0, next.cartState.totalPrice, 0.0)
        assertEquals("p1", next.cartState.items.single().productId)
        assertTrue(next.nodes.single() is CartActionNode)
    }

    @Test
    fun failedCartActionClearsPendingAddAndKeepsRetryablePayload() {
        val state = ChatUiState(
            cartState = ChatCartUiState(
                pendingAddProductIds = setOf("p1"),
            ),
        )

        val next = ChatReducer.reduce(
            state,
            envelope(
                event = AgentEventType.CartAction,
                nodeId = "cart_turn_1",
                payload = CartActionPayload(
                    action = "add",
                    productId = "p1",
                    status = "failed",
                    cart = null,
                ),
            ),
        )

        val node = next.nodes.single() as CartActionNode
        assertTrue(next.cartState.pendingAddProductIds.isEmpty())
        assertEquals("没有加成功", next.cartState.error)
        assertEquals("failed", node.payload.status)
        assertEquals("p1", node.payload.productId)
    }

    @Test
    fun checkoutPreviewCreatesCartActionNodeWithCartPreserved() {
        val state = ChatUiState(
            cartState = ChatCartUiState(
                items = listOf(CartItemPayload(productId = "p1", name = "洁面A", price = 52.0, quantity = 1)),
                totalItems = 1,
                totalPrice = 52.0,
            ),
        )

        val next = ChatReducer.reduce(
            state,
            envelope(
                event = AgentEventType.CartAction,
                nodeId = "checkout_preview_turn_1",
                payload = CartActionPayload(
                    action = "checkout_preview",
                    status = "success",
                    cart = CartSummaryPayload(
                        items = listOf(CartItemPayload(productId = "p1", name = "洁面A", price = 52.0, quantity = 1)),
                        totalItems = 1,
                        totalPrice = 52.0,
                    ),
                ),
            ),
        )

        val node = next.nodes.filterIsInstance<CartActionNode>().last()
        assertEquals("checkout_preview", node.payload.action)
        assertEquals(1, next.cartState.totalItems)
        assertEquals(52.0, next.cartState.totalPrice, 0.01)
    }

    @Test
    fun checkoutConfirmKeepsCartWhenBackendReturnsItems() {
        val state = ChatUiState(
            cartState = ChatCartUiState(
                items = listOf(CartItemPayload(productId = "p1", name = "洁面A", quantity = 1)),
                totalItems = 1,
                totalPrice = 52.0,
            ),
        )

        val next = ChatReducer.reduce(
            state,
            envelope(
                event = AgentEventType.CartAction,
                nodeId = "checkout_confirm_turn_1",
                payload = CartActionPayload(
                    action = "checkout_confirm",
                    status = "success",
                    cart = CartSummaryPayload(
                        items = listOf(CartItemPayload(productId = "p1", name = "洁面A", quantity = 1)),
                        totalItems = 1,
                        totalPrice = 52.0,
                    ),
                ),
            ),
        )

        val node = next.nodes.filterIsInstance<CartActionNode>().last()
        assertEquals("checkout_confirm", node.payload.action)
        assertEquals(1, next.cartState.totalItems)
    }

    @Test
    fun checkoutCancelPreservesCart() {
        val state = ChatUiState(
            cartState = ChatCartUiState(
                items = listOf(CartItemPayload(productId = "p1", name = "洁面A", quantity = 2)),
                totalItems = 2,
                totalPrice = 104.0,
            ),
        )

        val next = ChatReducer.reduce(
            state,
            envelope(
                event = AgentEventType.CartAction,
                nodeId = "checkout_cancel_turn_1",
                payload = CartActionPayload(
                    action = "checkout_cancel",
                    status = "success",
                    cart = CartSummaryPayload(
                        items = listOf(CartItemPayload(productId = "p1", name = "洁面A", quantity = 2)),
                        totalItems = 2,
                        totalPrice = 104.0,
                    ),
                ),
            ),
        )

        val node = next.nodes.filterIsInstance<CartActionNode>().last()
        assertEquals("checkout_cancel", node.payload.action)
        assertEquals(2, next.cartState.totalItems)
        assertEquals(104.0, next.cartState.totalPrice, 0.01)
    }

    @Test
    fun checkoutFailedSetsCartError() {
        val next = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.CartAction,
                nodeId = "checkout_failed_turn_1",
                payload = CartActionPayload(
                    action = "checkout_preview",
                    status = "failed",
                ),
            ),
        )

        val node = next.nodes.filterIsInstance<CartActionNode>().last()
        assertEquals("failed", node.payload.status)
        assertNotNull(next.cartState.error)
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
