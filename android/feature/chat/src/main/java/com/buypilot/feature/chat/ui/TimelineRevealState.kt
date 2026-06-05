package com.buypilot.feature.chat.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.presentation.revealTextKey

private const val TextRevealProgressReportStep = 24
private const val SnapshotProgressSeparator = "\u001F"

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

@Immutable
internal data class TimelineRevealSnapshot(
    val enteredTimelineItemKeys: Set<String> = emptySet(),
    val startedStructuredNodeKeys: Set<String> = emptySet(),
    val enteredStructuredNodeKeys: Set<String> = emptySet(),
    val completedTextKeys: Set<String> = emptySet(),
    val liveRevealedTextKeys: Set<String> = emptySet(),
    val textRevealProgressByKey: Map<String, TextRevealProgress> = emptyMap(),
    val latestTextRevealProgressByKey: Map<String, TextRevealProgress> = emptyMap(),
    val lastSnapshotTextRevealProgressByKey: Map<String, TextRevealProgress> = emptyMap(),
)

@Stable
internal class TimelineRevealSnapshotHolder(
    snapshot: TimelineRevealSnapshot = TimelineRevealSnapshot(),
) {
    var snapshot by mutableStateOf(snapshot)
}

internal val TimelineRevealSnapshotHolderSaver = Saver<TimelineRevealSnapshotHolder, ArrayList<Any>>(
    save = { holder ->
        val snapshot = holder.snapshot
        arrayListOf(
            ArrayList(snapshot.enteredTimelineItemKeys),
            ArrayList(snapshot.startedStructuredNodeKeys),
            ArrayList(snapshot.enteredStructuredNodeKeys),
            ArrayList(snapshot.completedTextKeys),
            ArrayList(snapshot.liveRevealedTextKeys),
            encodeProgressMap(snapshot.textRevealProgressByKey),
            encodeProgressMap(snapshot.latestTextRevealProgressByKey),
            encodeProgressMap(snapshot.lastSnapshotTextRevealProgressByKey),
        )
    },
    restore = { saved ->
        TimelineRevealSnapshotHolder(
            TimelineRevealSnapshot(
                enteredTimelineItemKeys = saved.stringSetAt(0),
                startedStructuredNodeKeys = saved.stringSetAt(1),
                enteredStructuredNodeKeys = saved.stringSetAt(2),
                completedTextKeys = saved.stringSetAt(3),
                liveRevealedTextKeys = saved.stringSetAt(4),
                textRevealProgressByKey = saved.progressMapAt(5),
                latestTextRevealProgressByKey = saved.progressMapAt(6),
                lastSnapshotTextRevealProgressByKey = saved.progressMapAt(7),
            ),
        )
    },
)

@Stable
internal class TimelineRevealStore(
    initialSnapshot: TimelineRevealSnapshot = TimelineRevealSnapshot(),
    private val onSnapshotChanged: (TimelineRevealSnapshot) -> Unit = {},
) {
    private val enteredTimelineItemKeys = mutableMapOf<String, Boolean>()
    private val startedStructuredNodeKeys = mutableMapOf<String, Boolean>()
    val enteredStructuredNodeKeys = mutableStateMapOf<String, Boolean>()
    val completedTextKeys = mutableStateMapOf<String, Boolean>()
    val textRevealProgressByKey = mutableStateMapOf<String, TextRevealProgress>()
    private val liveRevealedTextKeys = mutableStateMapOf<String, Boolean>()
    private val latestTextRevealProgressByKey = mutableMapOf<String, TextRevealProgress>()
    private val lastSnapshotTextRevealProgressByKey = mutableMapOf<String, TextRevealProgress>()

    init {
        initialSnapshot.enteredTimelineItemKeys.forEach { enteredTimelineItemKeys[it] = true }
        initialSnapshot.startedStructuredNodeKeys.forEach { startedStructuredNodeKeys[it] = true }
        initialSnapshot.enteredStructuredNodeKeys.forEach { enteredStructuredNodeKeys[it] = true }
        initialSnapshot.completedTextKeys.forEach { completedTextKeys[it] = true }
        initialSnapshot.liveRevealedTextKeys.forEach { liveRevealedTextKeys[it] = true }
        textRevealProgressByKey.putAll(initialSnapshot.textRevealProgressByKey)
        latestTextRevealProgressByKey.putAll(initialSnapshot.latestTextRevealProgressByKey)
        lastSnapshotTextRevealProgressByKey.putAll(initialSnapshot.lastSnapshotTextRevealProgressByKey)
    }

    fun hasEnteredTimelineItem(key: String): Boolean = enteredTimelineItemKeys[key] == true
    fun markTimelineItemEntered(key: String) {
        if (enteredTimelineItemKeys[key] == true) return
        enteredTimelineItemKeys[key] = true
        notifySnapshotChanged()
    }

    fun hasStartedStructuredNode(key: String): Boolean = startedStructuredNodeKeys[key] == true
    fun markStructuredNodeStarted(key: String) {
        if (startedStructuredNodeKeys[key] == true) return
        startedStructuredNodeKeys[key] = true
        notifySnapshotChanged()
    }

    fun hasEnteredStructuredNode(key: String): Boolean = enteredStructuredNodeKeys[key] == true
    fun markStructuredNodeEntered(key: String) {
        if (enteredStructuredNodeKeys[key] == true) return
        enteredStructuredNodeKeys[key] = true
        notifySnapshotChanged()
    }

    fun hasCompletedText(key: String): Boolean = completedTextKeys[key] == true
    fun markTextCompleted(key: String) {
        if (completedTextKeys[key] == true) return
        completedTextKeys[key] = true
        notifySnapshotChanged()
    }

    fun hasLiveRevealedText(key: String): Boolean = liveRevealedTextKeys[key] == true
    fun markTextLiveRevealed(key: String) {
        if (liveRevealedTextKeys[key] == true) return
        liveRevealedTextKeys[key] = true
        notifySnapshotChanged()
    }

    fun completedTextKeySet(): Set<String> = completedTextKeys.keys.toSet()
    fun liveRevealedTextKeySet(): Set<String> = liveRevealedTextKeys.keys.toSet()

    fun updateTextProgress(key: String, visible: Int, total: Int) {
        val previous = lastSnapshotTextRevealProgressByKey[key]
        val next = TextRevealProgress(visibleLength = visible, totalLength = total)
        latestTextRevealProgressByKey[key] = next
        var snapshotDirty = false
        if (total > 0 && visible > 0 && visible < total && liveRevealedTextKeys[key] != true) {
            liveRevealedTextKeys[key] = true
            snapshotDirty = true
        }
        val completed = total > 0 && visible >= total
        val shouldSnapshot = previous == null ||
            !previous.hasStarted ||
            completed ||
            visible - previous.visibleLength >= TextRevealProgressReportStep
        if (shouldSnapshot && textRevealProgressByKey[key] != next) {
            lastSnapshotTextRevealProgressByKey[key] = next
            textRevealProgressByKey[key] = next
            snapshotDirty = true
        }
        if (snapshotDirty) {
            notifySnapshotChanged()
        }
    }

    fun textRevealProgress(key: String): TextRevealProgress? =
        latestTextRevealProgressByKey[key] ?: textRevealProgressByKey[key]

    fun pruneToKeys(timelineItemKeys: Set<String>, nodeKeys: Set<String>) {
        val before = snapshot()
        enteredTimelineItemKeys.keys.retainAll(timelineItemKeys)
        startedStructuredNodeKeys.keys.retainAll(nodeKeys)
        enteredStructuredNodeKeys.keys.retainAll(nodeKeys)
        completedTextKeys.keys.retainAll(nodeKeys)
        textRevealProgressByKey.keys.retainAll(nodeKeys)
        liveRevealedTextKeys.keys.retainAll(nodeKeys)
        latestTextRevealProgressByKey.keys.retainAll(nodeKeys)
        lastSnapshotTextRevealProgressByKey.keys.retainAll(nodeKeys)
        val after = snapshot()
        if (after != before) {
            onSnapshotChanged(after)
        }
    }

    fun snapshot(): TimelineRevealSnapshot =
        TimelineRevealSnapshot(
            enteredTimelineItemKeys = enteredTimelineItemKeys.keys.toSet(),
            startedStructuredNodeKeys = startedStructuredNodeKeys.keys.toSet(),
            enteredStructuredNodeKeys = enteredStructuredNodeKeys.keys.toSet(),
            completedTextKeys = completedTextKeys.keys.toSet(),
            liveRevealedTextKeys = liveRevealedTextKeys.keys.toSet(),
            textRevealProgressByKey = textRevealProgressByKey.toMap(),
            latestTextRevealProgressByKey = latestTextRevealProgressByKey.toMap(),
            lastSnapshotTextRevealProgressByKey = lastSnapshotTextRevealProgressByKey.toMap(),
        )

    private fun notifySnapshotChanged() {
        onSnapshotChanged(snapshot())
    }
}

private fun encodeProgressMap(map: Map<String, TextRevealProgress>): ArrayList<String> =
    ArrayList(
        map.map { (key, progress) ->
            listOf(key, progress.visibleLength.toString(), progress.totalLength.toString())
                .joinToString(SnapshotProgressSeparator)
        },
    )

private fun ArrayList<Any>.stringSetAt(index: Int): Set<String> =
    (getOrNull(index) as? List<*>)
        ?.filterIsInstance<String>()
        ?.toSet()
        ?: emptySet()

private fun ArrayList<Any>.progressMapAt(index: Int): Map<String, TextRevealProgress> =
    (getOrNull(index) as? List<*>)
        ?.filterIsInstance<String>()
        ?.mapNotNull { encoded ->
            val parts = encoded.split(SnapshotProgressSeparator)
            val key = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val visible = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            val total = parts.getOrNull(2)?.toIntOrNull() ?: return@mapNotNull null
            key to TextRevealProgress(visibleLength = visible, totalLength = total)
        }
        ?.toMap()
        ?: emptyMap()

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
    if (visualOnlyThinking && (this[index] as ThinkingNode).payload.stage != LocalAssistantPendingStage) {
        return false
    }
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
