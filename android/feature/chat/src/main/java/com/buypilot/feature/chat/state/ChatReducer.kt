package com.buypilot.feature.chat.state

import com.buypilot.core.model.AgentEventType
import com.buypilot.core.model.AgentPayload
import com.buypilot.core.model.AgentUiEnvelope
import com.buypilot.core.model.CartActionPayload
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.CriteriaPayload
import com.buypilot.core.model.DonePayload
import com.buypilot.core.model.ErrorPayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.TextDeltaPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.CartActionNode
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.ErrorNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.PendingDecision
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ProductSwipeAction
import com.buypilot.feature.chat.model.ProductSwipeState
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.model.UserMessageNode

object ChatReducer {
    fun addUserMessage(
        state: ChatUiState,
        key: String,
        content: String,
        imageUrl: String? = null,
        fromEditResubmit: Boolean = false,
    ): ChatUiState =
        state.copy(
            nodes = state.nodes + UserMessageNode(
                key = key,
                content = content,
                imageUrl = imageUrl,
            ),
            inputState = ChatInputState.Streaming,
            isStreaming = true,
            lastError = null,
            lastUserMessage = content,
            lastUserMessageKey = key,
            lastRetryableRequest = ChatRetryRequest(
                message = content,
                imageUrl = imageUrl,
                fromEditResubmit = fromEditResubmit,
            ),
            streamingTextKey = null,
            streamingTextLength = 0,
            awaitingCriteriaAdjustment = false,
            awaitingConvergenceDeckIds = emptySet(),
            latestConvergeableDeckId = null,
            activeConvergenceDeckId = null,
            pendingDecisions = emptyMap(),
        )

    fun reduce(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val base = state.copy(
            sessionId = state.sessionId ?: envelope.sessionId,
            currentTurnId = envelope.turnId,
        )
        val contentBase = if (envelope.event.shouldClearTransientThinking(base, envelope.turnId)) {
            base.withoutThinking(envelope.turnId)
        } else {
            base
        }

        return when (envelope.event) {
            AgentEventType.Thinking -> {
                val payload = envelope.payload as ThinkingPayload
                if (payload.fallback || payload.isFallback) {
                    return base.copy(
                        inputState = ChatInputState.Streaming,
                        isStreaming = true,
                    )
                }
                base.upsertThinkingAtTail(
                    ThinkingNode(
                        key = payload.thinkingNodeKey(envelope),
                        payload = payload,
                        turnId = envelope.turnId,
                    ),
                ).copy(
                    inputState = ChatInputState.Streaming,
                    isStreaming = true,
                )
            }

            AgentEventType.Clarification -> reduceClarification(contentBase, envelope)

            AgentEventType.CriteriaCard -> reduceCriteriaCard(contentBase, envelope)

            AgentEventType.TextDelta -> reduceTextDelta(base, envelope)

            AgentEventType.ProductCard -> reduceProductCard(contentBase, envelope)

            AgentEventType.CartAction -> {
                val payload = envelope.payload as CartActionPayload
                val rawNodeKey = envelope.nodeId.ifBlank { "cart_action_${envelope.turnId}" }
                val existing = contentBase.nodes.filterIsInstance<CartActionNode>()
                    .lastOrNull {
                        it.turnId == envelope.turnId &&
                            it.key.matchesRawOrTurnScopedNodeKey(rawNodeKey, envelope.turnId)
                    }
                val nodeKey = existing?.key ?: contentBase.uniqueNodeKeyForTurn(rawNodeKey, envelope.turnId)
                val nextCart = payload.cart
                val pendingAddProductIds = payload.productId
                    .takeIf { it.isNotBlank() }
                    ?.let { contentBase.cartState.pendingAddProductIds - it }
                    ?: contentBase.cartState.pendingAddProductIds
                contentBase.upsertNode(
                    CartActionNode(
                        key = nodeKey,
                        payload = payload,
                        turnId = envelope.turnId,
                    ),
                ).let { nextState ->
                    if (nextCart == null) {
                        nextState.copy(
                            cartState = nextState.cartState.copy(
                                error = if (payload.status == "failed") {
                                    "没有加成功"
                                } else {
                                    nextState.cartState.error
                                },
                                pendingAddProductIds = pendingAddProductIds,
                            ),
                        )
                    } else {
                        nextState.copy(
                            cartState = nextState.cartState.copy(
                                items = nextCart.items,
                                totalItems = nextCart.totalItems,
                                totalPrice = nextCart.totalPrice,
                                isLoading = false,
                                error = null,
                                updatingProductIds = emptySet(),
                                pendingAddProductIds = pendingAddProductIds,
                            ),
                        )
                    }
                }
            }

            AgentEventType.FinalDecision -> reduceFinalDecision(contentBase, envelope)

            AgentEventType.Done -> {
                val payload = envelope.payload as DonePayload
                val awaitingDeckId = payload.deckId?.takeIf { it.isNotBlank() }
                val finalizedContentBase = contentBase.markTurnTextDone(envelope.turnId)
                    .markStreamingTextDoneWhenTurnMissing(envelope.turnId)
                val doneBase = if (
                    payload.finishReason == "awaiting_product_feedback" &&
                    awaitingDeckId != null &&
                    finalizedContentBase.productCountForDeck(awaitingDeckId) >= 2 &&
                    !finalizedContentBase.isDeckFullyHandled(awaitingDeckId)
                ) {
                    finalizedContentBase.markLatestDeckAwaitingConvergence(awaitingDeckId)
                } else {
                    finalizedContentBase
                }
                doneBase.copy(
                    inputState = if (payload.finishReason == "canceled" || payload.finishReason == "cancelled") {
                        ChatInputState.Canceled
                    } else if (finalizedContentBase.inputState == ChatInputState.Clarifying ||
                        finalizedContentBase.inputState == ChatInputState.Error ||
                        finalizedContentBase.inputState == ChatInputState.Canceled
                    ) {
                        finalizedContentBase.inputState
                    } else {
                        ChatInputState.Idle
                    },
                    isStreaming = false,
                    awaitingCriteriaAdjustment = payload.finishReason == "awaiting_criteria_adjustment",
                    activeConvergenceDeckId = if (
                        doneBase.activeConvergenceDeckId != null &&
                        doneBase.nodes.none {
                            it is FinalDecisionNode && it.deckId == doneBase.activeConvergenceDeckId
                        }
                    ) {
                        doneBase.activeConvergenceDeckId
                    } else {
                        null
                    },
                )
            }

            AgentEventType.Error -> {
                val payload = envelope.payload as ErrorPayload
                val rawNodeKey = envelope.nodeId.ifBlank { "error_${envelope.turnId}" }
                val existing = contentBase.nodes.filterIsInstance<ErrorNode>()
                    .lastOrNull {
                        it.turnId == envelope.turnId &&
                            it.key.matchesRawOrTurnScopedNodeKey(rawNodeKey, envelope.turnId)
                    }
                val nodeKey = existing?.key ?: contentBase.uniqueNodeKeyForTurn(rawNodeKey, envelope.turnId)
                contentBase.upsertNode(
                    ErrorNode(
                        key = nodeKey,
                        code = payload.code,
                        message = payload.message,
                        retryable = payload.retryable,
                        turnId = envelope.turnId,
                    ),
                ).copy(
                    inputState = ChatInputState.Error,
                    isStreaming = false,
                    lastError = payload.message,
                )
            }

            AgentEventType.Unknown -> base
        }
    }

    fun cancel(state: ChatUiState): ChatUiState {
        val base = state.currentTurnId?.let { state.withoutThinking(it) } ?: state
        return base.copy(
            inputState = ChatInputState.Canceled,
            isStreaming = false,
            cartState = base.cartState.copy(pendingAddProductIds = emptySet()),
        )
    }

    fun markComposing(state: ChatUiState, hasText: Boolean, hasImage: Boolean): ChatUiState =
        state.copy(
            inputState = when {
                hasImage -> ChatInputState.ImageAttached
                hasText -> ChatInputState.Composing
                else -> ChatInputState.Idle
            },
        )

    fun selectProduct(
        state: ChatUiState,
        deckId: String,
        productId: String?,
    ): ChatUiState {
        val products = state.productIdsForDeck(deckId)
        val swipeState = state.productSwipeStates[deckId].orEmpty()
        val handledProductIds = swipeState.swipedProductIds.toSet()
        val selectedProductId = productId
            ?.takeIf { it in products && it !in handledProductIds }
            ?: products.firstOrNull { it !in handledProductIds }
            ?: productId?.takeIf { it in products }
            ?: products.firstOrNull()
        return state.copy(
            productSwipeStates = state.productSwipeStates + (
                deckId to swipeState.copy(currentProductId = selectedProductId)
                ),
        )
    }

    fun swipeProduct(
        state: ChatUiState,
        deckId: String,
        productId: String,
        feedbackType: String,
        action: String,
    ): ChatUiState {
        val products = state.productIdsForDeck(deckId)
        if (productId !in products) return state

        val swipeState = state.productSwipeStates[deckId].orEmpty()
        if (feedbackType == "view_detail" || action == "view_detail" || action == "open_evidence") {
            val handledProductIds = swipeState.swipedProductIds.toSet()
            val nextCurrentProductId = if (productId in handledProductIds) {
                swipeState.currentProductId
                    ?.takeIf { it in products && it !in handledProductIds }
                    ?: products.firstOrNull { it !in handledProductIds }
                    ?: productId
            } else {
                productId
            }
            val nextSwipeState = swipeState.copy(
                currentProductId = nextCurrentProductId,
                viewedProductIds = (swipeState.viewedProductIds + productId).distinct(),
            )
            return state.copy(productSwipeStates = state.productSwipeStates + (deckId to nextSwipeState))
        }
        val swiped = (swipeState.swipedProductIds + productId).distinct()
        val nextProductId = products.firstOrNull { it !in swiped }
            ?: products.firstOrNull { it != productId }
            ?: productId
        val nextUndoStack = swipeState.undoStack
            .filterNot { action -> action.productId == productId && action.isChoiceFeedback() } +
            ProductSwipeAction(
                productId = productId,
                feedbackType = feedbackType,
                action = action,
            )
        val nextSwipeState = swipeState.copy(
            currentProductId = nextProductId,
            viewedProductIds = (swipeState.viewedProductIds + productId).distinct(),
            swipedProductIds = swiped,
            undoStack = nextUndoStack,
        )
        return state.copy(productSwipeStates = state.productSwipeStates + (deckId to nextSwipeState))
    }

    fun undoSwipe(state: ChatUiState, deckId: String): ChatUiState {
        val swipeState = state.productSwipeStates[deckId] ?: return state
        val restored = swipeState.undoStack.lastOrNull() ?: return state
        val nextSwipeState = swipeState.copy(
            currentProductId = restored.productId,
            swipedProductIds = swipeState.swipedProductIds.filterNot { it == restored.productId },
            undoStack = swipeState.undoStack.dropLast(1),
        )
        return state.copy(productSwipeStates = state.productSwipeStates + (deckId to nextSwipeState))
    }

    fun convergeDeck(
        state: ChatUiState,
        deckId: String,
        usePendingDecision: Boolean = true,
        allowFullyHandled: Boolean = false,
        presentationTurnId: String? = null,
    ): ChatUiState {
        if (state.isDeckFullyHandled(deckId) && !allowFullyHandled) {
            return state.clearDeckConvergence(deckId)
        }
        val pending = state.pendingDecisions[deckId].takeIf { usePendingDecision }
        val withoutWaiting = state.copy(
            awaitingConvergenceDeckIds = state.awaitingConvergenceDeckIds - deckId,
            pendingDecisions = state.pendingDecisions - deckId,
            activeConvergenceDeckId = deckId.takeIf { !usePendingDecision },
            inputState = ChatInputState.Idle,
            isStreaming = false,
        )
        return if (pending == null) {
            withoutWaiting
        } else {
            val decisionTurnId = presentationTurnId?.takeIf { it.isNotBlank() } ?: pending.turnId
            withoutWaiting.upsertNode(
                FinalDecisionNode(
                    key = pending.key,
                    payload = pending.payload,
                    turnId = decisionTurnId,
                    deckId = deckId,
                ),
            )
        }
    }

    private fun reduceTextDelta(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as TextDeltaPayload
        if (state.isActiveConvergenceTurn(envelope)) {
            return state
        }
        if (payload.delta.isKnownCategoryMismatchedFollowup(state, envelope.turnId)) {
            return state
        }
        val rawMessageKey = payload.messageId.ifBlank { envelope.nodeId }
        val existing = state.nodes.filterIsInstance<AiStreamNode>()
            .firstOrNull {
                it.turnId == envelope.turnId &&
                    (
                        it.key == rawMessageKey ||
                            (payload.messageId.isNotBlank() && it.messageId == payload.messageId)
                        )
            }
        if (existing == null && payload.delta.isEmpty()) {
            return state.copy(
                lastError = null,
            )
        }
        val messageKey = existing?.key ?: state.uniqueNodeKeyForTurn(rawMessageKey, envelope.turnId)
        val textBase = state
        val node = AiStreamNode(
            key = existing?.key ?: messageKey,
            messageId = payload.messageId,
            content = (existing?.content.orEmpty()) + payload.delta,
            done = payload.done,
            turnId = envelope.turnId,
        )
        return textBase.upsertNode(node).copy(
            inputState = ChatInputState.Streaming,
            isStreaming = true,
            lastError = null,
            lastAssistantMessageKey = node.key,
            streamingTextKey = node.key,
            streamingTextLength = node.content.length,
        )
    }

    private fun reduceCriteriaCard(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as CriteriaCardPayload
        if (state.isActiveConvergenceTurn(envelope)) {
            return state
        }
        if (payload.criteria.isCategoryMismatchedWithTurnDeck(state, envelope.turnId)) {
            return state
        }
        val rawNodeKey = envelope.nodeId.ifBlank {
            payload.criteria.criteriaId.ifBlank { "criteria_${envelope.turnId}" }
        }
        val existing = state.nodes.filterIsInstance<CriteriaNode>()
            .firstOrNull {
                it.turnId == envelope.turnId &&
                    (
                        it.key == rawNodeKey ||
                            (
                                payload.criteria.criteriaId.isNotBlank() &&
                                    it.payload.criteria.criteriaId == payload.criteria.criteriaId
                                )
                        )
            }
        val nodeKey = existing?.key ?: state.uniqueNodeKeyForTurn(rawNodeKey, envelope.turnId)
        val stalePreviousCriteriaKeys = state.nodes
            .filterIsInstance<CriteriaNode>()
            .mapNotNullTo(mutableSetOf()) { node ->
                node.key.takeIf { node.key != nodeKey && node.turnId != envelope.turnId }
            }
        return state.upsertNode(
            CriteriaNode(
                key = nodeKey,
                payload = payload,
                turnId = envelope.turnId,
            ),
        ).copy(staleCriteriaNodeKeys = (state.staleCriteriaNodeKeys + stalePreviousCriteriaKeys) - nodeKey)
    }

    private fun reduceClarification(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as ClarificationPayload
        if (state.isActiveConvergenceTurn(envelope)) {
            return state
        }
        val rawNodeKey = envelope.nodeId.ifBlank { "clarification_${envelope.turnId}" }
        val existing = state.nodes.filterIsInstance<ClarificationNode>()
            .firstOrNull {
                it.turnId == envelope.turnId &&
                    it.key.matchesRawOrTurnScopedNodeKey(rawNodeKey, envelope.turnId)
            }
        val nodeKey = existing?.key ?: state.uniqueNodeKeyForTurn(rawNodeKey, envelope.turnId)
        return state
            .upsertNode(
                ClarificationNode(
                    key = nodeKey,
                    payload = payload,
                    turnId = envelope.turnId,
                ),
            )
            .copy(
                inputState = ChatInputState.Clarifying,
                isStreaming = true,
                streamingTextKey = null,
                streamingTextLength = 0,
            )
    }

    private fun reduceProductCard(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as ProductCardPayload
        val deckId = envelope.deckId ?: envelope.nodeId
        if (state.isActiveConvergenceTurn(envelope)) {
            return state
        }
        val existing = state.nodes.filterIsInstance<ProductDeckNode>()
            .lastOrNull { it.deckId == deckId && it.turnId == envelope.turnId }
        val isNewDeckInstance = existing == null &&
            state.nodes.any { it is ProductDeckNode && it.deckId == deckId }
        val deckBaseState = if (isNewDeckInstance) {
            state.clearDeckRuntimeState(deckId)
        } else {
            state
        }
        val nodeKey = existing?.key ?: state.uniqueNodeKeyForTurn(deckId, envelope.turnId)
        val products = (existing?.products.orEmpty()
            .filterNot { it.product.productId == payload.product.productId } + payload)
            .sortedBy { it.rank }
        val nextState = deckBaseState.upsertNode(
            ProductDeckNode(
                key = nodeKey,
                deckId = deckId,
                products = products,
                turnId = envelope.turnId,
            ),
        ).pruneMismatchedTurnCriteriaAndFollowup(envelope.turnId)
        return if (products.size <= 1 || nextState.isDeckFullyHandled(deckId)) {
            nextState.clearDeckConvergence(deckId)
        } else {
            nextState
        }
    }

    private fun ChatUiState.uniqueNodeKeyForTurn(baseKey: String, turnId: String): String {
        if (nodes.none { it.key == baseKey }) return baseKey
        val turnScopedKey = if (turnId.isNotBlank()) "${baseKey}_$turnId" else baseKey
        if (nodes.none { it.key == turnScopedKey }) return turnScopedKey
        var index = 2
        while (nodes.any { it.key == "${turnScopedKey}_$index" }) {
            index += 1
        }
        return "${turnScopedKey}_$index"
    }

    private fun String.matchesRawOrTurnScopedNodeKey(rawKey: String, turnId: String): Boolean {
        if (this == rawKey) return true
        if (turnId.isBlank()) return false
        val turnScopedPrefix = "${rawKey}_$turnId"
        return this == turnScopedPrefix || startsWith("${turnScopedPrefix}_")
    }

    private fun reduceFinalDecision(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as FinalDecisionPayload
        val explicitDeckId = envelope.deckId?.takeIf { it.isNotBlank() }
        val rawNodeKey = envelope.nodeId.ifBlank { "decision_${envelope.turnId}" }
        val existing = state.nodes.filterIsInstance<FinalDecisionNode>()
            .lastOrNull {
                it.turnId == envelope.turnId &&
                    it.key.matchesRawOrTurnScopedNodeKey(rawNodeKey, envelope.turnId)
            }
        val nodeKey = existing?.key ?: state.uniqueNodeKeyForTurn(rawNodeKey, envelope.turnId)
        if (
            state.activeConvergenceDeckId != null &&
            explicitDeckId != null &&
            explicitDeckId != state.activeConvergenceDeckId
        ) {
            return state
        }
        val latestDeck = state.nodes.filterIsInstance<ProductDeckNode>().lastOrNull()
        val associatedDeck = explicitDeckId
            ?.let { deckId ->
                state.nodes.filterIsInstance<ProductDeckNode>().lastOrNull { it.deckId == deckId }
            }
            ?: latestDeck?.takeIf { deck ->
                deck.products.size >= 2 && envelope.turnId == deck.turnId
            }
        val deckId = associatedDeck?.deckId
        val isMultiProductDeckDecision = associatedDeck != null && associatedDeck.products.size >= 2
        val isCurrentDeckScopedConvergence = explicitDeckId != null &&
            envelope.turnId.isNotBlank() &&
            (state.currentTurnId == envelope.turnId || state.activeConvergenceDeckId == explicitDeckId) &&
            state.activeConvergenceDeckId == explicitDeckId
        if (deckId != null && isMultiProductDeckDecision && !isCurrentDeckScopedConvergence) {
            if (state.isDeckFullyHandled(deckId)) {
                return state.clearDeckConvergence(deckId)
            }
            return state.copy(
                awaitingConvergenceDeckIds = state.awaitingConvergenceDeckIds,
                latestConvergeableDeckId = state.latestConvergeableDeckId.takeIf { it == deckId },
                pendingDecisions = state.pendingDecisions + (
                    deckId to PendingDecision(
                        key = nodeKey,
                        payload = payload,
                        turnId = envelope.turnId,
                    )
                    ),
            )
        }

        val withDecision = state.upsertNode(
            FinalDecisionNode(
                key = nodeKey,
                payload = payload,
                turnId = envelope.turnId,
                deckId = deckId,
            ),
        )
        return if (deckId != null && isMultiProductDeckDecision) {
            withDecision.clearDeckConvergence(deckId)
        } else {
            withDecision
        }
    }

    private fun ChatUiState.productIdsForDeck(deckId: String): List<String> =
        nodes.filterIsInstance<ProductDeckNode>()
            .lastOrNull { it.deckId == deckId }
            ?.products
            .orEmpty()
            .map { it.product.productId }
            .filter { it.isNotBlank() }

    private fun ChatUiState.productCountForDeck(deckId: String): Int =
        nodes.filterIsInstance<ProductDeckNode>()
            .lastOrNull { it.deckId == deckId }
            ?.products
            .orEmpty()
            .size

    private fun ChatUiState.isActiveConvergenceTurn(envelope: AgentUiEnvelope<AgentPayload>): Boolean =
        activeConvergenceDeckId != null &&
            currentTurnId == envelope.turnId &&
            envelope.turnId.isNotBlank()

    private fun ChatUiState.turnDeckCategories(turnId: String): Set<String> =
        nodes.filterIsInstance<ProductDeckNode>()
            .filter { deck -> turnId.isNotBlank() && deck.turnId == turnId }
            .flatMap { deck -> deck.products.map { it.product.category.normalizedCategory() } }
            .filter { it.isNotBlank() }
            .toSet()

    private fun ChatUiState.pruneMismatchedTurnCriteriaAndFollowup(turnId: String): ChatUiState {
        val deckCategories = turnDeckCategories(turnId)
        if (deckCategories.isEmpty()) return this
        val nextNodes = nodes.filterNot { node ->
            when (node) {
                is CriteriaNode -> node.turnId == turnId &&
                    node.payload.criteria.category.normalizedCategory()
                        .takeIf { it.isNotBlank() }
                        ?.let { it !in deckCategories } == true
                is AiStreamNode -> node.turnId == turnId &&
                    node.content.hasBeautySpecificFollowupCopy() &&
                    "美妆护肤" !in deckCategories
                else -> false
            }
        }
        return if (nextNodes.size == nodes.size) this else copy(nodes = nextNodes)
    }

    private fun CriteriaPayload.isCategoryMismatchedWithTurnDeck(
        state: ChatUiState,
        turnId: String,
    ): Boolean {
        val criteriaCategory = category.normalizedCategory()
        if (criteriaCategory.isBlank()) return false
        val deckCategories = state.turnDeckCategories(turnId)
        if (deckCategories.isEmpty()) return false
        return criteriaCategory !in deckCategories
    }

    private fun String.isKnownCategoryMismatchedFollowup(
        state: ChatUiState,
        turnId: String,
    ): Boolean {
        val text = trim()
        if (text.isBlank() || !text.hasBeautySpecificFollowupCopy()) return false
        val deckCategories = state.turnDeckCategories(turnId)
        if (deckCategories.isEmpty()) return false
        return "美妆护肤" !in deckCategories
    }

    private fun String.hasBeautySpecificFollowupCopy(): Boolean =
        contains("再温和一点") ||
            contains("不要酒精") ||
            contains("预算再低一点") ||
            contains("预算低一点")

    private fun String.normalizedCategory(): String =
        trim()

    private fun ChatUiState.isDeckFullyHandled(deckId: String): Boolean {
        val products = productIdsForDeck(deckId)
        if (products.size <= 1) return false
        val handledProductIds = productSwipeStates[deckId]?.swipedProductIds.orEmpty().toSet()
        return products.all { it in handledProductIds }
    }

    private fun ProductSwipeState?.orEmpty(): ProductSwipeState = this ?: ProductSwipeState()

    private fun ProductSwipeAction.isChoiceFeedback(): Boolean =
        feedbackType == "like" ||
            feedbackType == "not_interested" ||
            action == "like" ||
            action == "not_interested"

    private fun ChatUiState.markLatestDeckAwaitingConvergence(deckId: String): ChatUiState =
        copy(
            awaitingConvergenceDeckIds = setOf(deckId),
            latestConvergeableDeckId = deckId,
            pendingDecisions = pendingDecisions.filterKeys { it == deckId },
        )

    private fun ChatUiState.clearDeckConvergence(deckId: String): ChatUiState =
        copy(
            awaitingConvergenceDeckIds = awaitingConvergenceDeckIds - deckId,
            latestConvergeableDeckId = latestConvergeableDeckId.takeIf { it != deckId },
            activeConvergenceDeckId = activeConvergenceDeckId.takeIf { it != deckId },
            pendingDecisions = pendingDecisions - deckId,
        )

    private fun ChatUiState.clearDeckRuntimeState(deckId: String): ChatUiState =
        clearDeckConvergence(deckId).copy(
            productSwipeStates = productSwipeStates - deckId,
        )

    private fun ChatUiState.upsertNode(node: ChatUiNode): ChatUiState {
        val index = nodes.indexOfFirst { it.key == node.key }
        val nextNodes = if (index >= 0) {
            nodes.toMutableList().also { it[index] = node }
        } else {
            nodes + node
        }
        return copy(nodes = nextNodes)
    }

    private fun ChatUiState.upsertThinkingAtTail(node: ThinkingNode): ChatUiState {
        val activeTurnId = node.turnId.takeIf { it.isNotBlank() }
        fun hasSameTurnContentAfter(index: Int): Boolean {
            if (activeTurnId == null) return false
            return nodes.drop(index + 1).any { next ->
                next !is ThinkingNode && next.assistantTurnIdOrNull() == activeTurnId
            }
        }
        val nextNodes = nodes.filterIndexed { index, existing ->
            if (existing !is ThinkingNode) return@filterIndexed true
            val sameKey = existing.key == node.key
            val sameStage = activeTurnId != null &&
                existing.turnId == activeTurnId &&
                existing.payload.stage == node.payload.stage
            val unpairedSameTurnThinking = activeTurnId != null &&
                existing.turnId == activeTurnId &&
                !hasSameTurnContentAfter(index)
            !(sameKey || sameStage || unpairedSameTurnThinking)
        } + node
        return copy(nodes = nextNodes)
    }

    private fun ChatUiNode.assistantTurnIdOrNull(): String? =
        when (this) {
            is ThinkingNode -> turnId
            is AiStreamNode -> turnId
            is ClarificationNode -> turnId
            is CriteriaNode -> turnId
            is ProductDeckNode -> turnId
            is FinalDecisionNode -> turnId
            else -> null
        }?.takeIf { it.isNotBlank() }

    private fun ThinkingPayload.thinkingNodeKey(envelope: AgentUiEnvelope<AgentPayload>): String {
        val stableStage = stage.takeIf { it.isNotBlank() } ?: "thinking"
        return "${envelope.nodeId}_$stableStage"
    }

    private fun AgentEventType.shouldClearTransientThinking(state: ChatUiState, turnId: String): Boolean =
        when (this) {
            AgentEventType.Thinking,
            AgentEventType.TextDelta,
            AgentEventType.CriteriaCard,
            AgentEventType.ProductCard,
            AgentEventType.Clarification,
            AgentEventType.Unknown -> false
            AgentEventType.FinalDecision -> true
            AgentEventType.Done -> !state.hasClarificationForTurn(turnId)
            else -> true
        }

    private fun ChatUiState.hasClarificationForTurn(turnId: String): Boolean =
        turnId.isNotBlank() && nodes.any { it is ClarificationNode && it.turnId == turnId }

    private fun ChatUiState.withoutThinking(turnId: String): ChatUiState {
        val thinkingKey = "thinking_$turnId"
        val nextNodes = nodes.filterNot {
            it is ThinkingNode && (it.turnId == turnId || it.key == thinkingKey)
        }
        return if (nextNodes.size == nodes.size) {
            this
        } else {
            copy(nodes = nextNodes)
        }
    }

    private fun ChatUiState.markTurnTextDone(turnId: String): ChatUiState {
        if (turnId.isBlank()) return this
        var changed = false
        val nextNodes = nodes.map { node ->
            if (node is AiStreamNode && node.turnId == turnId && !node.done) {
                changed = true
                node.copy(done = true)
            } else {
                node
            }
        }
        return if (changed) copy(nodes = nextNodes) else this
    }

    private fun ChatUiState.markStreamingTextDoneWhenTurnMissing(turnId: String): ChatUiState {
        if (turnId.isNotBlank()) return this
        val key = streamingTextKey ?: return this
        var changed = false
        val nextNodes = nodes.map { node ->
            if (node is AiStreamNode && node.key == key && !node.done) {
                changed = true
                node.copy(done = true)
            } else {
                node
            }
        }
        return if (changed) copy(nodes = nextNodes) else this
    }
}
