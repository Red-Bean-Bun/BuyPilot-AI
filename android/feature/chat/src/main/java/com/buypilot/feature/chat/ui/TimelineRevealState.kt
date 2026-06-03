package com.buypilot.feature.chat.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.presentation.revealTextKey

private const val TextRevealProgressReportStep = 24

@Immutable
internal data class TextRevealProgress(
    val visibleLength: Int = 0,
    val totalLength: Int = 0,
) {
    val hasStarted: Boolean
        get() = visibleLength > 0
}

internal data class TurnNodeVisibilityState(
    val visibleNodeKeys: Set<String>,
    val textHandoffKeys: Set<String>,
    val structuredHandoffKeys: Set<String>,
)

@Stable
internal class TimelineRevealStore {
    private val enteredTimelineItemKeys = mutableMapOf<String, Boolean>()
    private val startedStructuredNodeKeys = mutableMapOf<String, Boolean>()
    val enteredStructuredNodeKeys = mutableStateMapOf<String, Boolean>()
    val completedTextKeys = mutableStateMapOf<String, Boolean>()
    val textRevealProgressByKey = mutableStateMapOf<String, TextRevealProgress>()
    private val liveRevealedTextKeys = mutableStateMapOf<String, Boolean>()
    private val latestTextRevealProgressByKey = mutableMapOf<String, TextRevealProgress>()
    private val lastSnapshotTextRevealProgressByKey = mutableMapOf<String, TextRevealProgress>()

    fun hasEnteredTimelineItem(key: String): Boolean = enteredTimelineItemKeys[key] == true
    fun markTimelineItemEntered(key: String) {
        enteredTimelineItemKeys[key] = true
    }

    fun hasStartedStructuredNode(key: String): Boolean = startedStructuredNodeKeys[key] == true
    fun markStructuredNodeStarted(key: String) {
        startedStructuredNodeKeys[key] = true
    }

    fun hasEnteredStructuredNode(key: String): Boolean = enteredStructuredNodeKeys[key] == true
    fun markStructuredNodeEntered(key: String) {
        enteredStructuredNodeKeys[key] = true
    }

    fun hasCompletedText(key: String): Boolean = completedTextKeys[key] == true
    fun markTextCompleted(key: String) {
        completedTextKeys[key] = true
    }

    fun hasLiveRevealedText(key: String): Boolean = liveRevealedTextKeys[key] == true

    fun updateTextProgress(key: String, visible: Int, total: Int) {
        val previous = lastSnapshotTextRevealProgressByKey[key]
        val next = TextRevealProgress(visibleLength = visible, totalLength = total)
        latestTextRevealProgressByKey[key] = next
        if (total > 0 && visible > 0 && visible < total && liveRevealedTextKeys[key] != true) {
            liveRevealedTextKeys[key] = true
        }
        val completed = total > 0 && visible >= total
        val shouldSnapshot = previous == null ||
            !previous.hasStarted ||
            completed ||
            visible - previous.visibleLength >= TextRevealProgressReportStep
        if (shouldSnapshot && textRevealProgressByKey[key] != next) {
            lastSnapshotTextRevealProgressByKey[key] = next
            textRevealProgressByKey[key] = next
        }
    }

    fun textRevealProgress(key: String): TextRevealProgress? =
        latestTextRevealProgressByKey[key] ?: textRevealProgressByKey[key]

    fun pruneToKeys(timelineItemKeys: Set<String>, nodeKeys: Set<String>) {
        enteredTimelineItemKeys.keys.retainAll(timelineItemKeys)
        startedStructuredNodeKeys.keys.retainAll(nodeKeys)
        enteredStructuredNodeKeys.keys.retainAll(nodeKeys)
        completedTextKeys.keys.retainAll(nodeKeys)
        textRevealProgressByKey.keys.retainAll(nodeKeys)
        liveRevealedTextKeys.keys.retainAll(nodeKeys)
        latestTextRevealProgressByKey.keys.retainAll(nodeKeys)
        lastSnapshotTextRevealProgressByKey.keys.retainAll(nodeKeys)
    }
}

internal fun shouldAnimateTurnNode(
    motionEnabled: Boolean,
    hasStarted: Boolean,
): Boolean = motionEnabled && !hasStarted

internal fun shouldAnimateTimelineItem(
    animateEnter: Boolean,
    hasEntered: Boolean,
): Boolean = animateEnter && !hasEntered

internal fun shouldConsumeFlightUserAnchor(
    userMessageKey: String?,
    activeFlightMessageKey: String?,
): Boolean =
    userMessageKey != null && userMessageKey == activeFlightMessageKey

internal fun shouldAnimateInitialCompletedText(
    turnId: String,
    currentTurnId: String?,
    revealKey: String,
    revealedMessageKeys: Set<String>,
    liveRevealedMessageKeys: Set<String>,
): Boolean =
    turnId == currentTurnId &&
        revealKey !in revealedMessageKeys &&
        revealKey !in liveRevealedMessageKeys

internal fun List<ChatUiNode>.visibleTurnNodeKeys(
    completedTextKeys: Set<String>,
    textRevealProgress: Map<String, TextRevealProgress>,
    enteredStructuredKeys: Set<String>,
): TurnNodeVisibilityState {
    val visible = mutableSetOf<String>()
    val handoffTexts = mutableSetOf<String>()
    val handoffStructured = mutableSetOf<String>()

    forEachIndexed { index, node ->
        when (node) {
            is ThinkingNode -> {
                if (canRevealThinkingNode(index, completedTextKeys, textRevealProgress, enteredStructuredKeys)) {
                    visible += node.key
                }
            }
            else -> {
                if (canRevealTurnNode(index, node, completedTextKeys, enteredStructuredKeys)) {
                    visible += node.key
                    val revealTextKey = node.revealTextKey()
                    if (revealTextKey != null &&
                        textRevealProgress[revealTextKey]?.hasStarted != true &&
                        shouldDelayNodeForThinkingExit(index)
                    ) {
                        handoffTexts += revealTextKey
                    } else if (
                        revealTextKey == null &&
                        node.key !in enteredStructuredKeys &&
                        shouldDelayNodeForThinkingExit(index)
                    ) {
                        handoffStructured += node.key
                    }
                }
            }
        }
    }
    return TurnNodeVisibilityState(
        visibleNodeKeys = visible,
        textHandoffKeys = handoffTexts,
        structuredHandoffKeys = handoffStructured,
    )
}

internal fun List<ChatUiNode>.textRevealProgressForVisibility(
    revealStore: TimelineRevealStore,
    liveRevealedMessageKeys: Set<String>,
): Map<String, TextRevealProgress> =
    mapNotNull { node ->
        node.revealTextKey()?.let { key ->
            val storedProgress = revealStore.textRevealProgress(key)
            val liveProgress = node
                .takeIf { key in liveRevealedMessageKeys }
                ?.liveRevealedProgress()
            (storedProgress ?: liveProgress)?.let { key to it }
        }
    }.toMap()

internal fun List<ChatUiNode>.canRevealThinkingNode(
    index: Int,
    completedTextKeys: Set<String>,
    textRevealProgress: Map<String, TextRevealProgress>,
    enteredStructuredKeys: Set<String>,
): Boolean {
    for (previousIndex in 0 until index) {
        val previous = this[previousIndex]
        when (previous) {
            is ThinkingNode -> Unit
            is AiStreamNode -> {
                if (previous.hasBlankRevealContent()) continue
                if (previous.key !in completedTextKeys) return false
            }
            is ClarificationNode -> {
                if (previous.revealTextKey() !in completedTextKeys || previous.key !in enteredStructuredKeys) {
                    return false
                }
            }
            else -> {
                if (previous.key !in enteredStructuredKeys) return false
            }
        }
    }
    val nextNode = (index + 1 until size)
        .map { this[it] }
        .firstOrNull { it !is ThinkingNode && !it.hasBlankRevealContent() }
    val visualOnlyThinking = (this[index] as ThinkingNode)
        .payload
        .userFacingThinkingMessage()
        .isBlank()
    if (visualOnlyThinking) return false
    return when (nextNode) {
        null -> true
        is AiStreamNode -> {
            val nextTextKey = nextNode.revealTextKey()
            nextTextKey != null &&
                nextTextKey !in completedTextKeys &&
                textRevealProgress[nextTextKey]?.hasStarted != true
        }
        is ClarificationNode -> {
            val questionKey = nextNode.revealTextKey()
            questionKey != null &&
                questionKey !in completedTextKeys &&
                textRevealProgress[questionKey]?.hasStarted != true
        }
        else -> nextNode.key !in enteredStructuredKeys
    }
}

internal fun List<ChatUiNode>.canRevealTurnNode(
    index: Int,
    node: ChatUiNode,
    completedTextKeys: Set<String>,
    enteredStructuredKeys: Set<String>,
): Boolean {
    if (node.hasBlankRevealContent()) return false
    for (previousIndex in 0 until index) {
        val previous = this[previousIndex]
        when (previous) {
            is ThinkingNode -> Unit
            is AiStreamNode -> {
                if (previous.hasBlankRevealContent()) continue
                val previousRevealKey = previous.revealTextKey()
                if (previousRevealKey !in completedTextKeys) {
                    return false
                }
            }
            is ClarificationNode -> {
                if (previous.revealTextKey() !in completedTextKeys || previous.key !in enteredStructuredKeys) {
                    return false
                }
            }
            else -> {
                if (previous.key !in enteredStructuredKeys) return false
            }
        }
    }
    return true
}

internal fun List<ChatUiNode>.shouldDelayNodeForThinkingExit(index: Int): Boolean =
    index > 0 &&
        !this[index].hasBlankRevealContent() &&
        (this[index - 1] as? ThinkingNode)
            ?.payload
            ?.userFacingThinkingMessage()
            ?.isNotBlank() == true

internal fun List<ChatUiNode>.settledTextRevealKeys(): Set<String> =
    mapNotNullTo(mutableSetOf()) { node -> node.revealTextKey() }

internal fun List<ChatUiNode>.settledStructuredNodeKeys(): Set<String> =
    filterNot { it is AiStreamNode || it is ThinkingNode }
        .mapTo(mutableSetOf()) { node -> node.key }

internal fun List<ChatUiNode>.hasUnsettledTurnVisual(
    completedTextKeys: Set<String>,
    enteredStructuredKeys: Set<String>,
): Boolean =
    any { node ->
        when (node) {
            is AiStreamNode -> !node.hasBlankRevealContent() && node.revealTextKey() !in completedTextKeys
            is ClarificationNode -> node.revealTextKey() !in completedTextKeys || node.key !in enteredStructuredKeys
            is ThinkingNode -> false
            else -> node.key !in enteredStructuredKeys
        }
    }

internal fun List<ChatUiNode>.allTurnTextRevealed(
    revealStore: TimelineRevealStore,
    revealedMessageKeys: Set<String>,
): Boolean =
    all { node ->
        val revealKey = node.revealTextKey()
        revealKey == null ||
            node.hasBlankRevealContent() ||
            revealStore.hasCompletedText(revealKey) ||
            revealKey in revealedMessageKeys
    }

internal fun ChatUiNode.isTextOrThinkingNode(): Boolean =
    this is AiStreamNode || this is ThinkingNode

internal fun List<ChatUiNode>.shouldRenderThinkingNodeInOwnSlot(index: Int): Boolean {
    if (getOrNull(index) !is ThinkingNode) return false
    (index + 1 until size)
        .map { this[it] }
        .firstOrNull { it !is ThinkingNode && !it.hasBlankRevealContent() }
        ?: return true
    return false
}

private fun ChatUiNode.hasBlankRevealContent(): Boolean =
    when (this) {
        is AiStreamNode -> content.isBlank()
        is ClarificationNode -> payload.question.isBlank()
        else -> false
    }

internal fun ChatUiNode.liveRevealedProgress(): TextRevealProgress? {
    val contentLength = when (this) {
        is AiStreamNode -> content.length
        is ClarificationNode -> payload.question.length
        else -> 0
    }
    if (contentLength <= 0) return null
    return TextRevealProgress(
        visibleLength = contentLength,
        totalLength = contentLength,
    )
}
