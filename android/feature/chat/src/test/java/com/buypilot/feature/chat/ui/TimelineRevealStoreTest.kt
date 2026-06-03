package com.buypilot.feature.chat.ui

import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.CartActionPayload
import com.buypilot.core.model.CartSummaryPayload
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.CartActionNode
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ProductSwipeState
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.model.UserMessageNode
import com.buypilot.feature.chat.model.ErrorNode
import com.buypilot.feature.chat.presentation.AssistantTurnTimelineItem
import com.buypilot.feature.chat.presentation.StandaloneTimelineItem
import com.buypilot.feature.chat.presentation.toTimelinePresentationState
import com.buypilot.feature.chat.presentation.toTimelineRenderItems
import com.buypilot.feature.chat.state.ChatUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineRevealStoreTest {
    @Test
    fun scrollCoordinatorKeepsHighestPriorityIntent() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FollowLatest),
                autoFocusSuppressed = false,
            ),
        )
        assertEquals(TimelineScrollIntentKind.FollowLatest, coordinator.pendingIntent?.intent?.kind)
        assertEquals(1, coordinator.pendingIntentCount)

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FinalDecision, key = "final_1"),
                autoFocusSuppressed = false,
            ),
        )
        assertEquals(TimelineScrollIntentKind.FinalDecision, coordinator.pendingIntent?.intent?.kind)
        assertEquals(listOf(TimelineScrollIntentKind.FinalDecision), coordinator.queuedIntentKinds)

        assertFalse(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FollowLatest),
                autoFocusSuppressed = false,
            ),
        )
        assertEquals(TimelineScrollIntentKind.FinalDecision, coordinator.pendingIntent?.intent?.kind)
    }

    @Test
    fun scrollCoordinatorDropsKeyboardCorrectionBehindCurrentAnchor() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.UserSent, key = "user_1"),
                autoFocusSuppressed = false,
            ),
        )
        assertFalse(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.KeyboardChanged, deltaPx = 120),
                autoFocusSuppressed = false,
            ),
        )

        assertEquals(
            listOf(TimelineScrollIntentKind.UserSent),
            coordinator.queuedIntentKinds,
        )
    }

    @Test
    fun scrollCoordinatorAllowsAssistantAnchorAfterUserAnchor() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.UserSent, key = "user_1"),
                autoFocusSuppressed = false,
            ),
        )
        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.AssistantStarted, turnId = "turn_1"),
                autoFocusSuppressed = false,
            ),
        )

        assertEquals(
            listOf(TimelineScrollIntentKind.UserSent, TimelineScrollIntentKind.AssistantStarted),
            coordinator.queuedIntentKinds,
        )
    }

    @Test
    fun scrollCoordinatorReadableAnchorSupersedesViewportFreeze() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.ViewportFreeze, index = 1),
                autoFocusSuppressed = false,
            ),
        )
        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.AssistantStarted, turnId = "turn_1"),
                autoFocusSuppressed = false,
            ),
        )

        assertEquals(
            listOf(TimelineScrollIntentKind.AssistantStarted),
            coordinator.queuedIntentKinds,
        )
    }

    @Test
    fun scrollCoordinatorDropsViewportFreezeBehindReadableAnchor() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.AssistantStarted, turnId = "turn_1"),
                autoFocusSuppressed = false,
            ),
        )
        assertFalse(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.ViewportFreeze, index = 1),
                autoFocusSuppressed = false,
            ),
        )

        assertEquals(
            listOf(TimelineScrollIntentKind.AssistantStarted),
            coordinator.queuedIntentKinds,
        )
    }

    @Test
    fun scrollCoordinatorKeyboardCorrectionSupersedesQueuedFollowLatest() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FollowLatest),
                autoFocusSuppressed = false,
            ),
        )
        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.KeyboardChanged, deltaPx = 320),
                autoFocusSuppressed = false,
            ),
        )

        assertEquals(
            listOf(TimelineScrollIntentKind.KeyboardChanged),
            coordinator.queuedIntentKinds,
        )
    }

    @Test
    fun scrollCoordinatorFinalDecisionDropsQueuedLowerAnchorsAfterVolatileCorrectionsWereSkipped() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.UserSent, key = "user_1"),
                autoFocusSuppressed = false,
            ),
        )
        assertFalse(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.KeyboardChanged, deltaPx = 120),
                autoFocusSuppressed = false,
            ),
        )
        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.AssistantStarted, turnId = "turn_1"),
                autoFocusSuppressed = false,
            ),
        )
        assertFalse(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FollowLatest),
                autoFocusSuppressed = false,
            ),
        )
        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FinalDecision, key = "final_1"),
                autoFocusSuppressed = false,
            ),
        )

        assertEquals(
            listOf(TimelineScrollIntentKind.FinalDecision),
            coordinator.queuedIntentKinds,
        )
    }

    @Test
    fun scrollCoordinatorDoesNotRestartSamePriorityIntent() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FollowLatest),
                autoFocusSuppressed = false,
            ),
        )
        val firstPending = coordinator.pendingIntent ?: error("missing first pending intent")

        assertFalse(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FollowLatest),
                autoFocusSuppressed = false,
            ),
        )

        assertSame(firstPending, coordinator.pendingIntent)
    }

    @Test
    fun scrollCoordinatorReplacesSamePriorityWhenTargetChanges() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FinalDecision, key = "final_1"),
                autoFocusSuppressed = false,
            ),
        )
        val firstPending = coordinator.pendingIntent ?: error("missing first pending intent")

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FinalDecision, key = "final_2"),
                autoFocusSuppressed = false,
            ),
        )

        val replaced = coordinator.pendingIntent ?: error("missing replacement pending intent")
        assertEquals(TimelineScrollIntentKind.FinalDecision, replaced.intent.kind)
        assertEquals("final_2", replaced.intent.key)
        assertTrue(replaced.serial > firstPending.serial)
    }

    @Test
    fun scrollCoordinatorAllowsExplicitUserAnchorDuringAutoFocusSuppression() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.UserSent, key = "user_1"),
                autoFocusSuppressed = true,
            ),
        )
        assertEquals(TimelineScrollIntentKind.UserSent, coordinator.pendingIntent?.intent?.kind)
        coordinator.clear(coordinator.pendingIntent?.serial ?: error("missing user sent intent"))

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.RouteReturn, key = "assistant_1"),
                autoFocusSuppressed = true,
            ),
        )
        assertEquals(TimelineScrollIntentKind.RouteReturn, coordinator.pendingIntent?.intent?.kind)
    }

    @Test
    fun explicitUserAnchorSupersedesStaleRouteReturn() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(
                    kind = TimelineScrollIntentKind.RouteReturn,
                    key = "deck_before_detail",
                    index = 4,
                    scrollOffset = 120,
                ),
                autoFocusSuppressed = true,
            ),
        )
        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(
                    kind = TimelineScrollIntentKind.UserSent,
                    key = "user_after_detail",
                    index = 9,
                ),
                autoFocusSuppressed = true,
            ),
        )

        assertEquals(
            listOf(TimelineScrollIntentKind.UserSent),
            coordinator.queuedIntentKinds,
        )
        assertEquals("user_after_detail", coordinator.pendingIntent?.intent?.key)
    }

    @Test
    fun staleRouteReturnCannotSupersedeQueuedUserAnchor() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(
                    kind = TimelineScrollIntentKind.UserSent,
                    key = "user_after_detail",
                    index = 9,
                ),
                autoFocusSuppressed = true,
            ),
        )
        assertFalse(
            coordinator.request(
                intent = TimelineScrollIntent(
                    kind = TimelineScrollIntentKind.RouteReturn,
                    key = "deck_before_detail",
                    index = 4,
                    scrollOffset = 120,
                ),
                autoFocusSuppressed = true,
            ),
        )

        assertEquals(
            listOf(TimelineScrollIntentKind.UserSent),
            coordinator.queuedIntentKinds,
        )
        assertEquals("user_after_detail", coordinator.pendingIntent?.intent?.key)
    }

    @Test
    fun viewportFreezeDoesNotQueueBehindRouteReturn() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(
                    kind = TimelineScrollIntentKind.RouteReturn,
                    key = "deck_before_detail",
                    index = 4,
                    scrollOffset = 120,
                ),
                autoFocusSuppressed = true,
            ),
        )
        assertFalse(
            coordinator.request(
                intent = TimelineScrollIntent(
                    kind = TimelineScrollIntentKind.ViewportFreeze,
                    key = "clarification_before_detail",
                    index = 5,
                    scrollOffset = 80,
                ),
                autoFocusSuppressed = true,
            ),
        )

        assertEquals(
            listOf(TimelineScrollIntentKind.RouteReturn),
            coordinator.queuedIntentKinds,
        )
    }

    @Test
    fun explicitUserAnchorSupersedesStaleSystemAnchors() {
        val coordinator = TimelineScrollCoordinator()

        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FinalDecision, key = "old_final"),
                autoFocusSuppressed = false,
            ),
        )
        assertTrue(
            coordinator.request(
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.UserSent, key = "new_user"),
                autoFocusSuppressed = false,
            ),
        )

        assertEquals(
            listOf(TimelineScrollIntentKind.UserSent),
            coordinator.queuedIntentKinds,
        )
    }

    @Test
    fun suppressionBlockerMatchesScrollCoordinatorAllowedKinds() {
        assertFalse(
            shouldBlockTimelineAutoFocusForSuppression(
                intentKind = TimelineScrollIntentKind.UserSent,
                isAutoFocusSuppressed = true,
            ),
        )
        assertFalse(
            shouldBlockTimelineAutoFocusForSuppression(
                intentKind = TimelineScrollIntentKind.RouteReturn,
                isAutoFocusSuppressed = true,
            ),
        )
        assertFalse(
            shouldBlockTimelineAutoFocusForSuppression(
                intentKind = TimelineScrollIntentKind.ViewportFreeze,
                isAutoFocusSuppressed = true,
            ),
        )
        assertTrue(
            shouldBlockTimelineAutoFocusForSuppression(
                intentKind = TimelineScrollIntentKind.AssistantStarted,
                isAutoFocusSuppressed = true,
            ),
        )
        assertFalse(
            shouldBlockTimelineAutoFocusForSuppression(
                intentKind = TimelineScrollIntentKind.AssistantStarted,
                isAutoFocusSuppressed = false,
            ),
        )
    }

    @Test
    fun scrollCoordinatorUserDragInterruptsPendingIntentAndClearUsesSerial() {
        val coordinator = TimelineScrollCoordinator()
        coordinator.request(
            intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.FinalDecision, key = "final_1"),
            autoFocusSuppressed = false,
        )
        val previousSerial = coordinator.pendingIntent?.serial ?: error("missing initial pending intent")

        coordinator.interruptWithUserDrag()

        val dragIntent = coordinator.pendingIntent ?: error("missing user drag intent")
        assertEquals(TimelineScrollIntentKind.UserDrag, dragIntent.intent.kind)
        assertTrue(dragIntent.serial > previousSerial)

        coordinator.clear(previousSerial)
        assertSame(dragIntent, coordinator.pendingIntent)

        coordinator.clear(dragIntent.serial)
        assertNull(coordinator.pendingIntent)
    }

    @Test
    fun keyboardScrollOnlyRunsWhenComposerOpensNearTimelineEnd() {
        assertTrue(
            shouldScrollTimelineForKeyboardChange(
                deltaPx = 320,
                isComposerFocused = true,
                isUserDragging = false,
                wasNearEndBeforeKeyboard = true,
            ),
        )
        assertFalse(
            shouldScrollTimelineForKeyboardChange(
                deltaPx = 320,
                isComposerFocused = true,
                isUserDragging = false,
                wasNearEndBeforeKeyboard = false,
            ),
        )
        assertFalse(
            shouldScrollTimelineForKeyboardChange(
                deltaPx = 320,
                isComposerFocused = true,
                isUserDragging = true,
                wasNearEndBeforeKeyboard = true,
            ),
        )
        assertFalse(
            shouldScrollTimelineForKeyboardChange(
                deltaPx = 0,
                isComposerFocused = true,
                isUserDragging = false,
                wasNearEndBeforeKeyboard = true,
            ),
        )
    }

    @Test
    fun followLatestBubbleRequiresUserDetachedFromLatestContent() {
        assertTrue(
            shouldShowTimelineFollowLatestBubble(
                isComposerFocused = false,
                keyboardVisible = false,
                isNearTimelineEnd = false,
                userDetachedFromLatest = true,
                followLatestActive = false,
            ),
        )
        assertFalse(
            shouldShowTimelineFollowLatestBubble(
                isComposerFocused = false,
                keyboardVisible = false,
                isNearTimelineEnd = true,
                userDetachedFromLatest = true,
                followLatestActive = false,
            ),
        )
        assertFalse(
            shouldShowTimelineFollowLatestBubble(
                isComposerFocused = false,
                keyboardVisible = false,
                isNearTimelineEnd = false,
                userDetachedFromLatest = false,
                followLatestActive = false,
            ),
        )
        assertFalse(
            shouldShowTimelineFollowLatestBubble(
                isComposerFocused = true,
                keyboardVisible = false,
                isNearTimelineEnd = false,
                userDetachedFromLatest = true,
                followLatestActive = false,
            ),
        )
        assertFalse(
            shouldShowTimelineFollowLatestBubble(
                isComposerFocused = false,
                keyboardVisible = true,
                isNearTimelineEnd = false,
                userDetachedFromLatest = true,
                followLatestActive = false,
            ),
        )
        assertFalse(
            shouldShowTimelineFollowLatestBubble(
                isComposerFocused = false,
                keyboardVisible = false,
                isNearTimelineEnd = false,
                userDetachedFromLatest = true,
                followLatestActive = true,
            ),
        )
    }

    @Test
    fun userDragPausesStreamingFollowOnlyAfterLeavingLatestContent() {
        assertFalse(
            shouldPauseTimelineFollowForUserDrag(
                isUserDragging = true,
                isNearTimelineEnd = true,
            ),
        )
        assertTrue(
            shouldPauseTimelineFollowForUserDrag(
                isUserDragging = true,
                isNearTimelineEnd = false,
            ),
        )
        assertFalse(
            shouldPauseTimelineFollowForUserDrag(
                isUserDragging = false,
                isNearTimelineEnd = false,
            ),
        )
    }

    @Test
    fun finalDecisionAutoFocusOnlyTargetsCurrentTurn() {
        assertTrue(
            shouldAutoFocusTimelineFinalDecision(
                decisionKey = "final_current",
                decisionTurnId = "turn_2",
                currentTurnId = "turn_2",
                lastFocusedDecisionKey = null,
                routeReturnSettledDecisionKey = null,
                autoFocusSuppressed = false,
                manualScrollActive = false,
            ),
        )
        assertFalse(
            shouldAutoFocusTimelineFinalDecision(
                decisionKey = "final_old",
                decisionTurnId = "turn_1",
                currentTurnId = "turn_2",
                lastFocusedDecisionKey = null,
                routeReturnSettledDecisionKey = null,
                autoFocusSuppressed = false,
                manualScrollActive = false,
            ),
        )
        assertFalse(
            shouldAutoFocusTimelineFinalDecision(
                decisionKey = "final_current",
                decisionTurnId = "turn_2",
                currentTurnId = "turn_2",
                lastFocusedDecisionKey = "final_current",
                routeReturnSettledDecisionKey = null,
                autoFocusSuppressed = false,
                manualScrollActive = false,
            ),
        )
        assertFalse(
            shouldAutoFocusTimelineFinalDecision(
                decisionKey = "final_current",
                decisionTurnId = "turn_2",
                currentTurnId = "turn_2",
                lastFocusedDecisionKey = null,
                routeReturnSettledDecisionKey = null,
                autoFocusSuppressed = false,
                manualScrollActive = true,
            ),
        )
        assertFalse(
            shouldAutoFocusTimelineFinalDecision(
                decisionKey = "final_current",
                decisionTurnId = "turn_2",
                currentTurnId = "turn_2",
                lastFocusedDecisionKey = null,
                routeReturnSettledDecisionKey = null,
                autoFocusSuppressed = false,
                manualScrollActive = false,
                userDetachedFromLatest = true,
            ),
        )
    }

    @Test
    fun manualScrollSettleBlocksOnlySystemAutoFocusIntents() {
        assertFalse(
            shouldBlockTimelineAutoFocusForManualScroll(
                intentKind = TimelineScrollIntentKind.UserSent,
                isManualScrollActive = true,
            ),
        )
        assertFalse(
            shouldBlockTimelineAutoFocusForManualScroll(
                intentKind = TimelineScrollIntentKind.RouteReturn,
                isManualScrollActive = true,
            ),
        )
        assertTrue(
            shouldBlockTimelineAutoFocusForManualScroll(
                intentKind = TimelineScrollIntentKind.AssistantStarted,
                isManualScrollActive = true,
            ),
        )
        assertTrue(
            shouldBlockTimelineAutoFocusForManualScroll(
                intentKind = TimelineScrollIntentKind.FinalDecision,
                isManualScrollActive = true,
            ),
        )
        assertTrue(
            shouldBlockTimelineAutoFocusForManualScroll(
                intentKind = TimelineScrollIntentKind.FollowLatest,
                isManualScrollActive = true,
            ),
        )
        assertFalse(
            shouldBlockTimelineAutoFocusForManualScroll(
                intentKind = TimelineScrollIntentKind.FinalDecision,
                isManualScrollActive = false,
            ),
        )
    }

    @Test
    fun assistantStartedAnchorIgnoresClarificationFlightSuppression() {
        assertFalse(
            isTimelineAutoFocusSuppressedForIntent(
                intentKind = TimelineScrollIntentKind.AssistantStarted,
                isRouteReturnSuppressed = false,
                isClarificationFlightActive = true,
            ),
        )
        assertTrue(
            isTimelineAutoFocusSuppressedForIntent(
                intentKind = TimelineScrollIntentKind.AssistantStarted,
                isRouteReturnSuppressed = true,
                isClarificationFlightActive = false,
            ),
        )
        assertTrue(
            isTimelineAutoFocusSuppressedForIntent(
                intentKind = TimelineScrollIntentKind.FollowLatest,
                isRouteReturnSuppressed = false,
                isClarificationFlightActive = true,
            ),
        )
    }

    @Test
    fun viewportFreezeOnlyRunsAfterActualScrollDrift() {
        assertFalse(
            shouldFreezeTimelineViewport(
                currentIndex = 3,
                currentOffset = 120,
                targetIndex = 3,
                targetOffset = 124,
                tolerancePx = 8f,
            ),
        )
        assertTrue(
            shouldFreezeTimelineViewport(
                currentIndex = 3,
                currentOffset = 120,
                targetIndex = 4,
                targetOffset = 120,
                tolerancePx = 8f,
            ),
        )
        assertTrue(
            shouldFreezeTimelineViewport(
                currentIndex = 3,
                currentOffset = 120,
                targetIndex = 3,
                targetOffset = 136,
                tolerancePx = 8f,
            ),
        )
    }

    @Test
    fun timelineMotionStopsChangingHeightAfterDragOrEntry() {
        assertTrue(shouldAnimateTimelineItem(animateEnter = true, hasEntered = false))
        assertFalse(shouldAnimateTimelineItem(animateEnter = false, hasEntered = false))
        assertFalse(shouldAnimateTimelineItem(animateEnter = true, hasEntered = true))

        assertTrue(shouldAnimateTurnNode(motionEnabled = true, hasStarted = false))
        assertFalse(shouldAnimateTurnNode(motionEnabled = false, hasStarted = false))
        assertFalse(shouldAnimateTurnNode(motionEnabled = true, hasStarted = true))
    }

    @Test
    fun flightUserAnchorIsConsumedWithoutRequestingASecondUserScroll() {
        assertTrue(
            shouldConsumeFlightUserAnchor(
                userMessageKey = "user_after_clarification",
                activeFlightMessageKey = "user_after_clarification",
            ),
        )
        assertFalse(
            shouldConsumeFlightUserAnchor(
                userMessageKey = "user_after_clarification",
                activeFlightMessageKey = "previous_user",
            ),
        )
        assertFalse(
            shouldConsumeFlightUserAnchor(
                userMessageKey = null,
                activeFlightMessageKey = "user_after_clarification",
            ),
        )
    }

    @Test
    fun initialCompletedTextDoesNotReplayAfterItWasLiveRevealed() {
        assertTrue(
            shouldAnimateInitialCompletedText(
                turnId = "turn_1",
                currentTurnId = "turn_1",
                revealKey = "text_1",
                revealedMessageKeys = emptySet(),
                liveRevealedMessageKeys = emptySet(),
            ),
        )
        assertFalse(
            shouldAnimateInitialCompletedText(
                turnId = "turn_1",
                currentTurnId = "turn_1",
                revealKey = "text_1",
                revealedMessageKeys = setOf("text_1"),
                liveRevealedMessageKeys = emptySet(),
            ),
        )
        assertFalse(
            shouldAnimateInitialCompletedText(
                turnId = "turn_1",
                currentTurnId = "turn_1",
                revealKey = "text_1",
                revealedMessageKeys = emptySet(),
                liveRevealedMessageKeys = setOf("text_1"),
            ),
        )
    }

    @Test
    fun timelineStructureSignatureChangesForNewNodesButNotTextContentGrowth() {
        val intro = text("intro_text")
        val textOnlyTurn = AssistantTurnTimelineItem(
            turnId = "turn_1",
            nodes = listOf(intro),
            segmentIndex = 0,
        )
        val longerTextTurn = textOnlyTurn.copy(
            nodes = listOf(intro.copy(content = intro.content + "，继续展开更多内容")),
        )
        val structuredTurn = textOnlyTurn.copy(
            nodes = listOf(intro, deck("deck_1")),
        )

        assertEquals(textOnlyTurn.structureSignature(), longerTextTurn.structureSignature())
        assertTrue(textOnlyTurn.structureSignature() != structuredTurn.structureSignature())
    }

    @Test
    fun timelineStructureSignatureChangesWhenProductDeckGrowsInPlace() {
        val intro = text("intro_text")
        val firstDeck = deck("deck_1").copy(products = listOf(product("p1")))
        val grownDeck = firstDeck.copy(products = listOf(product("p1"), product("p2")))
        val firstTurn = AssistantTurnTimelineItem(
            turnId = "turn_1",
            nodes = listOf(intro, firstDeck),
            segmentIndex = 0,
        )
        val grownTurn = firstTurn.copy(nodes = listOf(intro, grownDeck))

        assertTrue(firstTurn.structureSignature() != grownTurn.structureSignature())
    }

    @Test
    fun timelineStructureSignatureChangesWhenFinalDecisionContentGrowsInPlace() {
        val compactFinal = finalDecision("final_decision")
        val detailedFinal = compactFinal.copy(
            payload = FinalDecisionPayload(
                summary = "优先选这一款。",
                why = listOf("预算匹配", "核心功能更接近你的需求"),
                notFor = listOf("如果你更在意轻薄，可以继续看备选。"),
            ),
        )
        val compactTurn = AssistantTurnTimelineItem(
            turnId = "turn_1",
            nodes = listOf(compactFinal),
            segmentIndex = 0,
        )
        val detailedTurn = compactTurn.copy(nodes = listOf(detailedFinal))

        assertTrue(compactTurn.structureSignature() != detailedTurn.structureSignature())
    }

    @Test
    fun scrollAnchorSignatureIgnoresContentGrowthInsideStableTimelineItems() {
        val intro = text("intro_text")
        val firstDeck = deck("deck_1").copy(products = listOf(product("p1")))
        val grownDeck = firstDeck.copy(products = listOf(product("p1"), product("p2")))
        val compactFinal = finalDecision("final_decision")
        val detailedFinal = compactFinal.copy(
            payload = FinalDecisionPayload(
                summary = "优先选这一款。",
                why = listOf("预算匹配", "核心功能更接近你的需求"),
                notFor = listOf("如果你更在意轻薄，可以继续看备选。"),
            ),
        )
        val compactTurn = AssistantTurnTimelineItem(
            turnId = "turn_1",
            nodes = listOf(intro, firstDeck, compactFinal),
            segmentIndex = 0,
        )
        val grownTurn = compactTurn.copy(nodes = listOf(intro.copy(content = intro.content + "，继续展开"), grownDeck, detailedFinal))

        assertEquals(
            listOf(compactTurn).scrollAnchorSignature(),
            listOf(grownTurn).scrollAnchorSignature(),
        )
    }

    @Test
    fun scrollAnchorSignatureChangesWhenNodeShapeChanges() {
        val intro = text("intro_text")
        val deck = deck("deck_1")
        val textOnlyTurn = AssistantTurnTimelineItem(
            turnId = "turn_1",
            nodes = listOf(intro),
            segmentIndex = 0,
        )
        val deckTurn = textOnlyTurn.copy(nodes = listOf(intro, deck))

        assertTrue(
            listOf(textOnlyTurn).scrollAnchorSignature() != listOf(deckTurn).scrollAnchorSignature(),
        )
    }

    @Test
    fun routeReturnKeepsNewFinalDecisionEligibleForReadableFocus() {
        assertEquals(
            "final_before_detail",
            routeAwareFocusedFinalDecisionKey(
                currentFocusedKey = "final_before_detail",
                routeReturnAnchorCaptured = true,
                capturedFinalDecisionKey = "final_before_detail",
                latestFinalDecisionKey = "final_after_convergence",
            ),
        )
        assertEquals(
            null,
            routeAwareFocusedFinalDecisionKey(
                currentFocusedKey = null,
                routeReturnAnchorCaptured = true,
                capturedFinalDecisionKey = null,
                latestFinalDecisionKey = "final_after_convergence",
            ),
        )
        assertEquals(
            "final_after_convergence",
            routeAwareFocusedFinalDecisionKey(
                currentFocusedKey = "final_after_convergence",
                routeReturnAnchorCaptured = false,
                capturedFinalDecisionKey = null,
                latestFinalDecisionKey = "final_after_convergence",
            ),
        )
    }

    @Test
    fun routeReturnAnchorUsesAnchoredItemTurnInsteadOfGlobalCurrentTurn() {
        val oldDeck = deck("deck_old").copy(turnId = "turn_old")
        val oldTurn = AssistantTurnTimelineItem(
            turnId = "turn_old",
            nodes = listOf(oldDeck),
            segmentIndex = 0,
        )
        val newTurn = AssistantTurnTimelineItem(
            turnId = "turn_new",
            nodes = listOf(text("text_new").copy(turnId = "turn_new")),
            segmentIndex = 0,
        )

        assertEquals(
            "turn_old",
            routeReturnAnchorTurnId(
                timelineItems = listOf(oldTurn, newTurn),
                visibleItemIndex = 1,
                anchorNodeKey = oldDeck.key,
                currentTurnId = "turn_new",
            ),
        )
    }

    @Test
    fun timelineStructureSignatureChangesWhenCartOrErrorContentChanges() {
        val cartPending = StandaloneTimelineItem(
            CartActionNode(
                key = "cart_action",
                payload = CartActionPayload(action = "add", productId = "p1", status = "pending"),
            ),
        )
        val cartSuccess = StandaloneTimelineItem(
            CartActionNode(
                key = "cart_action",
                payload = CartActionPayload(
                    action = "add",
                    productId = "p1",
                    status = "success",
                    cart = CartSummaryPayload(totalItems = 2),
                ),
            ),
        )
        val shortError = StandaloneTimelineItem(
            ErrorNode(key = "error_1", code = "NETWORK", message = "失败", retryable = true),
        )
        val longError = StandaloneTimelineItem(
            ErrorNode(key = "error_1", code = "NETWORK", message = "连接失败，请稍后重试", retryable = true),
        )

        assertTrue(cartPending.structureSignature() != cartSuccess.structureSignature())
        assertTrue(shortError.structureSignature() != longError.structureSignature())
    }

    @Test
    fun updateTextProgressKeepsLatestProgressWithoutSnapshottingEveryCharacter() {
        val store = TimelineRevealStore()

        store.updateTextProgress(key = "text_1", visible = 1, total = 100)
        store.updateTextProgress(key = "text_1", visible = 2, total = 100)

        assertEquals(1, store.textRevealProgressByKey["text_1"]?.visibleLength)
        assertEquals(2, store.textRevealProgress("text_1")?.visibleLength)
        assertTrue(store.hasLiveRevealedText("text_1"))

        store.updateTextProgress(key = "text_1", visible = 25, total = 100)

        assertEquals(25, store.textRevealProgressByKey["text_1"]?.visibleLength)
        assertEquals(25, store.textRevealProgress("text_1")?.visibleLength)
    }

    @Test
    fun visibilityProgressUsesLatestPrivateTextProgressInsteadOfSnapshotCheckpoint() {
        val store = TimelineRevealStore()
        val text = text("text_1", content = "这里是一段很长很长的流式文字，用来验证回到瀑布区时不会倒退")

        store.updateTextProgress(key = text.key, visible = 1, total = 100)
        store.updateTextProgress(key = text.key, visible = 2, total = 100)

        val progress = listOf<ChatUiNode>(text).textRevealProgressForVisibility(
            revealStore = store,
            liveRevealedMessageKeys = emptySet(),
        )

        assertEquals(1, store.textRevealProgressByKey[text.key]?.visibleLength)
        assertEquals(2, progress[text.key]?.visibleLength)
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
        assertTrue(store.hasLiveRevealedText("old_text"))

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
        assertTrue(store.hasLiveRevealedText("text_1"))
        assertFalse(store.hasLiveRevealedText("old_text"))
    }

    @Test
    fun textAfterThinkingKeepsThinkingVisibleUntilTextStarts() {
        val thinking = thinking("thinking_search")
        val text = text("intro_text")
        val state = listOf(thinking, text).visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )

        assertTrue(thinking.key in state.visibleNodeKeys)
        assertTrue(text.key in state.visibleNodeKeys)
        assertTrue(text.key in state.textHandoffKeys)
    }

    @Test
    fun blankTextAfterThinkingKeepsThinkingInOwnSlotAndDoesNotStartHandoff() {
        val thinking = thinking("thinking_search")
        val blankText = text("intro_text", content = "")
        val state = listOf(thinking, blankText).visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )

        assertTrue(thinking.key in state.visibleNodeKeys)
        assertFalse(blankText.key in state.visibleNodeKeys)
        assertFalse(blankText.key in state.textHandoffKeys)
        assertTrue(listOf<ChatUiNode>(thinking, blankText).shouldRenderThinkingNodeInOwnSlot(0))
    }

    @Test
    fun blankTextNodeDoesNotBlockFollowingStructuredCard() {
        val blankText = text("empty_intro", content = "")
        val deck = deck("deck_after_empty_text")
        val state = listOf<ChatUiNode>(blankText, deck).visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )

        assertFalse(blankText.key in state.visibleNodeKeys)
        assertTrue(deck.key in state.visibleNodeKeys)
    }

    @Test
    fun blankThinkingDoesNotDelayFollowingText() {
        val thinking = thinking("thinking_empty", message = "")
        val text = text("intro_text")
        val state = listOf(thinking, text).visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )

        assertFalse(thinking.key in state.visibleNodeKeys)
        assertTrue(text.key in state.visibleNodeKeys)
        assertFalse(text.key in state.textHandoffKeys)
    }

    @Test
    fun textAfterThinkingHidesThinkingAfterTextStarts() {
        val thinking = thinking("thinking_search")
        val text = text("intro_text")
        val state = listOf(thinking, text).visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = mapOf(text.key to TextRevealProgress(visibleLength = 1, totalLength = 20)),
            enteredStructuredKeys = emptySet(),
        )

        assertFalse(thinking.key in state.visibleNodeKeys)
        assertTrue(text.key in state.visibleNodeKeys)
        assertFalse(text.key in state.textHandoffKeys)
    }

    @Test
    fun textAfterThinkingDoesNotBringThinkingBackWhenLiveProgressWasRestored() {
        val thinking = thinking("thinking_search")
        val text = text("intro_text")
        val restoredLiveProgress = TextRevealProgress(
            visibleLength = text.content.length,
            totalLength = text.content.length,
        )
        val state = listOf(thinking, text).visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = mapOf(text.key to restoredLiveProgress),
            enteredStructuredKeys = emptySet(),
        )

        assertFalse(thinking.key in state.visibleNodeKeys)
        assertTrue(text.key in state.visibleNodeKeys)
        assertFalse(text.key in state.textHandoffKeys)
    }

    @Test
    fun genericThinkingUsesBackendTextBeforeBackendContentArrives() {
        val thinking = thinking("thinking_understanding", stage = "understanding", message = "正在理解您的需求...")
        val waitingState = listOf(thinking).visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )
        val text = text("intro_text")
        val handoffState = listOf(thinking, text).visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )

        assertTrue(thinking.key in waitingState.visibleNodeKeys)
        assertTrue(thinking.key in handoffState.visibleNodeKeys)
        assertTrue(text.key in handoffState.visibleNodeKeys)
    }

    @Test
    fun thinkingBeforeTextRevealNodeUsesHandoffSlotInsteadOfRenderingTwice() {
        val thinking = thinking("thinking_clarify", stage = "clarification", message = "正在确认你的预算范围...")
        val clarification = clarification("clarify_budget")
        val text = text("intro_text")
        val deck = deck("deck_1")

        assertFalse(listOf<ChatUiNode>(thinking, clarification).shouldRenderThinkingNodeInOwnSlot(0))
        assertFalse(listOf<ChatUiNode>(thinking, text).shouldRenderThinkingNodeInOwnSlot(0))
        assertFalse(listOf<ChatUiNode>(thinking, deck).shouldRenderThinkingNodeInOwnSlot(0))
        assertTrue(listOf<ChatUiNode>(thinking).shouldRenderThinkingNodeInOwnSlot(0))
    }

    @Test
    fun thinkingBeforeStructuredCardStaysVisibleUntilCardEntered() {
        val thinking = thinking("thinking_search", stage = "searching", message = "正在检索匹配商品...")
        val deck = deck("deck_1")
        val nodes = listOf<ChatUiNode>(thinking, deck)

        val beforeDeckEntered = nodes.visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )
        val afterDeckEntered = nodes.visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = setOf(deck.key),
        )

        assertTrue(thinking.key in beforeDeckEntered.visibleNodeKeys)
        assertTrue(deck.key in beforeDeckEntered.visibleNodeKeys)
        assertTrue(deck.key in beforeDeckEntered.structuredHandoffKeys)
        assertFalse(thinking.key in afterDeckEntered.visibleNodeKeys)
        assertTrue(deck.key in afterDeckEntered.visibleNodeKeys)
        assertFalse(deck.key in afterDeckEntered.structuredHandoffKeys)
    }

    @Test
    fun clarificationQuestionAfterThinkingUsesHandoffUntilQuestionStarts() {
        val thinking = thinking("thinking_clarify", stage = "clarification", message = "正在确认你的预算范围...")
        val clarification = clarification("clarify_budget")
        val questionKey = "${clarification.key}_question"

        val beforeQuestionStarts = listOf(thinking, clarification).visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )
        val afterQuestionStarts = listOf(thinking, clarification).visibleTurnNodeKeys(
            completedTextKeys = emptySet(),
            textRevealProgress = mapOf(questionKey to TextRevealProgress(visibleLength = 1, totalLength = 12)),
            enteredStructuredKeys = emptySet(),
        )

        assertTrue(thinking.key in beforeQuestionStarts.visibleNodeKeys)
        assertTrue(clarification.key in beforeQuestionStarts.visibleNodeKeys)
        assertTrue(questionKey in beforeQuestionStarts.textHandoffKeys)
        assertFalse(thinking.key in afterQuestionStarts.visibleNodeKeys)
        assertTrue(clarification.key in afterQuestionStarts.visibleNodeKeys)
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
    fun followingTextKeepsIntermediateThinkingUntilHandoffStarts() {
        val intro = text("intro_text")
        val thinking = thinking("thinking_decision")
        val nextText = text("decision_text")
        val nodes = listOf(intro, thinking, nextText)

        val state = nodes.visibleTurnNodeKeys(
            completedTextKeys = setOf(intro.key),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )

        assertTrue(thinking.key in state.visibleNodeKeys)
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
    fun productDeckWaitsForTextCompletionBeforeEntering() {
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
        assertFalse(deck.key in afterIntroGate.visibleNodeKeys)
        assertFalse(final.key in afterIntroGate.visibleNodeKeys)
        assertFalse(deck.key in afterDeckEnteredBeforeTextDone.visibleNodeKeys)
        assertFalse(final.key in afterDeckEnteredBeforeTextDone.visibleNodeKeys)
        assertTrue(deck.key in afterDeckEnteredAndTextDone.visibleNodeKeys)
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
        assertTrue(state.structuredHandoffKeys.isEmpty())
    }

    @Test
    fun multiProductDeckAutoClosesOnlyAfterAllCandidatesAreHandled() {
        val products = listOf(
            product("p1"),
            product("p2"),
            product("p3"),
        )

        assertFalse(
            isProductDeckFullyHandled(
                products = products,
                swipeState = ProductSwipeState(swipedProductIds = listOf("p1", "p2")),
            ),
        )
        assertTrue(
            isProductDeckFullyHandled(
                products = products,
                swipeState = ProductSwipeState(swipedProductIds = listOf("p1", "p2", "p3")),
            ),
        )
        assertFalse(
            isProductDeckFullyHandled(
                products = listOf(product("single")),
                swipeState = ProductSwipeState(swipedProductIds = listOf("single")),
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
    fun singleCandidateDeckLayoutDependsOnlyOnProductCount() {
        assertTrue(isSingleCandidateDeck(1))
        assertFalse(isSingleCandidateDeck(0))
        assertFalse(isSingleCandidateDeck(2))
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
        val unrelatedFinal = finalDecision("final_decision_other_deck", deckId = "deck_2")
        val final = finalDecision("final_decision", deckId = "deck_1")

        assertFalse(ChatUiState(nodes = listOf(deck, unrelatedFinal)).hasConvergedDecisionForDeck("deck_1"))
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
    fun assistantTurnKeyStaysStableWhenTransientThinkingIsRemoved() {
        val thinking = thinking("thinking_search")
        val text = text("intro_text")
        val withThinking = listOf<ChatUiNode>(thinking, text)
            .toTimelineRenderItems()
            .filterIsInstance<AssistantTurnTimelineItem>()
            .single()
        val withoutThinking = listOf<ChatUiNode>(text)
            .toTimelineRenderItems()
            .filterIsInstance<AssistantTurnTimelineItem>()
            .single()

        assertEquals(withThinking.key, withoutThinking.key)
    }

    @Test
    fun timelinePresentationCarriesRenderContextOffComposition() {
        val clarification = clarification("clarify_budget")
        val deck = deck("deck_1")
        val final = finalDecision("final_decision", deckId = "deck_1")
        val swipeState = ProductSwipeState(currentProductId = "p1")
        val state = ChatUiState(
            nodes = listOf(clarification, deck, final),
            backendBaseUrl = "http://localhost:8000",
            isStreaming = true,
            currentTurnId = "turn_1",
            productSwipeStates = mapOf("deck_1" to swipeState),
            awaitingConvergenceDeckIds = setOf("deck_1"),
            latestConvergeableDeckId = "deck_1",
            lastUserMessage = "帮我选",
        )

        val presentation = state.toTimelinePresentationState()

        assertTrue(presentation.hasNodes)
        assertTrue(presentation.hasStructuredContent)
        assertEquals(final.payload, presentation.latestFinalDecisionPayload)
        assertEquals("deck_1", presentation.latestFinalDecisionDeckId)
        assertEquals(listOf("clarify_budget"), presentation.clarificationKeys)
        assertEquals("deck_1", presentation.productDeckIdByProductId["p1"])
        assertEquals(deck, presentation.productDeckNodeByKey["deck_1"])
        assertEquals("deck_1", presentation.finalDecisionSourceDeckKeyByDecisionKey["final_decision"])
        assertEquals(setOf("deck_1"), presentation.convergedDeckIds)
        assertEquals(setOf("deck_1"), presentation.convergedProductDeckKeys)
        assertSame(presentation.productsById, presentation.renderContext.productsById)
        assertSame(presentation.productDeckIdByProductId, presentation.renderContext.productDeckIdByProductId)
        assertSame(presentation.productDeckNodeByKey, presentation.renderContext.productDeckNodeByKey)
        assertSame(
            presentation.finalDecisionSourceDeckKeyByDecisionKey,
            presentation.renderContext.finalDecisionSourceDeckKeyByDecisionKey,
        )
        assertEquals("http://localhost:8000", presentation.renderContext.backendBaseUrl)
        assertEquals(true, presentation.renderContext.isStreaming)
        assertEquals("turn_1", presentation.renderContext.currentTurnId)
        assertEquals(swipeState, presentation.renderContext.productSwipeStates["deck_1"])
        assertEquals(setOf("deck_1"), presentation.renderContext.awaitingConvergenceDeckIds)
        assertEquals("deck_1", presentation.renderContext.latestConvergeableDeckId)
        assertEquals("帮我选", presentation.renderContext.lastUserMessage)
    }

    @Test
    fun timelinePresentationScopesConvergedStateToExactProductDeckNode() {
        val oldDeck = ProductDeckNode(
            key = "deck_1",
            deckId = "deck_1",
            products = listOf(product("p1"), product("p2")),
            turnId = "turn_1",
        )
        val oldDecision = finalDecision("final_old", deckId = "deck_1")
        val newDeck = ProductDeckNode(
            key = "deck_1_turn_2",
            deckId = "deck_1",
            products = listOf(product("p3"), product("p4")),
            turnId = "turn_2",
        )

        val presentation = ChatUiState(nodes = listOf(oldDeck, oldDecision, newDeck)).toTimelinePresentationState()

        assertEquals(setOf("deck_1"), presentation.convergedDeckIds)
        assertEquals(setOf("deck_1"), presentation.convergedProductDeckKeys)
        assertEquals(setOf("deck_1"), presentation.renderContext.convergedProductDeckKeys)
        assertEquals("deck_1", presentation.finalDecisionSourceDeckKeyByDecisionKey["final_old"])
        assertEquals(oldDeck, presentation.productDeckNodeByKey["deck_1"])
        assertEquals(newDeck, presentation.productDeckNodeByKey["deck_1_turn_2"])
        assertEquals("deck_1_turn_2", presentation.latestProductDeckKey)
    }

    @Test
    fun timelinePresentationBindsEachDecisionToNearestPreviousDeckWithSameDeckId() {
        val firstDeck = ProductDeckNode(
            key = "deck_1",
            deckId = "deck_1",
            products = listOf(product("p1"), product("p2")),
            turnId = "turn_1",
        )
        val firstDecision = finalDecision("final_1", deckId = "deck_1")
        val secondDeck = ProductDeckNode(
            key = "deck_1_turn_2",
            deckId = "deck_1",
            products = listOf(product("p3"), product("p4")),
            turnId = "turn_2",
        )
        val secondDecision = finalDecision("final_2", deckId = "deck_1")

        val presentation = ChatUiState(
            nodes = listOf(firstDeck, firstDecision, secondDeck, secondDecision),
        ).toTimelinePresentationState()

        assertEquals("deck_1", presentation.finalDecisionSourceDeckKeyByDecisionKey["final_1"])
        assertEquals("deck_1_turn_2", presentation.finalDecisionSourceDeckKeyByDecisionKey["final_2"])
        assertEquals(setOf("deck_1", "deck_1_turn_2"), presentation.convergedProductDeckKeys)
    }

    @Test
    fun revealedTextKeysCountAsCompletedAfterTimelineRecreation() {
        val intro = text("intro_text")
        val deck = deck("deck_1")
        val nodes = listOf<ChatUiNode>(intro, deck)

        val state = nodes.visibleTurnNodeKeys(
            completedTextKeys = setOf(intro.key),
            textRevealProgress = emptyMap(),
            enteredStructuredKeys = emptySet(),
        )

        assertTrue(intro.key in state.visibleNodeKeys)
        assertTrue(deck.key in state.visibleNodeKeys)
        assertTrue(state.textHandoffKeys.isEmpty())
    }

    @Test
    fun productDeckHistoryKeepsOldTurnShapeWhileNewTurnIsStreaming() {
        val previousDeck = deck("deck_previous", turnId = "turn_1")
        val currentDeck = deck("deck_current", turnId = "turn_2")

        assertFalse(
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
    fun productDeckHistoryKeepsDeckShapeAfterStreamingSettles() {
        val previousDeck = deck("deck_previous", turnId = "turn_1")
        val latestDeck = deck("deck_latest", turnId = "turn_2")

        assertFalse(
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
    fun productDeckHistoryKeepsLatestDeckShapeAfterDecisionAppears() {
        val latestDeck = deck("deck_latest", turnId = "turn_1")

        assertFalse(
            shouldCompactProductDeckHistory(
                node = latestDeck,
                deckConverged = true,
                isStreaming = false,
                currentTurnId = null,
                latestProductDeckKey = latestDeck.key,
            ),
        )
        assertFalse(
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
    fun productDeckLookupUsesLatestDeckWhenBackendReusesDeckId() {
        val oldDeck = ProductDeckNode(
            key = "deck_1",
            deckId = "deck_1",
            products = listOf(product("p1"), product("p2")),
            turnId = "turn_1",
        )
        val oldDecision = finalDecision("final_old", deckId = "deck_1")
        val newDeck = ProductDeckNode(
            key = "deck_1_turn_2",
            deckId = "deck_1",
            products = listOf(product("p3"), product("p4")),
            turnId = "turn_2",
        )
        val state = ChatUiState(nodes = listOf(oldDeck, oldDecision, newDeck))

        assertEquals("deck_1_turn_2", state.findProductDeck("deck_1")?.key)
        assertEquals("p3", state.findProduct("deck_1", "p3")?.product?.productId)
        assertEquals("p1", state.findProduct("deck_1", "p1")?.product?.productId)
        assertTrue(state.isProductInDeck("deck_1", "p3"))
        assertFalse(state.isProductInDeck("deck_1", "p1"))
        assertFalse(state.hasConvergedDecisionForDeck("deck_1"))
        assertTrue(state.canOpenDeckForConvergence("deck_1").not())
    }

    @Test
    fun productDeckLookupCanPinHistoricalDeckWhenBackendReusesDeckId() {
        val oldDeck = ProductDeckNode(
            key = "deck_1",
            deckId = "deck_1",
            products = listOf(product("p1"), product("p2")),
            turnId = "turn_1",
        )
        val oldDecision = finalDecision("final_old", deckId = "deck_1")
        val newDeck = ProductDeckNode(
            key = "deck_1_turn_2",
            deckId = "deck_1",
            products = listOf(product("p3"), product("p4")),
            turnId = "turn_2",
        )
        val state = ChatUiState(
            nodes = listOf(oldDeck, oldDecision, newDeck),
            latestConvergeableDeckId = "deck_1",
            awaitingConvergenceDeckIds = setOf("deck_1"),
        )

        assertEquals("deck_1", state.findProductDeck("deck_1", "deck_1")?.key)
        assertEquals("p1", state.findProduct("deck_1", "p1", "deck_1")?.product?.productId)
        assertNull(state.findProduct("deck_1", "p3", "deck_1"))
        assertTrue(state.isProductInDeck("deck_1", "p1", "deck_1"))
        assertFalse(state.isProductInDeck("deck_1", "p3", "deck_1"))
        assertTrue(state.hasConvergedDecisionForDeck("deck_1", "deck_1"))
        assertFalse(state.canOpenDeckForConvergence("deck_1", "deck_1"))
    }

    @Test
    fun latestDeckCountsAsConvergedOnlyWhenDecisionAppearsAfterIt() {
        val deck = ProductDeckNode(
            key = "deck_1_turn_2",
            deckId = "deck_1",
            products = listOf(product("p3"), product("p4")),
            turnId = "turn_2",
        )
        val decision = finalDecision("final_new", deckId = "deck_1")
        val state = ChatUiState(nodes = listOf(deck, decision))

        assertTrue(state.hasConvergedDecisionForDeck("deck_1"))
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
    fun decisionSummaryDividerUsesNativeMarkdownRendererWithoutAndroidViewSwap() {
        val content = "---\n\n优先选小米 17 Ultra。"
        val rendered = content.toNativeMarkdownAnnotatedString()

        assertTrue(content.needsFinalMarkdownRender())
        assertFalse(content.requiresAndroidMarkdownRender())
        assertEquals("────────────\n优先选小米 17 Ultra。", rendered.text)
        assertTrue(rendered.spanStyles.isNotEmpty())
    }

    @Test
    fun decisionMetaLabelsNeverExposeBackendTokens() {
        assertEquals("低", "low".confidenceLabel())
        assertEquals("下一步：继续看候选", "continue_current_deck".nextStepLabel())
        assertEquals("待确认", "unknown_confidence".confidenceLabel())
        assertEquals("下一步：继续补充偏好", "backend_next_step".nextStepLabel())
    }

    @Test
    fun technicalErrorMessagesUseChineseFallbackReason() {
        val fallback = "当前连接不稳定，商品证据或模型回复没有完整返回。"

        assertEquals(fallback, "Failed to connect to /chat/stream".userFacingErrorReason(fallback))
        assertEquals(fallback, "HTTP 500 NETWORK_ERROR".userFacingErrorReason(fallback))
        assertEquals(fallback, """{"detail":"timeout"}""".userFacingErrorReason(fallback))
        assertEquals(
            fallback,
            "本轮导购处理失败，请稍后重试。 trace_id=android-turn-1da6b6ab-926e-46e8-862d-1db09c1f09f0"
                .userFacingErrorReason(fallback),
        )
        assertEquals("图片主体不够清晰。", "图片主体不够清晰。".userFacingErrorReason(fallback))
    }

    @Test
    fun thinkingStatusUsesBackendTextWithoutFrontendFallbackCopy() {
        assertEquals(
            "正在理解您的需求...",
            ThinkingPayload(stage = "understanding", message = "正在理解您的需求...").userFacingThinkingMessage(),
        )
        assertEquals(
            "",
            ThinkingPayload(stage = "intent_analysis", message = "").userFacingThinkingMessage(),
        )
        assertEquals(
            "",
            ThinkingPayload(stage = "searching", message = "Searching products").userFacingThinkingMessage(),
        )
        assertEquals(
            "",
            ThinkingPayload(stage = "backend_phase", message = "fallback_decision").userFacingThinkingMessage(),
        )
        assertEquals(
            "",
            ThinkingPayload(stage = "ranking", message = "").userFacingThinkingMessage(),
        )
        assertEquals(
            "正在确认你的预算范围...",
            ThinkingPayload(stage = "clarification", message = "正在确认你的预算范围...").userFacingThinkingMessage(),
        )
    }

    @Test
    fun streamedTextKeepsPlainRendererAfterCompletion() {
        assertTrue(
            shouldKeepPlainTextRendererAfterStreaming(
                stablePlainAfterLiveReveal = false,
                hasSeenLiveStream = true,
                animateInitialCompleted = false,
            ),
        )
        assertTrue(
            shouldKeepPlainTextRendererAfterStreaming(
                stablePlainAfterLiveReveal = false,
                hasSeenLiveStream = false,
                animateInitialCompleted = true,
            ),
        )
        assertFalse(
            shouldKeepPlainTextRendererAfterStreaming(
                stablePlainAfterLiveReveal = false,
                hasSeenLiveStream = false,
                animateInitialCompleted = false,
            ),
        )
    }

    @Test
    fun streamedTextRestoresCurrentLengthAfterLiveRevealInsteadOfReplaying() {
        assertEquals(
            18,
            initialStreamingVisibleLength(
                contentLength = 18,
                revealVisibleLength = null,
                alreadyCompleted = false,
                stablePlainAfterLiveReveal = true,
            ),
        )
        assertEquals(
            7,
            initialStreamingVisibleLength(
                contentLength = 18,
                revealVisibleLength = 7,
                alreadyCompleted = false,
                stablePlainAfterLiveReveal = false,
            ),
        )
        assertEquals(
            18,
            initialStreamingVisibleLength(
                contentLength = 18,
                revealVisibleLength = 7,
                alreadyCompleted = true,
                stablePlainAfterLiveReveal = false,
            ),
        )
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

    @Test
    fun productSeedShimmerStopsAfterArrivalSettles() {
        assertTrue(shouldRenderProductImageLoadingSeed(0f))
        assertTrue(shouldRenderProductImageLoadingSeed(0.5f))
        assertFalse(shouldRenderProductImageLoadingSeed(0.99f))
        assertFalse(shouldRenderProductImageLoadingSeed(1f))

        assertFalse(shouldRenderProductArrivalSeedBubble(0f))
        assertTrue(shouldRenderProductArrivalSeedBubble(0.5f))
        assertTrue(shouldRenderProductArrivalSeedBubble(1f))
    }

    private fun thinking(
        key: String,
        stage: String = key,
        message: String = "正在检索匹配商品...",
    ) = ThinkingNode(
        key = key,
        payload = ThinkingPayload(stage = stage, message = message),
        turnId = "turn_1",
    )

    private fun text(
        key: String,
        content: String = "这里是一段正在展开的文字",
    ) = AiStreamNode(
        key = key,
        messageId = key,
        content = content,
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

    private fun finalDecision(key: String, deckId: String? = null) = FinalDecisionNode(
        key = key,
        payload = FinalDecisionPayload(summary = "done"),
        turnId = "turn_1",
        deckId = deckId,
    )

    private fun product(productId: String) = ProductCardPayload(
        product = ProductPayload(productId = productId, name = productId),
    )
}
