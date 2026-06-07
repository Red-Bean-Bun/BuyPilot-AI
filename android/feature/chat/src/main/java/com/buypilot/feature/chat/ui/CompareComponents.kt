package com.buypilot.feature.chat.ui

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buypilot.core.model.CompareAxisPayload
import com.buypilot.core.model.CompareAxisValuePayload
import com.buypilot.core.model.CompareCardPayload
import com.buypilot.core.model.CompareRiskNotePayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.feature.chat.R
import com.buypilot.feature.chat.model.ProductDeckNode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay

private val MarkdownCompareMetricColumnWidth = 86.dp
private val MarkdownCompareProductColumnWidth = 148.dp
private val CompareProductFillerWordsRegex =
    Regex("(?i)\\b(搭载|采用|支持|适合|专为|官方|新品|全新)\\b")
private val CompareWhitespaceRegex = Regex("\\s+")
private val CompareNameSplitRegex = Regex("[，,（(\\s]+")
private val CompareNameTokenSplitRegex = Regex("[，,、（()\\s/｜|·\\-]+")
private val ComparePriceNumberRegex = Regex("""\d+(?:\.\d+)?""")
private val CompareAspectPhraseRegex = Regex("在\\s*方面")
private val CompareAdvantagePhraseRegex = Regex("(方面)?更有优势")
private val CompareBetterPhraseRegex = Regex("(方面)?表现更好")
private val CompareGenericCapabilityRegex = Regex("(核心参数|参数|配置|表现|能力)")
private val CompareTrailingPunctuationRegex = Regex("[。；;，,]+$")
private val CompareQuestionLineRegex = Regex("(?i)Q[:：].*")
private val CompareAnswerMarkerRegex = Regex("(?i)A[:：]")
private val CompareDesignedForRegex = Regex("(是)?专为")
private val CompareAudienceBuiltRegex = Regex("(爱好者|用户)打造")
private val CompareMarketingVerbRegex = Regex("(带来|提供|支持|搭配|采用|载全新|全新)")
private val ComparePunctuationSpacingRegex = Regex("[；;，,。]+")
private val CompareCellPunctuationRegex = Regex("[；;，,、\\s]")
private val CompareDimensionAdvantageRegex = Regex("(方面|维度)更有优势")
private val CompareInAspectRegex = Regex("在(.{1,8})方面")
private val CompareDedupCharsRegex = Regex("[^\\p{IsHan}A-Za-z0-9]")
private val CompareDedupWordsRegex = Regex("(更有优势|更占优|更优|表现更好|方面|维度)")
private val CompareAliasGenericWordsRegex = Regex("(?i)\\b(官方|新品|全新)\\b")
private val CompareAliasSplitRegex = Regex("[，,（(]")

@Composable
internal fun CompareSelectionTray(
    deck: ProductDeckNode?,
    backendBaseUrl: String,
    visible: Boolean,
    requestVersion: Int,
    composerBottomPadding: Dp,
    onDismiss: () -> Unit,
    onStartCompare: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val products = deck?.products.orEmpty()
    var selectedRanks by rememberSaveable(requestVersion, deck?.key) {
        mutableStateOf(products.take(2).mapIndexed { index, _ -> index + 1 })
    }
    LaunchedEffect(products.size, visible) {
        if (visible && selectedRanks.size < 2 && products.size >= 2) {
            selectedRanks = listOf(1, 2)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 4 },
        exit = fadeOut(tween(130)) +
            slideOutVertically(tween(150, easing = FastOutSlowInEasing)) { it / 4 },
        modifier = modifier.padding(bottom = composerBottomPadding + 8.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = BuyPilotColors.ShadowNeutral.copy(alpha = 0.08f)),
            color = BuyPilotColors.SurfaceCard.copy(alpha = 0.98f),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.7f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.compare_picker_title),
                            color = BuyPilotColors.TextPrimary,
                            fontSize = BuyPilotType.LargeBody,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.compare_picker_subtitle),
                            color = BuyPilotColors.TextMuted,
                            fontSize = BuyPilotType.Label,
                            lineHeight = 16.sp,
                        )
                    }
                    TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(stringResource(R.string.common_cancel), color = BuyPilotColors.TextSecondary, fontSize = BuyPilotType.Label)
                    }
                }

                if (deck == null || products.size < 2) {
                    Text(
                        text = stringResource(R.string.compare_picker_unavailable),
                        color = BuyPilotColors.TextMuted,
                        fontSize = BuyPilotType.Body,
                        lineHeight = 20.sp,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        products.take(4).forEachIndexed { index, payload ->
                            val rank = index + 1
                            val selected = rank in selectedRanks
                            CompareSelectableProductChip(
                                rank = rank,
                                product = payload.product,
                                backendBaseUrl = backendBaseUrl,
                                selected = selected,
                                onClick = {
                                    selectedRanks = if (selected) {
                                        if (selectedRanks.size <= 2) selectedRanks else selectedRanks - rank
                                    } else {
                                        if (selectedRanks.size >= 4) selectedRanks else (selectedRanks + rank).distinct().sorted()
                                    }.sorted()
                                },
                            )
                        }
                    }
                    QuietPrimaryButton(
                        label = stringResource(R.string.compare_start),
                        enabled = selectedRanks.size >= 2,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onStartCompare(selectedRanks) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProductDeckMarkdownCompareTable(
    payload: CompareCardPayload,
    modifier: Modifier = Modifier,
    animateText: Boolean = false,
) {
    val products = payload.products
        .filter { it.productId.isNotBlank() || it.name.isNotBlank() }
        .take(4)
    val axes = payload.axes
        .filter { axis ->
            axis.name.cleanCompareText().isNotBlank() &&
                products.any { product -> axis.valueFor(product.productId) != null }
        }
        .take(7)
    if (products.size < 2 || axes.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BuyPilotColors.SurfaceBg.copy(alpha = 0.34f))
                .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.66f), RoundedCornerShape(10.dp)),
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                MarkdownCompareColumn(
                    modifier = Modifier.width(MarkdownCompareMetricColumnWidth),
                    header = "",
                    cells = axes.map { MarkdownCompareCell(note = it.name.cleanCompareText()) },
                    headerEmphasis = false,
                    cellEmphasis = true,
                    revealKeyPrefix = "compare-table-${payload.compareId}-axis",
                    animateText = animateText,
                )
                products.forEach { product ->
                    MarkdownCompareColumnDivider(rowCount = axes.size)
                    MarkdownCompareColumn(
                        modifier = Modifier.width(MarkdownCompareProductColumnWidth),
                        header = product.shortCompareName(),
                        cells = axes.map { axis ->
                            axis.valueFor(product.productId).markdownCell(
                                axisName = axis.name,
                                productName = product.displayName(),
                            )
                        },
                        headerEmphasis = product.productId == payload.winnerProductId,
                        revealKeyPrefix = "compare-table-${payload.compareId}-${product.productId}",
                        animateText = animateText,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownCompareColumn(
    header: String,
    cells: List<MarkdownCompareCell>,
    modifier: Modifier = Modifier,
    subHeader: String? = null,
    headerEmphasis: Boolean = false,
    cellEmphasis: Boolean = false,
    revealKeyPrefix: String = header,
    animateText: Boolean = false,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(
                    if (headerEmphasis) {
                        BuyPilotColors.PrimarySoft.copy(alpha = 0.24f)
                    } else {
                        BuyPilotColors.SurfaceSubtle.copy(alpha = 0.36f)
                    },
                )
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            CompareStreamingLabel(
                key = "$revealKeyPrefix-header",
                text = header.cleanCompareText(),
                animate = animateText,
                delayMs = 80L,
                color = if (headerEmphasis) BuyPilotColors.PrimaryDark else BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Label,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            subHeader?.cleanCompareText()?.takeIf { it.isNotBlank() }?.let {
                    CompareStreamingLabel(
                        key = "$revealKeyPrefix-subheader",
                        text = it,
                        animate = animateText,
                        delayMs = 120L,
                        color = BuyPilotColors.TextMuted,
                        fontSize = BuyPilotType.Tiny,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        HorizontalDivider(color = BuyPilotColors.Border.copy(alpha = 0.72f))
        cells.forEachIndexed { index, cell ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(
                        if (index % 2 == 0) {
                            Color.Transparent
                        } else {
                            BuyPilotColors.SurfaceSubtle.copy(alpha = 0.2f)
                        },
                    )
                    .padding(horizontal = 9.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                CompareStreamingLabel(
                    key = "$revealKeyPrefix-cell-$index",
                    text = cell.note.ifBlank { "待确认" },
                    animate = animateText,
                    delayMs = 120L + index * 55L,
                    color = if (cellEmphasis) BuyPilotColors.TextPrimary else BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                    fontWeight = if (cellEmphasis) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 3,
                )
            }
            if (index != cells.lastIndex) {
                HorizontalDivider(color = BuyPilotColors.Border.copy(alpha = 0.48f))
            }
        }
    }
}

@Composable
private fun CompareStreamingLabel(
    key: String,
    text: String,
    animate: Boolean,
    delayMs: Long,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    maxLines: Int,
) {
    if (text.isBlank()) return
    if (animate) {
        StreamingAssistantText(
            nodeKey = key,
            content = text,
            done = true,
            animateInitialCompleted = true,
            initialRevealDelayMs = delayMs,
            style = TextStyle(
                color = color,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = fontWeight,
            ),
        )
    } else {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MarkdownCompareColumnDivider(rowCount: Int) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height((54 + 70 * rowCount + rowCount.coerceAtLeast(1)).dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BuyPilotColors.Border.copy(alpha = 0.58f)),
        )
    }
}

@Composable
private fun CompareSelectableProductChip(
    rank: Int,
    product: ProductPayload,
    backendBaseUrl: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) BuyPilotColors.Primary.copy(alpha = 0.58f) else BuyPilotColors.Border.copy(alpha = 0.62f),
        animationSpec = tween(160),
        label = "compare_selector_border",
    )
    Column(
        modifier = Modifier
            .width(94.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) BuyPilotColors.PrimarySoft.copy(alpha = 0.34f) else BuyPilotColors.SurfaceSubtle.copy(alpha = 0.62f))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BuyPilotColors.SurfaceImageAlt),
        ) {
            ProductImage(
                product = product,
                backendBaseUrl = backendBaseUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .background(BuyPilotColors.SurfaceCard.copy(alpha = 0.88f), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "#$rank",
                    color = BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = product.displayName(),
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Tiny,
            lineHeight = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun CompareSummaryCard(
    payload: CompareCardPayload,
    narrationContent: String,
    narrationDone: Boolean,
    conclusionContent: String,
    conclusionDone: Boolean,
    backendBaseUrl: String,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    onEntered: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    var mode by rememberSaveable(payload.compareId) { mutableStateOf(CompareInlineMode.Table) }
    var fallbackVisible by remember(payload.compareId) { mutableStateOf(false) }
    var narrationRevealComplete by remember(payload.compareId) { mutableStateOf(false) }
    var showModeSwitch by remember(payload.compareId) { mutableStateOf(false) }
    var showArtifact by remember(payload.compareId) { mutableStateOf(false) }
    var showAxisSummary by remember(payload.compareId) { mutableStateOf(false) }
    var showClosingAdvice by remember(payload.compareId) { mutableStateOf(false) }
    val streamedNarration = narrationContent.cleanCompareText()
    val streamedConclusion = conclusionContent.cleanCompareText()
    val hasStreamedNarration = streamedNarration.isNotBlank()
    val hasStreamedConclusion = streamedConclusion.isNotBlank()
    val fallbackReason = payload.winnerReason.cleanCompareText()
    val winnerReason = streamedNarration.ifBlank { if (fallbackVisible) fallbackReason else "" }
    val tradeoffs = remember(payload) { payload.displayTradeoffs() }
    val artifactReady = if (winnerReason.isNotBlank()) narrationRevealComplete else fallbackVisible
    LaunchedEffect(payload.compareId, hasStreamedNarration) {
        fallbackVisible = false
        if (!hasStreamedNarration) {
            delay(950)
            fallbackVisible = true
        }
    }
    LaunchedEffect(payload.compareId, winnerReason) {
        narrationRevealComplete = false
        if (winnerReason.isBlank()) {
            narrationRevealComplete = true
        }
    }
    LaunchedEffect(payload.compareId, artifactReady, hasStreamedConclusion, tradeoffs) {
        showModeSwitch = false
        showArtifact = false
        showAxisSummary = false
        showClosingAdvice = false
        if (!artifactReady) return@LaunchedEffect

        delay(40)
        showModeSwitch = true
        delay(120)
        showArtifact = true
        delay(120)
        showAxisSummary = true
        if (hasStreamedConclusion || tradeoffs.isNotEmpty()) {
            delay(110)
            showClosingAdvice = true
        }
    }
    StructuredCardMotion(
        key = payload.compareId.ifBlank { "compare_card" },
        motionEnabled = motionEnabled,
        alreadyEntered = alreadyEntered,
        durationMillis = 300,
        initialOffsetY = 8.dp,
        initialScale = 0.98f,
        onEntered = onEntered,
    ) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            if (winnerReason.isNotBlank()) {
                StreamingAssistantText(
                    nodeKey = if (streamedNarration.isNotBlank()) {
                        "compare-narration-${payload.compareId}"
                    } else {
                        "compare-winner-${payload.compareId}"
                    },
                    content = winnerReason,
                    done = if (hasStreamedNarration) narrationDone else true,
                    animateInitialCompleted = false,
                    onRevealComplete = { narrationRevealComplete = true },
                    style = TextStyle(
                        color = BuyPilotColors.TextPrimary,
                        fontSize = BuyPilotType.LargeBody,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }
            AnimatedVisibility(
                visible = artifactReady,
                enter = fadeIn(tween(220, delayMillis = 80, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(260, delayMillis = 80, easing = FastOutSlowInEasing)) { it / 20 },
                exit = fadeOut(tween(120)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    HorizontalDivider(color = BuyPilotColors.Border.copy(alpha = 0.58f))
                    CompareSectionReveal(visible = showModeSwitch, delayMillis = 0) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CompareInlineModeSwitch(mode = mode, onModeChange = { mode = it })
                        }
                    }
                    CompareSectionReveal(visible = showArtifact, delayMillis = 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(tween(240, easing = FastOutSlowInEasing)),
                        ) {
                            if (mode == CompareInlineMode.Table) {
                                ProductDeckMarkdownCompareTable(
                                    payload = payload,
                                    modifier = Modifier.fillMaxWidth(),
                                    animateText = false,
                                )
                            } else {
                                InlineCompareRadarBlock(
                                    payload = payload,
                                    showAxisSummary = showAxisSummary,
                                )
                            }
                        }
                    }
                    if (mode == CompareInlineMode.Table) {
                        CompareSectionReveal(visible = showAxisSummary, delayMillis = 0) {
                            CompareInlineAxisSummary(
                                payload = payload,
                                axes = payload.axes.filter { it.values.any { value -> value.score != null } }.take(4),
                            )
                        }
                    }
                    CompareSectionReveal(visible = showClosingAdvice, delayMillis = 0) {
                        if (hasStreamedConclusion) {
                            CompareClosingAdvice(
                                content = streamedConclusion,
                                done = conclusionDone,
                                compareId = payload.compareId,
                            )
                        } else if (tradeoffs.isNotEmpty()) {
                            CompareInlineTakeaways(tradeoffs = tradeoffs)
                        }
                    }
                }
            }
        }
    }
}

private enum class CompareInlineMode(val label: String) {
    Table("对比"),
    Radar("雷达"),
}

@Composable
private fun CompareInlineModeSwitch(
    mode: CompareInlineMode,
    onModeChange: (CompareInlineMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(BuyPilotColors.SurfaceSubtle.copy(alpha = 0.72f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CompareInlineMode.entries.forEach { item ->
            val selected = item == mode
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) BuyPilotColors.SurfaceCard else Color.Transparent)
                    .clickable(role = Role.Tab, onClick = { onModeChange(item) })
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.label,
                    color = if (selected) BuyPilotColors.TextPrimary else BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun InlineCompareRadarBlock(
    payload: CompareCardPayload,
    showAxisSummary: Boolean = true,
) {
    val axes = payload.axes.filter { it.values.any { value -> value.score != null } }.take(6)
    if (axes.size < 3 || payload.products.size < 2) {
        CompareBarsChart(payload = payload)
        return
    }
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "inline_compare_radar_progress",
    )
    val colors = comparePalette()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(214.dp),
        ) {
            val center = Offset(size.width / 2f, size.height / 2f + 2.dp.toPx())
            val radius = min(size.width, size.height) * 0.29f
            val count = axes.size
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BuyPilotColors.TextSecondary.toArgb()
                textSize = 11.sp.toPx()
                textAlign = Paint.Align.CENTER
            }
            for (ring in 1..3) {
                val ringPath = Path()
                for (i in 0 until count) {
                    val point = radarPoint(center, radius * ring / 3f, i, count)
                    if (i == 0) ringPath.moveTo(point.x, point.y) else ringPath.lineTo(point.x, point.y)
                }
                ringPath.close()
                drawPath(ringPath, BuyPilotColors.Border.copy(alpha = 0.42f), style = Stroke(width = 1.dp.toPx()))
            }
            for (i in 0 until count) {
                val point = radarPoint(center, radius, i, count)
                drawLine(BuyPilotColors.Border.copy(alpha = 0.38f), center, point, strokeWidth = 1.dp.toPx())
                val labelPoint = radarPoint(center, radius + 22.dp.toPx(), i, count)
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        axes[i].name.cleanCompareText().take(5),
                        labelPoint.x,
                        labelPoint.y + 4.dp.toPx(),
                        labelPaint,
                    )
                }
            }
            payload.products.take(4).forEachIndexed { productIndex, product ->
                val path = Path()
                axes.forEachIndexed { axisIndex, axis ->
                    val score = axis.scoreFor(product.productId) ?: 0.0
                    val point = radarPoint(center, radius * (score / 100.0).toFloat() * progress, axisIndex, count)
                    if (axisIndex == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                path.close()
                val color = colors[productIndex % colors.size]
                drawPath(path, color.copy(alpha = 0.11f))
                drawPath(path, color.copy(alpha = 0.82f), style = Stroke(width = 2.dp.toPx()))
            }
        }
        CompareLegend(products = payload.products, winnerProductId = payload.winnerProductId)
        CompareSectionReveal(visible = showAxisSummary, delayMillis = 0) {
            CompareInlineAxisSummary(payload = payload, axes = axes)
        }
    }
}

@Composable
private fun CompareInlineAxisSummary(
    payload: CompareCardPayload,
    axes: List<CompareAxisPayload>,
) {
    val rows = remember(payload, axes) {
        axes.mapNotNull { axis ->
            val best = axis.values
                .mapNotNull { value ->
                    val score = value.score ?: return@mapNotNull null
                    val product = payload.products.firstOrNull { it.productId == value.productId } ?: return@mapNotNull null
                    product to score.coerceIn(0.0, 100.0)
                }
                .maxByOrNull { it.second }
            if (best == null) {
                null
            } else {
                "${axis.name.cleanCompareText()}：${best.first.shortCompareName()}，${best.second.scorePhraseForAxis(axis.name)}"
            }
        }.take(4)
    }
    if (rows.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Text(
                text = row,
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Label,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompareSectionReveal(
    visible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220, delayMillis = delayMillis, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(280, delayMillis = delayMillis, easing = FastOutSlowInEasing)) { it / 14 },
        exit = fadeOut(tween(120)),
    ) {
        content()
    }
}

@Composable
private fun CompareClosingAdvice(
    content: String,
    done: Boolean,
    compareId: String,
) {
    StreamingAssistantText(
        nodeKey = "compare-conclusion-$compareId",
        content = content,
        done = done,
        animateInitialCompleted = true,
        style = TextStyle(
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.LargeBody,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
        ),
    )
}

@Composable
private fun CompareInlineTakeaways(
    tradeoffs: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tradeoffs.take(3).forEach { tradeoff ->
            Text(
                text = "· $tradeoff",
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Label,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
internal fun CompareModeScreen(
    payload: CompareCardPayload,
    backendBaseUrl: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = BuyPilotColors.SurfaceBg, modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
            CompareModeTopBar(onDismiss = onDismiss)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                CompareObjectLine(products = payload.products)
                CompareVerdictBlock(payload = payload)
                CompareEvidenceSummary(payload = payload)
                if (payload.riskNotes.isNotEmpty()) {
                    CompareRiskSection(notes = payload.riskNotes, products = payload.products)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun CompareModeTopBar(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.SurfaceCard)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_24),
                    contentDescription = stringResource(R.string.common_back),
                    tint = BuyPilotColors.PrimaryDark,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = stringResource(R.string.compare_evidence_title),
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Title,
                lineHeight = 23.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(42.dp))
        }
        HorizontalDivider(color = BuyPilotColors.Border.copy(alpha = 0.5f))
    }
}

@Composable
private fun CompareObjectLine(products: List<ProductPayload>) {
    val text = products
        .take(4)
        .mapIndexed { index, product -> "${index + 1}. ${product.shortCompareName()}" }
        .joinToString("  /  ")
    if (text.isBlank()) return
    Text(
        text = text,
        color = BuyPilotColors.TextMuted,
        fontSize = BuyPilotType.Label,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun CompareProductHeaderRow(
    products: List<ProductPayload>,
    backendBaseUrl: String,
    winnerProductId: String?,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        products.forEachIndexed { index, product ->
            val isWinner = product.productId == winnerProductId
            Column(
                modifier = Modifier.width(112.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(BuyPilotColors.SurfaceCard)
                        .border(
                            1.dp,
                            if (isWinner) BuyPilotColors.Primary.copy(alpha = 0.42f) else BuyPilotColors.Border.copy(alpha = 0.58f),
                            RoundedCornerShape(18.dp),
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ProductImage(
                        product = product,
                        backendBaseUrl = backendBaseUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(
                    text = product.displayName(),
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.compare_product_index, index + 1),
                    color = if (isWinner) BuyPilotColors.PrimaryDark else BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun CompareVerdictBlock(payload: CompareCardPayload) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = payload.winnerReason.cleanCompareText().ifBlank { "这几款没有拉开明显差距。" },
            color = BuyPilotColors.TextPrimary,
            fontSize = 21.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.SemiBold,
        )
        payload.focus?.cleanCompareText()?.takeIf { it.isNotBlank() }?.let { focus ->
            Text(
                text = stringResource(R.string.compare_focus_prefix, focus),
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Label,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun CompareChartModeSwitch(
    mode: CompareChartMode,
    onModeChange: (CompareChartMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(BuyPilotColors.SurfaceSubtle.copy(alpha = 0.72f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CompareChartMode.entries.forEach { item ->
            val selected = item == mode
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) BuyPilotColors.SurfaceCard else Color.Transparent)
                    .clickable(role = Role.Tab, onClick = { onModeChange(item) })
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.label,
                    color = if (selected) BuyPilotColors.TextPrimary else BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun CompareRadarChart(payload: CompareCardPayload) {
    val axes = payload.axes.filter { it.values.any { value -> value.score != null } }.take(6)
    if (axes.size < 3 || payload.products.isEmpty()) {
        CompareBarsChart(payload = payload)
        return
    }
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "compare_radar_progress",
    )
    val colors = comparePalette()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(286.dp),
        ) {
            val center = Offset(size.width / 2f, size.height / 2f + 4.dp.toPx())
            val radius = min(size.width, size.height) * 0.32f
            val count = axes.size
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BuyPilotColors.TextSecondary.toArgb()
                textSize = 11.sp.toPx()
                textAlign = Paint.Align.CENTER
            }
            for (ring in 1..4) {
                val ringPath = Path()
                for (i in 0 until count) {
                    val point = radarPoint(center, radius * ring / 4f, i, count)
                    if (i == 0) ringPath.moveTo(point.x, point.y) else ringPath.lineTo(point.x, point.y)
                }
                ringPath.close()
                drawPath(ringPath, BuyPilotColors.Border.copy(alpha = 0.46f), style = Stroke(width = 1.dp.toPx()))
            }
            for (i in 0 until count) {
                val point = radarPoint(center, radius, i, count)
                drawLine(BuyPilotColors.Border.copy(alpha = 0.42f), center, point, strokeWidth = 1.dp.toPx())
                val labelPoint = radarPoint(center, radius + 22.dp.toPx(), i, count)
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        axes[i].name.cleanCompareText().take(6),
                        labelPoint.x,
                        labelPoint.y + 4.dp.toPx(),
                        labelPaint,
                    )
                }
            }
            payload.products.take(4).forEachIndexed { productIndex, product ->
                val path = Path()
                axes.forEachIndexed { axisIndex, axis ->
                    val score = axis.scoreFor(product.productId) ?: 0.0
                    val point = radarPoint(center, radius * (score / 100.0).toFloat() * progress, axisIndex, count)
                    if (axisIndex == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                path.close()
                val color = colors[productIndex % colors.size]
                drawPath(path, color.copy(alpha = 0.13f))
                drawPath(path, color.copy(alpha = 0.82f), style = Stroke(width = 2.dp.toPx()))
            }
        }
        CompareLegend(products = payload.products, winnerProductId = payload.winnerProductId)
        CompareAxisHintRow(axes = axes)
    }
}

@Composable
private fun CompareAxisHintRow(axes: List<CompareAxisPayload>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        axes.forEach { axis ->
            Text(
                text = axis.name.cleanCompareText(),
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Tiny,
                lineHeight = 13.sp,
                modifier = Modifier
                    .background(BuyPilotColors.SurfaceSubtle.copy(alpha = 0.58f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun CompareBarsChart(payload: CompareCardPayload) {
    val colors = comparePalette()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        payload.axes.filter { it.values.any { value -> value.score != null } }.forEach { axis ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = axis.name.cleanCompareText(),
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                payload.products.take(4).forEachIndexed { index, product ->
                    val score = axis.scoreFor(product.productId)
                    if (score != null) {
                        CompareScoreBar(
                            label = product.displayName(),
                            score = score,
                            color = colors[index % colors.size],
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareScoreBar(
    label: String,
    score: Double,
    color: Color,
) {
    val progress by animateFloatAsState(
        targetValue = (score / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "compare_score_bar",
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label.shortCompareProductAlias().ifBlank { label },
            color = BuyPilotColors.TextSecondary,
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(86.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(CircleShape)
                .background(BuyPilotColors.SurfaceSubtle),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.78f)),
            )
        }
        Text(
            text = score.clean(),
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun CompareAxisDetails(payload: CompareCardPayload) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = stringResource(R.string.compare_dimension_detail),
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Title,
            lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold,
        )
        payload.axes.forEach { axis ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BuyPilotColors.SurfaceCard.copy(alpha = 0.66f), RoundedCornerShape(16.dp))
                    .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.54f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    text = axis.name.cleanCompareText(),
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                axis.values.forEach { value ->
                    val product = payload.products.firstOrNull { it.productId == value.productId }
                    val detail = value.detail.cleanCompareText()
                        .ifBlank { value.label.cleanCompareText() }
                    if (product != null && detail.isNotBlank()) {
                        Text(
                            text = "${product.displayName()}：$detail",
                            color = BuyPilotColors.TextSecondary,
                            fontSize = BuyPilotType.Label,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareEvidenceSummary(payload: CompareCardPayload) {
    val takeaways = remember(payload) { payload.displayTradeoffs() }
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        if (takeaways.isNotEmpty()) {
            CompareEvidenceTextGroup(
                title = stringResource(R.string.compare_key_differences),
                items = takeaways,
            )
        }
        CompareAxisEvidenceList(payload = payload)
        CompareEvidenceFooter()
    }
}

@Composable
private fun CompareAxisEvidenceList(payload: CompareCardPayload) {
    val axes = payload.axes.filter { it.values.isNotEmpty() }
    if (axes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.compare_dimension_evidence),
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Title,
            lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold,
        )
        axes.forEach { axis ->
            CompareAxisEvidenceBlock(axis = axis, products = payload.products)
        }
    }
}

@Composable
private fun CompareAxisEvidenceBlock(
    axis: CompareAxisPayload,
    products: List<ProductPayload>,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            text = axis.name.cleanCompareText(),
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Body,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        axis.values.forEach { value ->
            val product = products.firstOrNull { it.productId == value.productId } ?: return@forEach
            CompareAxisEvidenceRow(
                productName = product.shortCompareName(),
                axisName = axis.name,
                value = value,
            )
        }
    }
}

@Composable
private fun CompareAxisEvidenceRow(
    productName: String,
    axisName: String,
    value: CompareAxisValuePayload,
) {
    val score = value.score?.coerceIn(0.0, 100.0)
    val detail = value.detail.cleanCompareText().takeIf { it.isMeaningfulCompareCellText(axisName) }
        ?: value.label.cleanCompareText().takeIf { it.isMeaningfulCompareCellText(axisName) }
    val reason = compareScoreSummary(
        axisName = axisName,
        score = score,
        detail = detail,
        productName = productName,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = productName,
            color = BuyPilotColors.TextSecondary,
            fontSize = BuyPilotType.Label,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(104.dp),
        )
        Text(
            text = reason,
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Label,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CompareEvidenceTextGroup(
    title: String,
    items: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            text = title,
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Title,
            lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold,
        )
        items.take(4).forEach { item ->
            Text(
                text = "· ${item.cleanCompareText()}",
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun CompareEvidenceFooter() {
    Text(
        text = stringResource(R.string.compare_detail_explain),
        color = BuyPilotColors.TextMuted,
        fontSize = BuyPilotType.Label,
        lineHeight = 18.sp,
    )
}

@Composable
private fun CompareTextSection(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Title,
            lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold,
        )
        items.take(5).forEach { item ->
            Text(
                text = "· ${item.cleanCompareText()}",
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun CompareRiskSection(
    notes: List<CompareRiskNotePayload>,
    products: List<ProductPayload>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.compare_risk_title),
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Title,
            lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold,
        )
        notes.forEach { note ->
            val product = products.firstOrNull { it.productId == note.productId }
            Text(
                text = "${product?.displayName("该商品").orEmpty()}：${note.note.cleanCompareText()}",
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun CompareProductAvatarRow(products: List<ProductPayload>, backendBaseUrl: String) {
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        products.take(4).forEach { product ->
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BuyPilotColors.SurfaceImageAlt)
                    .border(2.dp, BuyPilotColors.SurfaceCard, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                ProductImage(
                    product = product,
                    backendBaseUrl = backendBaseUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun CompareLegend(products: List<ProductPayload>, winnerProductId: String?) {
    val colors = comparePalette()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        products.take(4).forEachIndexed { index, product ->
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colors[index % colors.size], CircleShape),
                )
                Text(
                    text = product.shortCompareName(),
                    color = if (product.productId == winnerProductId) BuyPilotColors.TextPrimary else BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 13.sp,
                    fontWeight = if (product.productId == winnerProductId) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CompareMetaPill(text: String) {
    Text(
        text = text,
        color = BuyPilotColors.TextSecondary,
        fontSize = BuyPilotType.Label,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(BuyPilotColors.SurfaceSubtle.copy(alpha = 0.74f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun QuietPrimaryButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.42f
    Box(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(CircleShape)
            .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.58f * alpha), CircleShape)
            .border(1.dp, BuyPilotColors.Primary.copy(alpha = 0.2f * alpha), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = BuyPilotColors.PrimaryDark.copy(alpha = alpha),
            fontSize = BuyPilotType.Body,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private enum class CompareChartMode(val label: String) {
    Radar("雷达"),
    Bars("评分条"),
}

private fun CompareAxisPayload.scoreFor(productId: String): Double? =
    values.firstOrNull { it.productId == productId }?.score?.coerceIn(0.0, 100.0)

private fun CompareAxisPayload.valueFor(productId: String): CompareAxisValuePayload? =
    values.firstOrNull { it.productId == productId }

private data class MarkdownCompareCell(
    val note: String = "",
)

private fun CompareAxisValuePayload?.markdownCell(
    axisName: String,
    productName: String,
): MarkdownCompareCell {
    if (this == null) return MarkdownCompareCell()
    val detail = detail.cleanCompareText()
    val label = label.cleanCompareText()
    if (axisName.isPriceAxis()) {
        return MarkdownCompareCell(note = detail.ifBlank { label }.priceNumberOnly())
    }
    val note = compareScoreSummary(
        axisName = axisName,
        score = score,
        detail = detail.takeIf { it.isMeaningfulCompareCellText(axisName) }
            ?: label.takeIf { it.isMeaningfulCompareCellText(axisName) },
        productName = productName,
    )
    return MarkdownCompareCell(
        note = note.ifBlank { "待确认" },
    )
}

private fun ProductPayload.shortCompareName(): String {
    val clean = displayName().cleanCompareText()
    if (clean.isBlank()) return "商品"
    val brand = brand.cleanCompareText()
    val withoutBrand = if (brand.isNotBlank() && clean.startsWith(brand, ignoreCase = true)) {
        clean.removePrefix(brand).trim()
    } else {
        clean
    }
    val knownFieldWords = listOfNotNull(brand, category, subCategory)
        .flatMap { it.compareNameTokens() }
        .filter { it.length >= 2 }
        .distinctBy { it.lowercase() }
    val withoutKnownFields = knownFieldWords.fold(withoutBrand) { text, word ->
        text.replace(word, " ", ignoreCase = true)
    }
    val normalized = withoutKnownFields
        .replace(CompareProductFillerWordsRegex, " ")
        .replace(CompareWhitespaceRegex, " ")
        .trim()
        .ifBlank { clean }
    return normalized
        .split(CompareNameSplitRegex)
        .take(3)
        .joinToString(" ")
        .take(18)
        .ifBlank { clean }
}

private fun String.compareNameTokens(): List<String> =
    cleanCompareText()
        .split(CompareNameTokenSplitRegex)
        .map { it.trim() }
        .filter { it.isNotBlank() }

private fun String.isPriceAxis(): Boolean {
    val clean = cleanCompareText()
    return listOf("价格", "售价", "预算", "price", "budget").any {
        clean.contains(it, ignoreCase = true)
    }
}

private fun String.priceNumberOnly(): String {
    val clean = cleanCompareText()
    val match = ComparePriceNumberRegex.find(clean)
    return match?.value?.let { "$it 元" } ?: clean
}

private fun String.shortCompareNote(axisName: String, productName: String): String {
    val axis = axisName.cleanCompareText()
    val product = productName.cleanCompareText()
    return cleanCompareText()
        .removePrefix(product)
        .replace(product, "")
        .replace(axis, "")
        .replace(CompareAspectPhraseRegex, "")
        .replace(CompareAdvantagePhraseRegex, "更优")
        .replace(CompareBetterPhraseRegex, "表现更好")
        .replace(CompareGenericCapabilityRegex, "")
        .replace(CompareTrailingPunctuationRegex, "")
        .replace(CompareWhitespaceRegex, " ")
        .trim('：', ':', '，', ',', '。', ' ')
        .ifBlank { cleanCompareText() }
        .take(20)
}

private fun compareScoreSummary(
    axisName: String,
    score: Double?,
    detail: String?,
    productName: String,
): String {
    val safeScore = score?.coerceIn(0.0, 100.0)
    val reason = detail?.extractCompareReason(axisName = axisName, productName = productName).orEmpty()
    val phrase = safeScore?.scorePhraseForAxis(axisName).orEmpty()
    return listOf(reason.ifBlank { phrase })
        .filter { it.isNotBlank() }
        .joinToString("\n")
        .ifBlank { phrase }
}

private fun String.extractCompareReason(axisName: String, productName: String): String {
    val axis = axisName.cleanCompareText()
    val cleaned = cleanCompareText()
        .removePrefix(productName.cleanCompareText())
        .replace(productName.cleanCompareText(), "")
        .replace(productName.shortCompareProductAlias(), "")
        .replace(axis, "")
        .replace(CompareQuestionLineRegex, "")
        .replace(CompareAnswerMarkerRegex, "")
        .replace(CompareDesignedForRegex, "")
        .replace(CompareAudienceBuiltRegex, "")
        .replace(CompareMarketingVerbRegex, "")
        .replace(ComparePunctuationSpacingRegex, " ")
        .replace(CompareWhitespaceRegex, " ")
        .trim('：', ':', '，', ',', '。', '；', ';', ' ')
    val compact = cleaned.trim()
    return when {
        compact.length < 4 -> ""
        else -> compact.take(12).trim()
    }
}

private fun String.cleanInlineCompareCell(productName: String): String {
    val product = productName.cleanCompareText()
    val alias = product.shortCompareProductAlias().ifBlank { product }
    return cleanCompareText()
        .replace(product, alias)
        .replace(CompareWhitespaceRegex, " ")
        .trim('：', ':', '，', ',', '。', '；', ';', ' ')
        .let { text ->
            if (text.length > 46) text.take(44).trimEnd('，', ',', '。', '；', ';', ' ') else text
        }
}

private fun String.isMeaningfulCompareCellText(axisName: String): Boolean {
    val clean = cleanCompareText()
    if (clean.length < 2) return false
    val axis = axisName.cleanCompareText()
    val punctuationStripped = clean.replace(CompareCellPunctuationRegex, "")
    if (punctuationStripped.isBlank()) return false
    if (punctuationStripped == axis) return false
    val dimensionWords = listOf("核心参数", "性能", "续航", "价格", "影像", "屏幕", "风险", "口碑")
    val withoutDimensionWords = dimensionWords.fold(punctuationStripped) { acc, word -> acc.replace(word, "") }
    if (withoutDimensionWords.isBlank()) return false
    return true
}

private fun Double.scoreKeyword(): String =
    when {
        this >= 86.0 -> "明显更强"
        this >= 72.0 -> "表现较好"
        this >= 58.0 -> "够用"
        else -> "偏弱"
    }

private fun Double.scorePhraseForAxis(axisName: String): String {
    return when {
        this >= 86.0 -> "明显占优"
        this >= 72.0 -> "表现更稳"
        this >= 58.0 -> "基本够用"
        else -> "相对偏弱"
    }
}

private fun CompareCardPayload.displayTradeoffs(): List<String> {
    val productNames = products.map { it.displayName().cleanCompareText() }.filter { it.isNotBlank() }
    val generated = axes
        .asSequence()
        .mapNotNull { axis -> axis.axisDifferenceSentence(products) }
        .distinctBy { it.compareDedupKey() }
        .take(3)
        .toList()
    if (generated.isNotEmpty()) return generated
    val direct = tradeoffs
        .asSequence()
        .map { it.cleanCompareText().compactCompareSentence(productNames) }
        .filter { it.isNotBlank() }
        .distinctBy { it.compareDedupKey() }
        .take(3)
        .toList()
    if (direct.size >= 2 && direct.map { it.compareDedupKey() }.distinct().size >= 2) {
        return direct
    }
    return generated.ifEmpty { direct }.take(3)
}

private fun CompareAxisPayload.axisDifferenceSentence(products: List<ProductPayload>): String? {
    val scored = values
        .mapNotNull { value ->
            val score = value.score ?: return@mapNotNull null
            val product = products.firstOrNull { it.productId == value.productId } ?: return@mapNotNull null
            product to score
        }
        .sortedByDescending { it.second }
    if (scored.size < 2) return null
    val best = scored.first()
    val second = scored[1]
    if (best.second - second.second < 6.0) return null
    val axis = name.cleanCompareText()
    val bestName = best.first.shortCompareName()
    return "$axis：$bestName ${best.second.scorePhraseForAxis(axis)}"
}

private fun String.compactCompareSentence(productNames: List<String>): String {
    var text = cleanCompareText()
    productNames.forEach { name ->
        val short = name.shortCompareProductAlias()
        if (short.isNotBlank()) {
            text = text.replace(name, short)
        }
    }
    return text
        .replace(CompareDimensionAdvantageRegex, "更优")
        .replace(CompareInAspectRegex, "在$1")
        .replace(CompareWhitespaceRegex, " ")
        .trim('：', ':', '，', ',', '。', ' ')
}

private fun String.compareDedupKey(): String =
    cleanCompareText()
        .replace(CompareDedupCharsRegex, "")
        .replace(CompareDedupWordsRegex, "")
        .takeLast(18)

private fun String.shortCompareProductAlias(): String {
    val clean = cleanCompareText()
    return clean
        .replace(CompareAliasGenericWordsRegex, " ")
        .replace(CompareWhitespaceRegex, " ")
        .split(CompareAliasSplitRegex)
        .firstOrNull()
        ?.trim()
        ?.take(12)
        .orEmpty()
}

private fun radarPoint(center: Offset, radius: Float, index: Int, count: Int): Offset {
    val angle = -PI / 2.0 + (2.0 * PI * index / count)
    return Offset(
        x = center.x + cos(angle).toFloat() * radius,
        y = center.y + sin(angle).toFloat() * radius,
    )
}

@Composable
private fun comparePalette(): List<Color> =
    listOf(
        BuyPilotColors.Primary,
        Color(0xFF5A7FA8),
        Color(0xFF7B8F62),
        Color(0xFF8D789E),
    )

private fun String?.cleanCompareText(): String =
    this?.withoutMarkdownMarkup()?.withoutInternalDebugTokens()?.trim().orEmpty()

private fun String?.userFacingCompareConfidence(): String =
    when (this?.trim()?.lowercase()) {
        "high" -> "把握较高"
        "low" -> "把握偏低"
        "medium" -> "把握中等"
        else -> "把握中等"
    }

private fun String.userFacingCompareMode(): String =
    when (trim().lowercase()) {
        "decision" -> "结论后候选差异"
        else -> "当前候选对比"
    }

@Preview(name = "Compare summary card")
@Composable
private fun CompareSummaryCardPreview() {
    val products = listOf(
        ProductPayload(
            productId = "p_preview_a",
            name = "候选商品 A",
            price = 299.0,
            brand = "品牌 A",
            category = "目标品类",
        ),
        ProductPayload(
            productId = "p_preview_b",
            name = "候选商品 B",
            price = 259.0,
            brand = "品牌 B",
            category = "目标品类",
        ),
    )
    Surface(color = BuyPilotColors.SurfaceBg) {
        CompareSummaryCard(
            payload = CompareCardPayload(
                compareId = "preview_compare",
                products = products,
                axes = listOf(
                    CompareAxisPayload(
                        name = "场景匹配",
                        values = listOf(
                            CompareAxisValuePayload(productId = "p_preview_a", score = 88.0, detail = "更贴合核心场景"),
                            CompareAxisValuePayload(productId = "p_preview_b", score = 74.0, detail = "基础需求可覆盖"),
                        ),
                    ),
                    CompareAxisPayload(
                        name = "预算友好",
                        values = listOf(
                            CompareAxisValuePayload(productId = "p_preview_a", score = 76.0, detail = "略高但理由更充分"),
                            CompareAxisValuePayload(productId = "p_preview_b", score = 86.0, detail = "价格更稳"),
                        ),
                    ),
                ),
                winnerProductId = "p_preview_a",
                winnerReason = "如果你更看重场景匹配，优先选 A；如果预算更敏感，B 更稳。",
                tradeoffs = listOf("场景匹配：A 更强", "预算友好：B 更稳"),
                confidence = "medium",
            ),
            narrationContent = "这两款不是简单谁更便宜谁更好，关键要看核心场景和预算取舍。",
            narrationDone = true,
            conclusionContent = "收束建议：重场景匹配选 A，重预算控制选 B。",
            conclusionDone = true,
            backendBaseUrl = "",
            motionEnabled = false,
            alreadyEntered = true,
            onEntered = {},
            onOpenDetail = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
