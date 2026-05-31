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
import com.buypilot.core.model.ErrorPayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.QuickActionPayload
import com.buypilot.core.model.TextDeltaPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.core.model.requests.ChatStreamRequest
import com.buypilot.feature.chat.BuildConfig
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.state.ChatReducer
import com.buypilot.feature.chat.state.ChatInputState
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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private const val FALLBACK_THINKING_MIN_MS = 8_000L
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ChatUiState(
            backendBaseUrl = chatRepository.backendBaseUrl,
            useMockChat = BuildConfig.USE_MOCK_CHAT,
        ),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private val fallbackThinkingStartedAt = mutableMapOf<String, Long>()
    private val pendingFeedbackJobsByDeck = mutableMapOf<String, MutableList<Job>>()
    private val convergenceJobsByDeck = mutableMapOf<String, Job>()
    private var nextSendFromEditResubmit = false

    fun sendMessage(message: String, imageUrl: String? = null) {
        if (message.isBlank() && imageUrl == null) return
        val state = _uiState.value
        val fromEditResubmit = consumeEditResubmitMarker()
        val convergenceDeckId = state.convergenceDeckForMessage(message, imageUrl)
        if (convergenceDeckId != null) {
            convergeProductDeck(convergenceDeckId, userMessage = message)
            return
        }
        if (imageUrl == null && message.isProductConvergenceCommand() && state.hasProductDeckHistory()) {
            return
        }
        if (BuildConfig.USE_MOCK_CHAT) {
            sendMockMessage(message, imageUrl, fromEditResubmit = fromEditResubmit)
            return
        }

        startRealStream(message = message, imageUrl = imageUrl, fromEditResubmit = fromEditResubmit)
    }

    fun retryLastMessage() {
        val request = _uiState.value.lastRetryableRequest ?: return
        val message = request.message.trim()
        if (message.isEmpty()) return

        streamJob?.cancel()
        if (BuildConfig.USE_MOCK_CHAT) {
            sendMockMessage(message, imageUrl = null, retryingLast = true)
        } else {
            startRealStream(message = message, imageUrl = null)
        }
    }

    fun editLastMessage(message: String) {
        if (message.isBlank()) return
        nextSendFromEditResubmit = true
        _uiState.update { ChatReducer.markComposing(it, hasText = true, hasImage = false) }
    }

    fun copyAssistantText(text: String) {
        if (text.isBlank()) return
    }

    fun feedbackAssistant(messageKey: String, feedback: String) {
        if (messageKey.isBlank() || feedback.isBlank()) return
    }

    fun sendCriteriaPatch(criteriaPatch: JsonObject) {
        val patchMessage = "应用并重新推荐"
        if (BuildConfig.USE_MOCK_CHAT) {
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

    private fun consumeEditResubmitMarker(): Boolean {
        val fromEdit = nextSendFromEditResubmit
        nextSendFromEditResubmit = false
        return fromEdit
    }

    private fun startRealStream(
        message: String,
        imageUrl: String? = null,
        criteriaPatch: JsonObject? = null,
        skipStages: List<String> = emptyList(),
        fromEditResubmit: Boolean = false,
        showUserMessage: Boolean = true,
        convergenceDeckId: String? = null,
    ) {
        val nowMs = System.currentTimeMillis()
        val clientTurnId = Ids.clientTurnId()
        val currentSessionId = _uiState.value.sessionId
        var userMessageRecorded = !showUserMessage || currentSessionId != null
        if (showUserMessage) {
            _uiState.update {
                ChatReducer.addUserMessage(
                    state = it,
                    key = Ids.userMessageId(nowMs),
                    content = message,
                    imageUrl = imageUrl,
                    fromEditResubmit = fromEditResubmit,
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    currentTurnId = clientTurnId,
                    inputState = ChatInputState.Streaming,
                    isStreaming = true,
                    lastError = null,
                )
            }
        }
        applyEnvelope(
            localInitialThinkingEnvelope(
                turnId = clientTurnId,
                sessionId = currentSessionId,
                nowMs = nowMs,
            ),
        )

        viewModelScope.launch {
            if (showUserMessage && currentSessionId != null) {
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
            var convergenceProducedDecision = false
            var convergenceFinished = false
            var convergenceFailed = false

            chatRepository.streamChat(request)
                .catch { throwable ->
                    if (!convergenceDeckId.isNullOrBlank() && !convergenceProducedDecision) {
                        convergenceFailed = true
                        return@catch
                    }
                    applyEnvelope(
                        AgentUiEnvelope(
                            event = AgentEventType.Error,
                            sessionId = _uiState.value.sessionId ?: currentSessionId ?: "local_session_001",
                            turnId = clientTurnId,
                            seq = 1,
                            eventId = "$clientTurnId:error",
                            nodeId = "error_$clientTurnId",
                            payload = ErrorPayload(
                                code = "NETWORK_ERROR",
                                message = throwable.message ?: "流式连接失败",
                                retryable = true,
                            ),
                        ),
                    )
                }
                .collect { envelope ->
                    val deckScopedEnvelope = if (!convergenceDeckId.isNullOrBlank()) {
                        envelope.copy(
                            turnId = clientTurnId,
                            deckId = envelope.deckId?.takeIf { it.isNotBlank() } ?: convergenceDeckId,
                        )
                    } else {
                        envelope
                    }
                    if (
                        !convergenceDeckId.isNullOrBlank() &&
                        deckScopedEnvelope.event == AgentEventType.FinalDecision
                    ) {
                        convergenceProducedDecision = true
                    }
                    if (
                        !convergenceDeckId.isNullOrBlank() &&
                        deckScopedEnvelope.event == AgentEventType.Done
                    ) {
                        convergenceFinished = true
                    }
                    val sessionId = deckScopedEnvelope.sessionId
                    if (showUserMessage && !userMessageRecorded && sessionId != null) {
                        userMessageRecorded = true
                        chatRepository.recordUserMessage(
                            sessionId = sessionId,
                            content = message,
                            nowMs = nowMs,
                        )
                    }
                    applyEnvelopeWithFallbackDelay(deckScopedEnvelope)
                }
            if (
                !convergenceDeckId.isNullOrBlank() &&
                (convergenceFinished || convergenceFailed) &&
                !convergenceProducedDecision
            ) {
                sendFallbackConvergence(
                    deckId = convergenceDeckId,
                    userMessage = message,
                    showUserMessage = false,
                    cancelExisting = false,
                )
            }
        }
    }

    fun cancel() {
        val state = _uiState.value
        streamJob?.cancel()
        fallbackThinkingStartedAt.clear()
        _uiState.update(ChatReducer::cancel)

        if (BuildConfig.USE_MOCK_CHAT) return

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

    fun selectProduct(deckId: String, productId: String?) {
        // 单次原子更新：只改 currentProductId，不触发 renderContext 重建
        _uiState.update { ChatReducer.selectProduct(it, deckId, productId) }
        val targetProductId = productId?.takeIf { it.isNotBlank() } ?: return
        if (!_uiState.value.canSubmitProductFeedback(deckId)) return
        submitProductInteraction(
            sessionId = _uiState.value.sessionId,
            deckId = deckId,
            productId = targetProductId,
            feedbackType = "view_detail",
            action = "view_detail",
            reason = "用户打开候选商品预览",
            silentFailure = true,
        )
    }

    fun swipeProduct(
        deckId: String,
        productId: String,
        feedbackType: String,
        action: String,
        reason: String? = null,
    ) {
        if (!_uiState.value.canSubmitProductFeedback(deckId)) return
        val sessionId = _uiState.value.sessionId
        _uiState.update { ChatReducer.swipeProduct(it, deckId, productId, feedbackType, action) }
        submitProductInteraction(sessionId, deckId, productId, feedbackType, action, reason)
    }

    fun undoSwipe(deckId: String) {
        _uiState.update { ChatReducer.undoSwipe(it, deckId) }
    }

    fun openProductEvidence(deckId: String, productId: String) {
        if (!_uiState.value.canSubmitProductFeedback(deckId)) return
        recordProductInteraction(
            deckId = deckId,
            productId = productId,
            feedbackType = "open_evidence",
            action = "open_evidence",
            reason = "用户查看候选商品证据",
        )
    }

    private fun recordProductInteraction(
        deckId: String,
        productId: String,
        feedbackType: String,
        action: String,
        reason: String? = null,
    ) {
        val sessionId = _uiState.value.sessionId
        _uiState.update { ChatReducer.swipeProduct(it, deckId, productId, feedbackType, action) }
        submitProductInteraction(sessionId, deckId, productId, feedbackType, action, reason)
    }

    private fun submitProductInteraction(
        sessionId: String?,
        deckId: String,
        productId: String,
        feedbackType: String,
        action: String,
        reason: String? = null,
        silentFailure: Boolean = false,
    ) {
        if (BuildConfig.USE_MOCK_CHAT || sessionId.isNullOrBlank()) return

        val job = viewModelScope.launch {
            runCatching {
                chatRepository.submitProductFeedback(
                    sessionId = sessionId,
                    deckId = deckId,
                    productId = productId,
                    feedbackType = feedbackType,
                    action = action,
                    reason = reason,
                )
            }.onFailure { throwable ->
                if (!silentFailure) {
                    _uiState.update {
                        it.copy(lastError = throwable.message ?: "商品反馈提交失败")
                    }
                }
            }
        }
        pendingFeedbackJobsByDeck
            .getOrPut(deckId) { mutableListOf() }
            .also { jobs ->
                jobs.removeAll { it.isCompleted || it.isCancelled }
                jobs += job
            }
    }

    fun convergeProductDeck(
        deckId: String,
        userMessage: String = "帮我选",
        showUserMessage: Boolean = false,
        allowFullyHandled: Boolean = false,
    ) {
        val state = _uiState.value
        if (!state.canConvergeLatestProductDeck(deckId, allowFullyHandled = allowFullyHandled)) return

        if (BuildConfig.USE_MOCK_CHAT) {
            sendMockConvergence(deckId, userMessage, showUserMessage)
            return
        }

        convergenceJobsByDeck[deckId]
            ?.takeIf { it.isActive }
            ?.let { return }
        val convergenceJob = viewModelScope.launch {
            awaitPendingFeedbackSubmissions(deckId)
            continueConvergingProductDeck(
                deckId = deckId,
                userMessage = userMessage,
                showUserMessage = showUserMessage,
                allowFullyHandled = allowFullyHandled,
            )
        }
        convergenceJobsByDeck[deckId] = convergenceJob
        convergenceJob.invokeOnCompletion {
            if (convergenceJobsByDeck[deckId] === convergenceJob) {
                convergenceJobsByDeck.remove(deckId)
            }
        }
    }

    private suspend fun awaitPendingFeedbackSubmissions(deckId: String) {
        while (true) {
            val activeJobs = pendingFeedbackJobsByDeck[deckId]
                .orEmpty()
                .filterNot { it.isCompleted || it.isCancelled }
            pendingFeedbackJobsByDeck[deckId]?.removeAll { it.isCompleted || it.isCancelled }
            if (activeJobs.isEmpty()) {
                pendingFeedbackJobsByDeck.remove(deckId)
                return
            }
            activeJobs.joinAll()
        }
    }

    private fun continueConvergingProductDeck(
        deckId: String,
        userMessage: String,
        showUserMessage: Boolean,
        allowFullyHandled: Boolean,
    ) {
        val state = _uiState.value
        if (!state.canConvergeLatestProductDeck(deckId, allowFullyHandled = allowFullyHandled)) return

        if (deckId in state.pendingDecisions && !state.hasProductFeedbackSignals(deckId)) {
            if (showUserMessage) {
                addConvergenceUserMessage(deckId, userMessage)
            }
            _uiState.update { ChatReducer.convergeDeck(it, deckId) }
            return
        }

        _uiState.update {
            ChatReducer.convergeDeck(
                state = it,
                deckId = deckId,
                usePendingDecision = false,
                allowFullyHandled = allowFullyHandled,
            )
        }
        startRealStream(
            message = if (showUserMessage) {
                userMessage.ifBlank { "帮我选" }
            } else {
                "继续"
            },
            showUserMessage = showUserMessage,
            convergenceDeckId = deckId,
        )
    }

    private fun ChatUiState.hasProductFeedbackSignals(deckId: String): Boolean {
        val swipeState = productSwipeStates[deckId] ?: return false
        return swipeState.swipedProductIds.isNotEmpty() ||
            swipeState.undoStack.any { action ->
                action.feedbackType == "like" ||
                    action.feedbackType == "not_interested" ||
                    action.action == "like" ||
                    action.action == "not_interested"
            }
    }

    private fun ChatUiState.canConvergeLatestProductDeck(
        deckId: String,
        allowFullyHandled: Boolean = false,
    ): Boolean {
        val deck = nodes.filterIsInstance<ProductDeckNode>().firstOrNull { it.deckId == deckId }
            ?: return false
        if (deck.products.size < 2 || hasConvergedDecisionForDeck(deckId)) return false
        val isLatestAwaitingDeck = latestConvergeableDeckId == deckId &&
            deckId in awaitingConvergenceDeckIds
        if (isLatestAwaitingDeck) return true
        return allowFullyHandled &&
            isDeckFullyHandled(deckId) &&
            nodes.filterIsInstance<ProductDeckNode>().lastOrNull()?.deckId == deckId
    }

    private fun ChatUiState.canSubmitProductFeedback(deckId: String): Boolean =
        canConvergeLatestProductDeck(deckId) &&
            !isDeckFullyHandled(deckId) &&
            !hasConvergedDecisionForDeck(deckId)

    private fun ChatUiState.hasConvergedDecisionForDeck(deckId: String): Boolean =
        nodes.any { it is FinalDecisionNode && it.deckId == deckId }

    private fun ChatUiState.isDeckFullyHandled(deckId: String): Boolean {
        val products = nodes.filterIsInstance<ProductDeckNode>()
            .firstOrNull { it.deckId == deckId }
            ?.products
            .orEmpty()
            .mapNotNull { it.product.productId.takeIf(String::isNotBlank) }
        if (products.size < 2) return false
        val handledProductIds = productSwipeStates[deckId]?.swipedProductIds.orEmpty().toSet()
        return products.all { it in handledProductIds }
    }

    private fun addConvergenceUserMessage(deckId: String, message: String): Pair<String, String> {
        val nowMs = System.currentTimeMillis()
        val sessionId = _uiState.value.sessionId ?: "local_session_001"
        val turnId = "converge_${deckId}_$nowMs"
        _uiState.update {
            ChatReducer.addUserMessage(
                state = it.copy(sessionId = sessionId, currentTurnId = turnId),
                key = Ids.userMessageId(nowMs),
                content = message.ifBlank { "帮我选" },
            )
        }
        return sessionId to turnId
    }

    private fun beginSilentConvergenceTurn(deckId: String): Pair<String, String> {
        val nowMs = System.currentTimeMillis()
        val sessionId = _uiState.value.sessionId ?: "local_session_001"
        val turnId = "converge_${deckId}_$nowMs"
        _uiState.update {
            it.copy(
                sessionId = sessionId,
                currentTurnId = turnId,
                inputState = ChatInputState.Streaming,
                isStreaming = true,
                lastError = null,
            )
        }
        return sessionId to turnId
    }

    private fun sendMockMessage(
        message: String,
        imageUrl: String?,
        fromEditResubmit: Boolean = false,
        retryingLast: Boolean = false,
    ) {
        val nowMs = System.currentTimeMillis()
        val sessionId = _uiState.value.sessionId ?: "mock_session_001"
        val turnId = "mock_turn_$nowMs"
        val currentNodes = _uiState.value.nodes
        val hasCriteriaContext = currentNodes.any { it is CriteriaNode }

        streamJob?.cancel()
        _uiState.update {
            ChatReducer.addUserMessage(
                state = it.copy(sessionId = sessionId, currentTurnId = turnId),
                key = Ids.userMessageId(nowMs),
                content = message,
                imageUrl = imageUrl,
                fromEditResubmit = fromEditResubmit,
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

            suspend fun emitTextStream(
                nodeId: String,
                messageId: String,
                text: String,
                chunkSize: Int = 12,
                chunkDelayMs: Long = 55L,
            ) {
                val chunks = text.trimIndent().chunked(chunkSize).ifEmpty { listOf("") }
                chunks.forEachIndexed { index, chunk ->
                    emit(
                        event = AgentEventType.TextDelta,
                        nodeId = nodeId,
                        payload = TextDeltaPayload(
                            messageId = messageId,
                            delta = chunk,
                            done = index == chunks.lastIndex,
                        ),
                        displayMode = "inline_text",
                    )
                    if (index != chunks.lastIndex) delay(chunkDelayMs)
                }
            }

            emit(
                event = AgentEventType.Thinking,
                nodeId = "thinking_$turnId",
                payload = ThinkingPayload(stage = "understanding", message = "正在理解您的需求..."),
            )
            delay(700)

            val mockError = if (retryingLast) null else message.toMockErrorPayload(imageUrl)
            if (mockError != null) {
                emit(
                    event = AgentEventType.Error,
                    nodeId = "error_$turnId",
                    payload = mockError,
                    displayMode = "inline_card",
                )
                return@launch
            }

            if (retryingLast) {
                val retryDeckId = "deck_mock_retry_$turnId"
                emitTextStream(
                    nodeId = "assistant_retry_$turnId",
                    messageId = "assistant_retry_$turnId",
                    text = mockRetryMarkdown(message),
                )
                delay(520)
                emit(
                    event = AgentEventType.Thinking,
                    nodeId = "thinking_$turnId",
                    payload = ThinkingPayload(stage = "searching", message = "重试后找到3个匹配商品..."),
                )
                delay(320)
                mockProducts().forEach { product ->
                    emit(
                        event = AgentEventType.ProductCard,
                        nodeId = "product_retry_${product.product.productId}_$turnId",
                        payload = product,
                        deckId = retryDeckId,
                        displayMode = "swipe_deck_item",
                    )
                    delay(220)
                }
                emitDone(
                    turnId = turnId,
                    sessionId = sessionId,
                    seq = seq,
                    deckId = retryDeckId,
                    totalProducts = 3,
                    finishReason = "awaiting_product_feedback",
                )
                return@launch
            }

            if (!hasCriteriaContext && message.needsMockClarification()) {
                emitTextStream(
                    nodeId = "clarification_analysis_$turnId",
                    messageId = "clarification_analysis_$turnId",
                    text = mockClarificationAnalysis(message),
                )
                delay(180)
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

            emit(
                event = AgentEventType.Thinking,
                nodeId = "thinking_$turnId",
                payload = ThinkingPayload(stage = "searching", message = "正在检索匹配商品..."),
            )
            delay(420)

            emitTextStream(
                nodeId = "assistant_intro_$turnId",
                messageId = "assistant_intro_$turnId",
                text = mockRecommendationMarkdown(message, fromEditResubmit),
            )
            delay(550)

            emit(
                event = AgentEventType.Thinking,
                nodeId = "thinking_$turnId",
                payload = ThinkingPayload(stage = "searching", message = "找到3个匹配商品..."),
            )
            delay(360)

            val deckId = "deck_mock_$turnId"
            mockProducts().forEach { product ->
                emit(
                    event = AgentEventType.ProductCard,
                    nodeId = "product_${product.product.productId}_$turnId",
                    payload = product,
                    deckId = deckId,
                    displayMode = "swipe_deck_item",
                )
                delay(260)
            }
            delay(180)
            emit(
                event = AgentEventType.CriteriaCard,
                nodeId = "criteria_$turnId",
                payload = mockCriteria(),
                displayMode = "summary_card",
            )
            emitDone(
                turnId = turnId,
                sessionId = sessionId,
                seq = seq,
                deckId = deckId,
                totalProducts = 3,
                finishReason = "awaiting_product_feedback",
            )
        }
    }

    private fun sendMockConvergence(deckId: String, userMessage: String, showUserMessage: Boolean) {
        val (sessionId, turnId) = if (showUserMessage) {
            addConvergenceUserMessage(deckId, userMessage)
        } else {
            beginSilentConvergenceTurn(deckId)
        }

        streamJob?.cancel()

        streamJob = viewModelScope.launch {
            var seq = 1
            suspend fun emit(
                event: AgentEventType,
                nodeId: String,
                payload: AgentPayload,
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
                    ),
                )
                seq += 1
            }

            emit(
                event = AgentEventType.Thinking,
                nodeId = "thinking_$turnId",
                payload = ThinkingPayload(stage = "decision", message = "正在结合你的选择收敛建议..."),
            )
            delay(520)

            emit(
                event = AgentEventType.TextDelta,
                nodeId = "converge_text_$turnId",
                payload = TextDeltaPayload(
                    messageId = "converge_text_$turnId",
                    delta = "我会优先考虑你感兴趣的候选，同时避开已经排除的商品。",
                    done = true,
                ),
                displayMode = "inline_text",
            )
            delay(360)

            _uiState.update { ChatReducer.convergeDeck(it, deckId) }

            emit(
                event = AgentEventType.FinalDecision,
                nodeId = "decision_mock_001",
                payload = mockDecision(),
                displayMode = "summary_card",
            )
            emitDone(turnId, sessionId, seq, finishReason = "completed")
        }
    }

    private fun sendFallbackConvergence(
        deckId: String,
        userMessage: String,
        showUserMessage: Boolean = false,
        cancelExisting: Boolean = true,
    ) {
        val (sessionId, turnId) = if (showUserMessage) {
            addConvergenceUserMessage(deckId, userMessage)
        } else {
            beginSilentConvergenceTurn(deckId)
        }
        val decision = fallbackConvergenceDecision(deckId)

        if (cancelExisting) streamJob?.cancel()
        streamJob = viewModelScope.launch {
            var seq = 1
            suspend fun emit(
                event: AgentEventType,
                nodeId: String,
                payload: AgentPayload,
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
                    ),
                )
                seq += 1
            }

            emit(
                event = AgentEventType.Thinking,
                nodeId = "thinking_$turnId",
                payload = ThinkingPayload(
                    stage = "fallback_decision",
                    message = "正在用当前候选和你的选择收敛建议...",
                ),
            )

            _uiState.update {
                ChatReducer.convergeDeck(
                    state = it,
                    deckId = deckId,
                    usePendingDecision = false,
                    allowFullyHandled = true,
                )
            }

            emit(
                event = AgentEventType.FinalDecision,
                nodeId = "fallback_decision_$turnId",
                payload = decision,
                displayMode = "summary_card",
            )
            emitDone(turnId, sessionId, seq, finishReason = "completed")
        }
    }

    private fun fallbackConvergenceDecision(deckId: String): FinalDecisionPayload {
        val state = _uiState.value
        val deck = state.nodes.filterIsInstance<ProductDeckNode>().firstOrNull { it.deckId == deckId }
        val products = deck?.products.orEmpty()
        val swipeState = state.productSwipeStates[deckId]
        val undoStack = swipeState?.undoStack.orEmpty()
        val likedProductId = undoStack
            .lastOrNull { it.feedbackType == "like" || it.action == "like" }
            ?.productId
        val dislikedIds = undoStack
            .filter { it.feedbackType == "not_interested" || it.action == "not_interested" }
            .map { it.productId }
            .toSet()
        val winner = products.firstOrNull { it.product.productId == likedProductId }
            ?: products.firstOrNull { it.product.productId !in dislikedIds }
            ?: products.firstOrNull()
        val product = winner?.product
        val productName = product?.name?.takeIf { it.isNotBlank() } ?: "当前 Top 候选商品"
        val price = product?.price?.let { "¥${it.toInt()}" } ?: "价格待确认"
        val tags = (
            product?.skinTypeMatch.orEmpty() +
                product?.ingredientTags.orEmpty() +
                product?.useScenario.orEmpty()
            ).distinct().take(3)

        return FinalDecisionPayload(
            winnerProductId = product?.productId,
            summary = """
                **优先看$productName。**

                我按你刚才对候选的选择做了收敛判断。它在当前候选里最适合作为首选。
            """.trimIndent(),
            why = listOf(
                price,
                "来自当前候选池",
                "结合已看和滑动反馈",
            ) + tags,
            notFor = if (dislikedIds.isNotEmpty()) {
                listOf("已排除的候选不会作为优先建议。")
            } else {
                listOf("如果还没查看候选，建议先点开 1 到 2 个商品再收敛会更稳。")
            },
            alternatives = products
                .filterNot { it.product.productId == product?.productId }
                .take(2)
                .map { AlternativePayload(productId = it.product.productId, name = it.product.name) },
            nextActions = listOf(
                QuickActionPayload(actionId = "cheaper", label = "再便宜一点", action = "criteria_patch"),
                QuickActionPayload(actionId = "similar", label = "换同类推荐", action = "feedback"),
            ),
        )
    }

    private fun emitDone(
        turnId: String,
        sessionId: String,
        seq: Int,
        deckId: String = "deck_mock_001",
        totalProducts: Int = 3,
        finishReason: String = "completed",
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
                        deckId = deckId,
                        totalProducts = totalProducts,
                        clientTurnId = turnId,
                        finishReason = finishReason,
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

    private fun localInitialThinkingEnvelope(
        turnId: String,
        sessionId: String?,
        nowMs: Long,
    ): AgentUiEnvelope<ThinkingPayload> =
        AgentUiEnvelope(
            event = AgentEventType.Thinking,
            sessionId = sessionId ?: _uiState.value.sessionId ?: "local_session_001",
            turnId = turnId,
            seq = 0,
            eventId = "$turnId:local-thinking",
            nodeId = "local_thinking_$turnId",
            createdAtMs = nowMs,
            payload = ThinkingPayload(
                stage = "understanding",
                message = "正在理解你的需求...",
            ),
        )

    private fun ChatUiState.convergenceDeckForMessage(message: String, imageUrl: String?): String? {
        if (imageUrl != null || !message.isProductConvergenceCommand()) return null
        return latestConvergeableDeckId
            ?.takeIf { canConvergeLatestProductDeck(it, allowFullyHandled = true) }
            ?: nodes.filterIsInstance<ProductDeckNode>()
                .lastOrNull()
                ?.deckId
                ?.takeIf { canConvergeLatestProductDeck(it, allowFullyHandled = true) }
    }

    private fun ChatUiState.hasProductDeckHistory(): Boolean =
        nodes.any { it is ProductDeckNode }

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

    private fun String.isProductConvergenceCommand(): Boolean {
        val text = lowercase()
            .trim()
            .replace("，", "")
            .replace("。", "")
            .replace("！", "")
            .replace("!", "")
        if (text.length > 12) return false
        return listOf(
            "继续",
            "收敛",
            "收敛建议",
            "帮我选",
            "给我建议",
            "给建议",
            "总结",
            "就这样",
            "就这个",
            "可以了",
            "好了",
            "没问题",
            "确认",
            "go",
            "ok",
            "continue",
        ).any { it in text }
    }

    private fun String.toMockErrorPayload(imageUrl: String?): ErrorPayload? {
        val text = lowercase()
        return when {
            imageUrl != null && listOf("图片失败", "识别失败", "image error").any { it in text } -> ErrorPayload(
                code = "IMAGE_ANALYSIS_FAILED",
                message = "图片内容没有识别稳定，可能是商品主体太小或画面反光。",
                retryable = true,
            )
            listOf("网络失败", "断网", "network error").any { it in text } -> ErrorPayload(
                code = "NETWORK_ERROR",
                message = "当前网络连接不稳定，商品证据还没有完整拉取。",
                retryable = true,
            )
            listOf("模型失败", "生成失败", "model error").any { it in text } -> ErrorPayload(
                code = "MODEL_ERROR",
                message = "模型这轮没有生成出可用答案，我保留了你的问题，可以直接重试。",
                retryable = true,
            )
            listOf("证据不足", "没有证据", "insufficient evidence").any { it in text } -> ErrorPayload(
                code = "EVIDENCE_INSUFFICIENT",
                message = "当前商品资料不足以支撑可靠推荐，建议补充预算、肤质或排除项。",
                retryable = false,
            )
            listOf("模拟错误", "未知错误", "unknown error").any { it in text } -> ErrorPayload(
                code = "UNKNOWN_ERROR",
                message = "这是一条 mock 错误，用来演示错误卡、编辑问题和重试恢复。",
                retryable = true,
            )
            else -> null
        }
    }

    private fun mockClarificationAnalysis(message: String): String =
        """
            我先看了一下你的需求。

            现在能判断大方向是护肤类推荐，但还缺一个会直接影响筛选的信息。不同肤质对清洁力、刺激风险和预算优先级的判断会不一样，所以我先补齐这个条件，再直接检索候选商品。
        """.trimIndent()

    private fun mockRecommendationMarkdown(message: String, fromEditResubmit: Boolean): String =
        """
            ${if (fromEditResubmit) "已采纳你编辑后的预算和排除项，" else ""}针对**油性肌肤**，我先按下面几个条件筛了一轮：

            1. **控油清洁**：优先选择清洁力稳定、洗后不容易紧绷的洁面。
            2. **预算友好**：控制在 **200 元以内**，超预算商品只作为备选提醒。
            3. **使用风险**：避开高刺激组合，敏感肌会额外提示注意点。

            判断原则：主推荐必须有商品资料或评价证据支撑，不只看营销描述。

            本轮会优先看三件事：预算控制在 200 元以内，肤质偏向油皮/混合肌，证据来自商品资料与评价片段。

            下面是这轮最值得看的候选商品：
        """.trimIndent()

    private fun mockRetryMarkdown(message: String): String =
        """
            已重新生成，这次我按最近文字问题重试，没有复用图片或标准补丁。

            1. **先恢复主结论**：继续优先找油皮可用、预算友好的洁面。
            2. **再压低风险**：含香精或明显超预算的商品只放到备选说明。
            3. **最后绑定证据**：推荐理由会带证据编号，例如 [1] [2]。

            如果你刚才触发的是“模拟错误 / 网络失败”，这条用于演示 Retry 后的成功恢复。

            本次恢复会重新拉取候选、清掉错误残留，并只使用最近这次输入作为重试来源。

            下面给出重试后的候选：
        """.trimIndent()

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
                    productId = "p_beauty_011",
                    name = "珊珂洗颜专科绵润泡沫洁面乳",
                    price = 52.0,
                    currency = "CNY",
                    imageUrl = "/assets/products/1_美妆护肤/images/p_beauty_011_live.jpg",
                    category = "美妆护肤",
                    subCategory = "洁面",
                    brand = "SENKA",
                    skinTypeMatch = listOf("油性", "混合"),
                    ingredientTags = listOf("氨基酸", "绵润泡沫"),
                    useScenario = listOf("日常护肤"),
                ),
                reason = "油皮清洁力强，泡沫绵密不刺激，性价比极高。",
                riskNotes = listOf("含微量香精，极敏感肌需留意。"),
                evidence = listOf(
                    EvidencePayload(
                        evidenceId = "chunk_beauty_011",
                        sourceType = "product_chunk",
                        trustLabel = "商品资料",
                        snippet = "珊珂洗颜专科绵润泡沫洁面乳，油性肌肤适用，氨基酸配方，52元。",
                    ),
                ),
                actions = listOf(
                    QuickActionPayload(actionId = "show_evidence", label = "看证据", action = "open_evidence"),
                    QuickActionPayload(actionId = "add_to_cart", label = "加入购物车", action = "add_to_cart"),
                    QuickActionPayload(actionId = "dislike_product", label = "不喜欢这个", action = "feedback", feedbackType = "not_interested"),
                ),
            ),
            ProductCardPayload(
                rank = 2,
                product = ProductPayload(
                    productId = "p_beauty_006",
                    name = "巴黎欧莱雅新多重防护隔离露",
                    price = 170.0,
                    currency = "CNY",
                    imageUrl = "/assets/products/1_美妆护肤/images/p_beauty_006_live.jpg",
                    category = "美妆护肤",
                    subCategory = "防晒",
                    brand = "L'OREAL PARIS",
                    skinTypeMatch = listOf("油性", "混合"),
                    ingredientTags = listOf("防晒", "多重防护"),
                    useScenario = listOf("日常护肤"),
                ),
                reason = "油皮防晒首选，轻薄不油腻。",
                riskNotes = emptyList(),
                evidence = listOf(
                    EvidencePayload(
                        evidenceId = "chunk_beauty_006",
                        sourceType = "product_chunk",
                        trustLabel = "商品资料",
                        snippet = "巴黎欧莱雅新多重防护隔离露，油性适用，轻薄防晒，170元。",
                    ),
                ),
                actions = listOf(
                    QuickActionPayload(actionId = "show_evidence", label = "看证据", action = "open_evidence"),
                    QuickActionPayload(actionId = "add_to_cart", label = "加入购物车", action = "add_to_cart"),
                    QuickActionPayload(actionId = "dislike_product", label = "不喜欢这个", action = "feedback", feedbackType = "not_interested"),
                ),
            ),
            ProductCardPayload(
                rank = 3,
                product = ProductPayload(
                    productId = "p_beauty_010",
                    name = "安热沙金灿倍护防晒乳",
                    price = 298.0,
                    currency = "CNY",
                    imageUrl = "/assets/products/1_美妆护肤/images/p_beauty_010_live.jpg",
                    category = "美妆护肤",
                    subCategory = "防晒",
                    brand = "ANESSA",
                    skinTypeMatch = listOf("油性", "混合", "敏感"),
                    ingredientTags = listOf("高倍防晒", "金灿防护"),
                    useScenario = listOf("户外"),
                ),
                reason = "防晒标杆产品，防护力最强但略超预算。",
                riskNotes = listOf("价格略超200元预算。"),
                evidence = listOf(
                    EvidencePayload(
                        evidenceId = "chunk_beauty_010",
                        sourceType = "product_chunk",
                        trustLabel = "商品资料",
                        snippet = "安热沙金灿倍护防晒乳，多肤质适用，高倍防晒，298元。",
                    ),
                ),
                actions = listOf(
                    QuickActionPayload(actionId = "show_evidence", label = "看证据", action = "open_evidence"),
                    QuickActionPayload(actionId = "add_to_cart", label = "加入购物车", action = "add_to_cart"),
                    QuickActionPayload(actionId = "dislike_product", label = "不喜欢这个", action = "feedback", feedbackType = "not_interested"),
                ),
            ),
        )

    private fun mockDecision(): FinalDecisionPayload =
        FinalDecisionPayload(
            winnerProductId = "p_beauty_011",
            summary = "如果你更看重控油效果和性价比，优先选珊珂洗颜专科洁面乳。",
            why = listOf("油性适用", "200元内", "控油效果好", "性价比高"),
            notFor = listOf("若你希望避免含香精产品，不建议Top1"),
            alternatives = listOf(
                AlternativePayload(productId = "p_beauty_006", name = "巴黎欧莱雅新多重防护隔离露"),
                AlternativePayload(productId = "p_beauty_010", name = "安热沙金灿倍护防晒乳"),
            ),
            nextActions = listOf(
                QuickActionPayload(actionId = "cheaper", label = "再便宜一点", action = "criteria_patch"),
                QuickActionPayload(actionId = "sensitive_skin", label = "敏感肌适用", action = "criteria_patch"),
                QuickActionPayload(actionId = "compare", label = "加入对比", action = "compare"),
            ),
        )
}
