package com.buypilot.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buypilot.core.common.id.Ids
import com.buypilot.core.data.ChatRepository
import com.buypilot.core.model.AgentEventType
import com.buypilot.core.model.AgentPayload
import com.buypilot.core.model.AgentUiEnvelope
import com.buypilot.core.model.AlternativePayload
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.CriteriaPayload
import com.buypilot.core.model.DonePayload
import com.buypilot.core.model.EvidencePayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.QuickActionPayload
import com.buypilot.core.model.TextDeltaPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.core.model.requests.ChatStreamRequest
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.state.ChatReducer
import com.buypilot.feature.chat.state.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private const val USE_MOCK_CHAT = true
private const val FALLBACK_THINKING_MIN_MS = 8_000L
private const val CRITERIA_CONFIRMATION_PROMPT = "你可以先确认或修改这些标准。没问题的话回复「继续」，我再开始推荐。"

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private val fallbackThinkingStartedAt = mutableMapOf<String, Long>()

    fun sendMessage(message: String, imageUrl: String? = null) {
        if (message.isBlank() && imageUrl == null) return
        if (USE_MOCK_CHAT) {
            sendMockMessage(message, imageUrl)
            return
        }

        startRealStream(message = message, imageUrl = imageUrl)
    }

    fun sendCriteriaPatch(criteriaPatch: JsonObject) {
        val patchMessage = "保存并重新推荐"
        if (USE_MOCK_CHAT) {
            sendMockMessage(patchMessage, imageUrl = null)
            return
        }
        startRealStream(
            message = patchMessage,
            imageUrl = null,
            criteriaPatch = criteriaPatch,
            skipStages = listOf("recommendation"),
        )
    }

    private fun startRealStream(
        message: String,
        imageUrl: String? = null,
        criteriaPatch: JsonObject? = null,
        skipStages: List<String> = emptyList(),
    ) {
        val nowMs = System.currentTimeMillis()
        val clientTurnId = Ids.clientTurnId()
        val currentSessionId = _uiState.value.sessionId
        var userMessageRecorded = currentSessionId != null
        _uiState.update {
            ChatReducer.addUserMessage(
                state = it,
                key = Ids.userMessageId(nowMs),
                content = message,
                imageUrl = imageUrl,
            )
        }

        viewModelScope.launch {
            if (currentSessionId != null) {
                chatRepository.recordUserMessage(
                    sessionId = currentSessionId,
                    content = message,
                    nowMs = nowMs,
                )
            }
        }

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            val request = ChatStreamRequest(
                message = message,
                sessionId = currentSessionId,
                imageUrl = imageUrl,
                criteriaPatch = criteriaPatch,
                skipStages = skipStages,
                clientTurnId = clientTurnId,
                clientTraceId = Ids.clientTraceId(),
            )

            chatRepository.streamChat(request)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isStreaming = false,
                            lastError = throwable.message ?: "流式连接失败",
                        )
                    }
                }
                .collect { envelope ->
                    val sessionId = envelope.sessionId
                    if (!userMessageRecorded && sessionId != null) {
                        userMessageRecorded = true
                        chatRepository.recordUserMessage(
                            sessionId = sessionId,
                            content = message,
                            nowMs = nowMs,
                        )
                    }
                    applyEnvelopeWithFallbackDelay(envelope)
                }
        }
    }

    fun cancel() {
        val state = _uiState.value
        streamJob?.cancel()
        fallbackThinkingStartedAt.clear()
        _uiState.update(ChatReducer::cancel)

        if (USE_MOCK_CHAT) return

        val sessionId = state.sessionId
        val turnId = state.currentTurnId
        if (sessionId != null && turnId != null) {
            viewModelScope.launch {
                runCatching { chatRepository.cancel(sessionId, turnId) }
            }
        }
    }

    fun onInputChanged(text: String, hasImage: Boolean = false) {
        _uiState.update { ChatReducer.markComposing(it, text.isNotBlank(), hasImage) }
    }

    private fun sendMockMessage(message: String, imageUrl: String?) {
        val nowMs = System.currentTimeMillis()
        val sessionId = _uiState.value.sessionId ?: "mock_session_001"
        val turnId = "mock_turn_$nowMs"
        val currentNodes = _uiState.value.nodes
        val hasCriteriaContext = currentNodes.any { it is CriteriaNode }
        val hasPendingCriteriaConfirmation = hasCriteriaContext &&
            currentNodes.none { it is ProductDeckNode || it is FinalDecisionNode }
        val shouldContinueFromCriteria = hasPendingCriteriaConfirmation && message.isMockContinueCommand()

        streamJob?.cancel()
        _uiState.update {
            ChatReducer.addUserMessage(
                state = it.copy(sessionId = sessionId, currentTurnId = turnId),
                key = Ids.userMessageId(nowMs),
                content = message,
                imageUrl = imageUrl,
            )
        }

        streamJob = viewModelScope.launch {
            var seq = 1
            suspend fun emit(
                event: AgentEventType,
                nodeId: String,
                payload: AgentPayload,
                deckId: String? = null,
                displayMode: String? = null,
            ) {
                applyEnvelopeWithFallbackDelay(
                    AgentUiEnvelope(
                        event = event,
                        sessionId = sessionId,
                        turnId = turnId,
                        seq = seq,
                        eventId = "$turnId:${seq.toString().padStart(4, '0')}",
                        nodeId = nodeId,
                        deckId = deckId,
                        displayMode = displayMode,
                        createdAtMs = System.currentTimeMillis(),
                        payload = payload,
                    )
                )
                seq += 1
            }

            emit(
                event = AgentEventType.Thinking,
                nodeId = "thinking_$turnId",
                payload = ThinkingPayload(stage = "understanding", message = "正在理解您的需求..."),
            )
            delay(700)

            if (!hasCriteriaContext && message.needsMockClarification()) {
                emit(
                    event = AgentEventType.Clarification,
                    nodeId = "clarification_$turnId",
                    payload = ClarificationPayload(
                        question = "请问你的**肤质**是？",
                        requiredSlots = listOf("skin_type"),
                        suggestedOptions = listOf("油性", "干性", "混合性", "敏感性", "中性", "痘痘肌", "干敏肌", "不确定"),
                    ),
                    displayMode = "inline_card",
                )
                emitDone(turnId, sessionId, seq)
                return@launch
            }

            if (!shouldContinueFromCriteria) {
                emit(
                    event = AgentEventType.Thinking,
                    nodeId = "thinking_$turnId",
                    payload = ThinkingPayload(stage = "criteria", message = "正在生成购买标准..."),
                )
                delay(550)

                emit(
                    event = AgentEventType.CriteriaCard,
                    nodeId = "criteria_mock_001",
                    payload = mockCriteria(),
                    displayMode = "summary_card",
                )
                delay(360)

                emit(
                    event = AgentEventType.TextDelta,
                    nodeId = "criteria_confirmation_$turnId",
                    payload = TextDeltaPayload(
                        messageId = "criteria_confirmation_$turnId",
                        delta = CRITERIA_CONFIRMATION_PROMPT,
                        done = true,
                    ),
                )
                emitDone(turnId, sessionId, seq, totalProducts = 0)
                return@launch
            }

            emit(
                event = AgentEventType.Thinking,
                nodeId = "thinking_$turnId",
                payload = ThinkingPayload(stage = "searching", message = "正在检索匹配商品..."),
            )
            delay(420)

            emit(
                event = AgentEventType.TextDelta,
                nodeId = "assistant_intro_$turnId",
                payload = TextDeltaPayload(
                    messageId = "assistant_intro_$turnId",
                    delta = """
                        针对**油性肌肤**，我先按下面几个条件筛了一轮：

                        - **控油清洁**：优先选择清洁力稳定、洗后不容易紧绷的洁面。
                        - **预算友好**：控制在 **200 元以内**。
                        - **使用风险**：避开过度刺激，敏感肌会额外提示注意点。

                        下面是这轮最值得看的候选商品：
                    """.trimIndent(),
                    done = true,
                ),
            )
            delay(550)

            emit(
                event = AgentEventType.Thinking,
                nodeId = "thinking_$turnId",
                payload = ThinkingPayload(stage = "searching", message = "找到3个匹配商品..."),
            )
            delay(360)

            mockProducts().forEach { product ->
                emit(
                    event = AgentEventType.ProductCard,
                    nodeId = "product_${product.product.productId}",
                    payload = product,
                    deckId = "deck_mock_001",
                    displayMode = "swipe_deck_item",
                )
                delay(260)
            }
            delay(650)

            emit(
                event = AgentEventType.Thinking,
                nodeId = "thinking_$turnId",
                payload = ThinkingPayload(stage = "recommending", message = "正在生成推荐解释..."),
            )
            delay(420)

            emit(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_mock_001",
                payload = mockDecision(),
                displayMode = "summary_card",
            )
            emitDone(turnId, sessionId, seq)
        }
    }

    private fun emitDone(
        turnId: String,
        sessionId: String,
        seq: Int,
        totalProducts: Int = 3,
    ) {
        _uiState.update {
            ChatReducer.reduce(
                it,
                AgentUiEnvelope(
                    event = AgentEventType.Done,
                    sessionId = sessionId,
                    turnId = turnId,
                    seq = seq,
                    eventId = "$turnId:${seq.toString().padStart(4, '0')}",
                    nodeId = "done_$turnId",
                    payload = DonePayload(
                        criteriaId = "criteria_mock_001",
                        deckId = "deck_mock_001",
                        totalProducts = totalProducts,
                        clientTurnId = turnId,
                    ),
                ),
            )
        }
    }

    private suspend fun applyEnvelopeWithFallbackDelay(envelope: AgentUiEnvelope<AgentPayload>) {
        val thinking = envelope.payload as? ThinkingPayload
        if (envelope.event == AgentEventType.Thinking && thinking?.isFallbackThinking() == true) {
            fallbackThinkingStartedAt[envelope.turnId] = envelope.createdAtMs ?: System.currentTimeMillis()
            applyEnvelope(envelope)
            return
        }

        val startedAt = fallbackThinkingStartedAt[envelope.turnId]
        if (startedAt != null && envelope.event != AgentEventType.Thinking) {
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < FALLBACK_THINKING_MIN_MS) {
                delay(FALLBACK_THINKING_MIN_MS - elapsed)
            }
            fallbackThinkingStartedAt.remove(envelope.turnId)
        }

        applyEnvelope(envelope)
    }

    private fun applyEnvelope(envelope: AgentUiEnvelope<AgentPayload>) {
        _uiState.update { ChatReducer.reduce(it, envelope) }
    }

    private fun ThinkingPayload.isFallbackThinking(): Boolean {
        if (fallback || isFallback) return true
        val marker = "${stage.lowercase()} ${message.lowercase()}"
        return listOf("fallback", "degraded", "mock", "兜底", "降级", "备用").any { it in marker }
    }

    private fun String.needsMockClarification(): Boolean {
        val text = lowercase()
        return listOf(
            "油",
            "干",
            "混合",
            "敏感",
            "肤质",
            "oil",
            "oily",
            "dry",
            "combination",
            "sensitive",
            "skin",
        ).none { it in text }
    }

    private fun String.isMockContinueCommand(): Boolean {
        val text = lowercase().trim()
        return listOf(
            "继续",
            "可以",
            "确认",
            "没问题",
            "开始推荐",
            "推荐吧",
            "go",
            "ok",
            "continue",
        ).any { it in text }
    }

    private fun mockCriteria(): CriteriaCardPayload =
        CriteriaCardPayload(
            editable = true,
            criteria = CriteriaPayload(
                criteriaId = "criteria_mock_001",
                category = "美妆护肤",
                summary = "**油皮洁面**，预算 **200 元以内**，避开酒精刺激",
                chips = listOf("洁面类", "油性肌肤", "200元以内", "不要含酒精"),
                productType = "洁面类",
                skinType = "油性肌肤",
                budgetMax = 200.0,
                ingredientAvoid = listOf("不要含酒精"),
                useScenario = listOf("日常护肤"),
            ),
            quickActions = listOf(
                QuickActionPayload(actionId = "cheaper", label = "再便宜一点", action = "criteria_patch"),
                QuickActionPayload(actionId = "mild", label = "温和亲肤", action = "criteria_patch"),
                QuickActionPayload(actionId = "large", label = "大容量", action = "criteria_patch"),
                QuickActionPayload(actionId = "brand", label = "注重品牌", action = "criteria_patch"),
            ),
        )

    private fun mockProducts(): List<ProductCardPayload> =
        listOf(
            ProductCardPayload(
                rank = 1,
                product = ProductPayload(
                    productId = "p_mock_001",
                    name = "Effaclar 净肤控油洁面啫喱",
                    price = 168.0,
                    currency = "¥",
                    category = "美妆护肤",
                    subCategory = "洁面",
                    brand = "LA ROCHE-POSAY",
                    skinTypeMatch = listOf("敏感肌可用"),
                    ingredientTags = listOf("控油王者", "温和"),
                    useScenario = listOf("日常洁面"),
                ),
                reason = "**控油能力强**，洗后不容易紧绷，适合预算 **200 元内**的油性肌肤日常洁面。",
                riskNotes = listOf("如果是**极度敏感肌**，建议先做局部测试。"),
                evidence = listOf(
                    EvidencePayload(
                        evidenceId = "EV-MOCK-001",
                        sourceType = "用户评价",
                        trustLabel = "Verified",
                        snippet = "> 质地清透，使用后皮肤水润不紧绷，**控油表现稳定**。",
                    ),
                ),
                actions = listOf(
                    QuickActionPayload(actionId = "show_evidence", label = "看证据", action = "open_evidence"),
                    QuickActionPayload(actionId = "not_interested", label = "不喜欢这个", action = "feedback", feedbackType = "not_interested"),
                ),
            ),
            ProductCardPayload(
                rank = 2,
                product = ProductPayload(
                    productId = "p_mock_002",
                    name = "珊珂洗颜专科绵润泡沫洁面乳",
                    price = 45.0,
                    currency = "¥",
                    category = "美妆护肤",
                    subCategory = "洁面",
                    brand = "SENKA",
                    skinTypeMatch = listOf("油性肌肤适用"),
                    ingredientTags = listOf("清洁力强", "高性价比"),
                    useScenario = listOf("日常护肤"),
                ),
                reason = "**价格低、清洁力强**，适合作为预算优先时的首选。",
                riskNotes = listOf("清洁力偏强，**极敏感肌需谨慎**。"),
                evidence = listOf(
                    EvidencePayload(
                        evidenceId = "EV-MOCK-002",
                        sourceType = "商品资料",
                        trustLabel = "商品资料",
                        snippet = "- 油性肌肤适用\n- 泡沫绵密\n- 价格在 **200 元以内**",
                    ),
                ),
            ),
            ProductCardPayload(
                rank = 3,
                product = ProductPayload(
                    productId = "p_mock_003",
                    name = "温和氨基酸洁面乳",
                    price = 129.0,
                    currency = "¥",
                    category = "美妆护肤",
                    subCategory = "洁面",
                    brand = "AURA CLEANSE",
                    skinTypeMatch = listOf("混合肌适用"),
                    ingredientTags = listOf("氨基酸", "温和"),
                    useScenario = listOf("晨间洁面"),
                ),
                reason = "清洁和**温和度**更均衡，适合不想要过强清洁力的用户。",
                riskNotes = emptyList(),
                evidence = listOf(
                    EvidencePayload(
                        evidenceId = "EV-MOCK-003",
                        sourceType = "官方说明",
                        trustLabel = "成分信息",
                        snippet = "**氨基酸表活体系**，主打温和洁面和日常使用。",
                    ),
                ),
            ),
        )

    private fun mockDecision(): FinalDecisionPayload =
        FinalDecisionPayload(
            winnerProductId = "p_mock_002",
            summary = """
                **最终建议：优先选珊珂洗颜专科绵润泡沫洁面乳。**

                我主要看了三件事：

                1. 是否适合**油性肌肤**的日常清洁。
                2. 价格是否稳定落在 **200 元以内**。
                3. 清洁力和潜在刺激之间是否平衡。

                如果你是**极度敏感肌**，我会建议先局部试用，或者切换到更温和的氨基酸洁面。
            """.trimIndent(),
            why = listOf("**油性肌肤适用**", "**200 元内**", "清洁力强", "极高性价比"),
            notFor = listOf("**极度敏感肌**", "完全不接受香精", "想要修护型面霜而不是洁面"),
            alternatives = listOf(
                AlternativePayload(productId = "p_mock_alt_001", name = "薇诺娜舒敏保湿特护霜"),
            ),
            nextActions = listOf(
                QuickActionPayload(actionId = "cheaper", label = "再便宜一点", action = "criteria_patch"),
                QuickActionPayload(actionId = "sensitive", label = "换成敏感肌适用", action = "criteria_patch"),
                QuickActionPayload(actionId = "similar", label = "换同类推荐", action = "feedback"),
            ),
        )
}
