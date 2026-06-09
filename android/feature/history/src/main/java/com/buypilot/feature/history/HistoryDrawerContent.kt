package com.buypilot.feature.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buypilot.core.data.SessionSummary

private val HistoryEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val DrawerBackground = Color(0xFFFCFCFD)
private val DrawerSurface = Color(0xFFFEFEFF)
private val DrawerSurfaceMuted = Color(0xFFF3F4F6)
private val DrawerPrimarySoft = Color(0xFFFFF0EA)
private val DrawerSelected = Color(0xFFF0F1F3)
private val DrawerPrimary = Color(0xFFFF6A3D)
private val DrawerDelete = Color(0xFFE84C3D)
private val DrawerTextPrimary = Color(0xFF17191D)
private val DrawerTextSecondary = Color(0xFF4E5561)
private val DrawerTextMuted = Color(0xFF929BAA)
private val DrawerRowShape = RoundedCornerShape(15.dp)
private val DrawerMenuShape = RoundedCornerShape(22.dp)

@Composable
fun HistoryDrawerContent(
    state: HistoryUiState,
    currentSessionId: String?,
    actionsEnabled: Boolean,
    onNewChat: () -> Unit,
    onSessionSelected: (String) -> Unit,
    onSessionPinToggled: (String) -> Unit,
    onSessionDeleted: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(DrawerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.history_drawer_title),
                color = DrawerTextPrimary,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = if (actionsEnabled) DrawerPrimarySoft else DrawerSurfaceMuted.copy(alpha = 0.52f),
                shape = CircleShape,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(enabled = actionsEnabled, role = Role.Button, onClick = onNewChat),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.history_new_chat),
                        tint = if (actionsEnabled) DrawerPrimary else DrawerTextMuted,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .size(width = 26.dp, height = 3.dp)
                .background(DrawerPrimary, RoundedCornerShape(100.dp))
                .alpha(0.72f),
        )

        AnimatedVisibility(visible = !actionsEnabled) {
            Text(
                text = stringResource(R.string.history_disabled_hint),
                color = DrawerTextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        if (state.sessions.isEmpty()) {
            HistoryEmptyState(modifier = Modifier.weight(1f))
        } else {
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 16.dp)
                        .background(DrawerPrimary, RoundedCornerShape(100.dp))
                        .alpha(0.66f),
                )
                Text(
                    text = stringResource(R.string.history_recents),
                    color = DrawerTextPrimary,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                items(state.sessions, key = { it.sessionId }) { session ->
                    ContextMenuSessionRow(
                        session = session,
                        selected = session.sessionId == currentSessionId,
                        pinned = session.sessionId in state.pinnedSessionIds,
                        enabled = actionsEnabled,
                        onClick = { onSessionSelected(session.sessionId) },
                        onPinToggle = { onSessionPinToggled(session.sessionId) },
                        onDelete = { onSessionDeleted(session.sessionId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.alpha(0.92f),
        ) {
            Image(
                painter = painterResource(R.drawable.redbean_bun_character_02),
                contentDescription = null,
                modifier = Modifier.size(104.dp),
            )
            Text(
                text = stringResource(R.string.history_empty_title),
                color = DrawerTextPrimary,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContextMenuSessionRow(
    session: SessionSummary,
    selected: Boolean,
    pinned: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember(session.sessionId) { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        HistorySessionRow(
            session = session,
            selected = selected,
            pinned = pinned,
            enabled = enabled,
            modifier = Modifier.combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = { menuExpanded = true },
            ),
        )
        HistorySessionMenu(
            expanded = menuExpanded,
            pinned = pinned,
            onDismiss = { menuExpanded = false },
            onPinToggle = {
                menuExpanded = false
                onPinToggle()
            },
            onDelete = {
                menuExpanded = false
                onDelete()
            },
        )
    }
}

@Composable
private fun HistorySessionMenu(
    expanded: Boolean,
    pinned: Boolean,
    onDismiss: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = 26.dp, y = (-4).dp),
        containerColor = DrawerSurface,
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        shape = DrawerMenuShape,
        modifier = Modifier.width(218.dp),
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (pinned) {
                            R.string.history_unpin_session
                        } else {
                            R.string.history_pin_session
                        },
                    ),
                    color = DrawerTextPrimary,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = null,
                    tint = DrawerTextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            },
            onClick = onPinToggle,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.history_delete_session),
                    color = DrawerDelete,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = DrawerDelete,
                    modifier = Modifier.size(22.dp),
                )
            },
            onClick = onDelete,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun HistorySessionRow(
    session: SessionSummary,
    selected: Boolean,
    pinned: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) DrawerSelected else DrawerSurface,
        animationSpec = tween(durationMillis = 240, easing = HistoryEase),
        label = "history_row_container",
    )
    val rowAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.58f,
        animationSpec = tween(durationMillis = 180, easing = HistoryEase),
        label = "history_row_alpha",
    )

    Surface(
        color = containerColor,
        shape = DrawerRowShape,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                alpha = rowAlpha
            }
            .clip(DrawerRowShape)
            .then(modifier),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = session.title.ifBlank {
                    session.lastMessage.ifBlank { stringResource(R.string.history_new_chat) }
                },
                color = if (selected) DrawerTextPrimary else DrawerTextSecondary,
                fontSize = 18.sp,
                lineHeight = 25.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(visible = pinned) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = stringResource(R.string.history_pinned_desc),
                    tint = DrawerPrimary.copy(alpha = 0.76f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
