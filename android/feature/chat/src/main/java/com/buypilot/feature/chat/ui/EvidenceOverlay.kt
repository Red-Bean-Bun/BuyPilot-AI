package com.buypilot.feature.chat.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.buypilot.core.model.EvidencePayload
import com.buypilot.core.model.ReasonAtomPayload
import com.buypilot.feature.chat.R
import com.buypilot.feature.chat.state.ChatUiState

private const val ProductEvidenceEnterMs = 560

// Light minimal evidence palette
private val EvidenceBg = Color(0xFFF8F9FB)
private val EvidenceSurface = Color(0xFFFFFFFF)
private val EvidenceSurfaceRaised = Color(0xFFF4F5F7)
private val EvidenceBorder = Color(0xFFE8EAF0)
private val EvidenceInk = Color(0xFF111318)
private val EvidenceInkSecondary = Color(0xFF4A5060)
private val EvidenceInkMuted = Color(0xFF9EA5B4)
private val EvidenceAccent = Color(0xFFFF6A3D)
private val EvidenceAccentSoft = Color(0xFFFFF2EE)
private val EvidenceNumberBg = Color(0xFFF0F1F4)
private val EvidenceWhitespaceRegex = Regex("""\s+""")
private val EvidenceLeadingBulletRegex = Regex("""^\s*[·•\-\s]+""")

@Composable
fun ProductEvidenceOverlayScreen(
    state: ChatUiState,
    deckId: String,
    productId: String,
    deckNodeKey: String? = null,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val payload = state.findProduct(deckId, productId, deckNodeKey)

    if (payload == null) {
        Surface(color = EvidenceBg, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ProductPageTopBar(title = stringResource(R.string.evidence_title), onBack = onBack)
                ExpiredRecommendationState(onBack = onBack)
            }
        }
        return
    }

    val evidenceItems = payload.evidence
        .mapNotNull { evidence -> evidence.takeIf { it.cleanSnippet().isNotBlank() } }
    val evidenceByKey = remember(evidenceItems) {
        evidenceItems
            .mapNotNull { evidence -> evidence.linkKey()?.let { key -> key to evidence } }
            .toMap()
    }
    val reason = payload.reason.withoutInternalDebugTokens().trim()
    val reasonAtoms = payload.reasonAtoms
        .filter { it.text.isNotBlank() || it.dimension.isNotBlank() || it.value.isNotBlank() }
        .take(5)
    val linkedEvidenceKeys = reasonAtoms.mapNotNull { it.evidenceId?.trim()?.takeIf(String::isNotBlank) }.toSet()
    val remainingEvidence = evidenceItems.filter { it.linkKey() !in linkedEvidenceKeys }
    val riskSourceTexts = remember(state.productDetails[productId]) {
        state.productDetails[productId]
            ?.reviews
            ?.map { it.content.withoutInternalDebugTokens().trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }
    val routeProgressState = rememberRouteEnterProgress(
        key = "evidence_${deckId}_${deckNodeKey.orEmpty()}_${productId}",
        durationMillis = ProductEvidenceEnterMs,
    )
    val progress = routeProgressState.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EvidenceBg),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 116.dp, bottom = 52.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item("lead") {
                EvidenceMotionBlock(progress = progress, start = 0.08f, end = 0.58f, offsetY = 18f) {
        EvidenceLead()
                }
            }
            item("report") {
                EvidenceMotionBlock(progress = progress, start = 0.2f, end = 1f, offsetY = 24f) {
                    EvidenceReportSurface(
                        reason = reason,
                        reasonAtoms = reasonAtoms,
                        evidenceByKey = evidenceByKey,
                        remainingEvidence = remainingEvidence,
                        riskNotes = payload.riskNotes,
                        riskSourceTexts = riskSourceTexts,
                    )
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 14.dp, top = 9.dp)
                .graphicsLayer {
                    val chromeEnter = segmentProgress(progress, 0.08f, 0.58f)
                    alpha = chromeEnter
                    translationY = (1f - chromeEnter) * -10f
                    scaleX = lerp(0.94f, 1f, chromeEnter)
                    scaleY = lerp(0.94f, 1f, chromeEnter)
                }
                .zIndex(2f)
                .size(44.dp)
                .background(EvidenceSurface, CircleShape)
                .border(1.dp, EvidenceBorder, CircleShape),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = EvidenceInk,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_24),
                contentDescription = stringResource(R.string.common_back),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun EvidenceMotionBlock(
    progress: Float,
    start: Float,
    end: Float,
    offsetY: Float,
    content: @Composable () -> Unit,
) {
    val t = segmentProgress(progress, start, end)
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = t
            translationY = (1f - t) * offsetY
            scaleX = lerp(0.982f, 1f, t)
            scaleY = lerp(0.982f, 1f, t)
        },
    ) {
        content()
    }
}

@Composable
private fun EvidenceLead(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 22.dp)
                    .background(EvidenceAccent, RoundedCornerShape(2.dp)),
            )
            Text(
                text = stringResource(R.string.evidence_why_title),
                color = EvidenceInk,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.evidence_lead_subtitle),
            color = EvidenceInkMuted,
            fontSize = BuyPilotType.Label,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun EvidenceReportSurface(
    reason: String,
    reasonAtoms: List<ReasonAtomPayload>,
    evidenceByKey: Map<String, EvidencePayload>,
    remainingEvidence: List<EvidencePayload>,
    riskNotes: List<String>,
    riskSourceTexts: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (reason.isNotBlank()) {
            EvidenceConclusion(reason = reason)
        }
        if (reasonAtoms.isNotEmpty()) {
            EvidenceSectionCard(
                title = stringResource(R.string.evidence_criteria_title),
                subtitle = stringResource(R.string.evidence_criteria_subtitle),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    reasonAtoms.forEachIndexed { index, atom ->
                        val evidence = atom.evidenceId
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                            ?.let(evidenceByKey::get)
                        EvidenceReasonRow(index = index, atom = atom, evidence = evidence)
                        if (index != reasonAtoms.lastIndex) {
                            HorizontalDivider(thickness = 1.dp, color = EvidenceBorder)
                        }
                    }
                }
            }
        }
        val cleanRiskNotes = riskNotes.toEvidenceRiskItems(riskSourceTexts)
        if (cleanRiskNotes.isNotEmpty()) {
            EvidenceRiskCard(items = cleanRiskNotes)
        }
        if (remainingEvidence.isNotEmpty()) {
            EvidenceSectionCard(
                title = stringResource(R.string.evidence_more_title),
                subtitle = stringResource(R.string.evidence_more_subtitle),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    remainingEvidence.take(5).forEachIndexed { index, evidence ->
                        EvidenceSnippetCard(evidence = evidence)
                        if (index != minOf(remainingEvidence.size, 5) - 1) {
                            HorizontalDivider(thickness = 1.dp, color = EvidenceBorder)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EvidenceConclusion(reason: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EvidenceAccentSoft, RoundedCornerShape(16.dp))
            .border(1.dp, EvidenceAccent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(EvidenceAccent, CircleShape),
            )
            Text(
                text = stringResource(R.string.evidence_judgement),
                color = EvidenceAccent,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = reason,
            color = EvidenceInk,
            fontSize = 17.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EvidenceReasonRow(
    index: Int,
    atom: ReasonAtomPayload,
    evidence: EvidencePayload?,
) {
    val title = atom.userFacingReasonTitle()
    val body = atom.text.withoutInternalDebugTokens().trim()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(EvidenceNumberBg, RoundedCornerShape(8.dp))
                    .border(1.dp, EvidenceBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "%02d".format(index + 1),
                    color = EvidenceAccent,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = title,
                    color = EvidenceInk,
                    fontSize = BuyPilotType.LargeBody,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (body.isNotBlank()) {
                    Text(
                        text = body,
                        color = EvidenceInkSecondary,
                        fontSize = BuyPilotType.Body,
                        lineHeight = 21.sp,
                    )
                }
            }
        }
        evidence?.let {
            EvidenceInlineQuote(evidence = it)
        }
    }
}

@Composable
private fun EvidenceInlineQuote(evidence: EvidencePayload) {
    val snippet = evidence.cleanSnippet()
    if (snippet.isBlank()) return
    val parts = remember(snippet) { snippet.toEvidenceSnippetParts() }
    var showDialog by rememberSaveable(evidence.evidenceId, snippet) { androidx.compose.runtime.mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(EvidenceSurfaceRaised)
            .border(1.dp, EvidenceBorder, RoundedCornerShape(10.dp))
            .clickable(role = Role.Button) { showDialog = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EvidenceSourceLabel(evidence = evidence)
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.evidence_view_full_arrow),
                color = EvidenceAccent,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (parts.size > 1) {
            // parts[0]=标题, parts[1]=品牌, parts[2]=品类, parts[3]=价格, ...
            Text(
                text = parts.first(),
                color = EvidenceInkSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 21.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // 只取品牌/品类/价格（前3个meta字段），过滤掉场景、别名等冗余字段
            val keyMeta = parts.drop(1).take(3).joinToString("  ·  ")
            if (keyMeta.isNotBlank()) {
                Text(
                    text = keyMeta,
                    color = EvidenceInkMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = snippet,
                color = EvidenceInkSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 21.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (showDialog) {
        if (parts.size > 1) {
            EvidenceProductDialog(parts = parts, onDismiss = { showDialog = false })
        } else {
            val sourceLabels = rememberEvidenceSourceLabels()
            EvidenceTextDialog(
                title = evidence.sourceType.userFacingEvidenceSourceLabel(
                    stringResource(R.string.evidence_product_info),
                    sourceLabels,
                ),
                body = snippet,
                onDismiss = { showDialog = false },
            )
        }
    }
}

@Composable
private fun EvidenceTextDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            color = EvidenceSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, EvidenceBorder),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    color = EvidenceInk,
                    fontSize = BuyPilotType.Title,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    color = EvidenceInkSecondary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 23.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

private val ProductPartLabels = listOf("商品名", "品牌", "品类", "价格", "使用场景", "别名")

@Composable
private fun EvidenceProductDialog(parts: List<String>, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true, usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            color = EvidenceSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, EvidenceBorder),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.evidence_product_info),
                    color = EvidenceInk,
                    fontSize = BuyPilotType.Title,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                parts.forEachIndexed { i, part ->
                    val label = ProductPartLabels.getOrElse(i) { "其他" }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(label, color = EvidenceInkMuted, fontSize = BuyPilotType.Label, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
                        Text(part, color = EvidenceInkSecondary, fontSize = BuyPilotType.Body, lineHeight = 22.sp)
                    }
                    if (i != parts.lastIndex) HorizontalDivider(thickness = 1.dp, color = EvidenceBorder)
                }
            }
        }
    }
}

@Composable
private fun EvidenceRiskCard(items: List<EvidenceRiskItemUi>) {
    var selectedIndex by remember(items) { androidx.compose.runtime.mutableIntStateOf(-1) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF5F3), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFFFDDD6), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFFD14A20).copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    color = Color(0xFFE05530),
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = stringResource(R.string.risk_note_title),
                color = Color(0xFFD14A20),
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.risk_note_subtitle),
                color = EvidenceInkMuted,
                fontSize = BuyPilotType.Tiny,
                lineHeight = 14.sp,
            )
        }
        items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { selectedIndex = index }
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                item.label?.let { label ->
                    Text(
                        text = label,
                        color = Color(0xFFD14A20),
                        fontSize = BuyPilotType.Label,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = item.text,
                    color = EvidenceInkSecondary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 22.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.evidence_view_full_arrow),
                    color = Color(0xFFD14A20),
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (index != items.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFFFDDD6))
            }
        }
    }
    selectedIndex
        .takeIf { it in items.indices }
        ?.let { index ->
            EvidenceTextDialog(
                title = stringResource(R.string.risk_note_title),
                body = items[index].text,
                onDismiss = { selectedIndex = -1 },
            )
        }
}

@Composable
private fun EvidenceSnippetCard(evidence: EvidencePayload) {
    val snippet = evidence.cleanSnippet()
    if (snippet.isBlank()) return
    var expanded by rememberSaveable(evidence.evidenceId, snippet) { androidx.compose.runtime.mutableStateOf(false) }
    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
            .clickable(enabled = snippet.length > 96) { expanded = !expanded }
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        EvidenceSourceLabel(evidence = evidence)
        Text(
            text = snippet,
            color = EvidenceInkSecondary,
            fontSize = BuyPilotType.Body,
            lineHeight = 22.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
        )
        if (snippet.length > 96) {
            Text(
                text = if (expanded) "收起" else "展开全文 →",
                color = EvidenceAccent,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EvidenceSourceLabel(evidence: EvidencePayload) {
    val sourceLabels = rememberEvidenceSourceLabels()
    val fallbackLabel = stringResource(R.string.evidence_product_info)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EvidencePill(evidence.sourceType.userFacingEvidenceSourceLabel(fallbackLabel, sourceLabels))
        evidence.trustLabel?.withoutInternalDebugTokens()?.trim()?.takeIf(String::isNotBlank)?.let {
            Text(text = "·", color = EvidenceInkMuted, fontSize = BuyPilotType.Label, lineHeight = 16.sp)
            EvidencePill(it, muted = true)
        }
    }
}

@Composable
private fun EvidencePill(label: String, muted: Boolean = false) {
    Text(
        text = label,
        color = if (muted) EvidenceInkMuted else EvidenceAccent,
        fontSize = BuyPilotType.Label,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun EvidenceSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EvidenceSurface, RoundedCornerShape(16.dp))
            .border(1.dp, EvidenceBorder, RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = EvidenceInk,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = EvidenceInkMuted,
                fontSize = BuyPilotType.Tiny,
                lineHeight = 14.sp,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = EvidenceBorder)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            content()
        }
    }
}

private fun EvidencePayload.cleanSnippet(): String =
    snippet.withoutInternalDebugTokens().trim()

private data class EvidenceRiskItemUi(
    val label: String?,
    val text: String,
)

private fun String.toEvidenceSnippetParts(): List<String> =
    split('|')
        .map { part ->
            part
                .withoutInternalDebugTokens()
                .trim()
                .replace(EvidenceWhitespaceRegex, " ")
        }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .ifEmpty { listOf(this.withoutInternalDebugTokens().trim()) }

private fun List<String>.toEvidenceRiskItems(sourceTexts: List<String>): List<EvidenceRiskItemUi> =
    mapNotNull { raw ->
        val clean = raw
            .withoutMarkdownMarkup()
            .withoutInternalDebugTokens()
            .trim()
            .takeIf(String::isNotBlank)
            ?: return@mapNotNull null
        if (clean.looksLikeQuestionAnswerEvidence()) return@mapNotNull null
        val marker = RiskRatingMarkerRegex.find(clean)
        val label = marker?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
        val text = clean
            .replace(RiskRatingMarkerRegex, "")
            .replace(EvidenceLeadingBulletRegex, "")
            .trim()
            .expandFromSources(sourceTexts)
            .takeIf(String::isNotBlank)
            ?: return@mapNotNull null
        EvidenceRiskItemUi(label = label, text = text)
    }
        .distinctBy { it.text }
        .take(3)

private fun String.expandFromSources(sourceTexts: List<String>): String {
    val probe = substringBefore("...")
        .substringBefore("…")
        .trim()
        .takeIf { it.length >= 12 }
        ?: return this
    val compactProbe = probe.compactForEvidenceMatch()
    return sourceTexts.firstOrNull { source ->
        val compactSource = source.compactForEvidenceMatch()
        compactSource.contains(compactProbe) || compactProbe.take(18).let(compactSource::contains)
    } ?: this
}

private fun String.compactForEvidenceMatch(): String =
    withoutMarkdownMarkup()
        .withoutInternalDebugTokens()
        .replace(EvidenceWhitespaceRegex, "")
        .trim()

private val RiskRatingMarkerRegex = Regex("""\[([^\]\[:：]{1,16})\s+评分[:：]\s*(\d{1,2})\]""")
private val QaEvidenceRegex = Regex("""(?:^|\s)(?:Q|问|问题)\s*[:：].*(?:A|答|回答)\s*[:：]""", RegexOption.IGNORE_CASE)
private val QuestionOnlyEvidenceRegex = Regex("""^\s*(?:Q|问|问题)\s*[:：]""", RegexOption.IGNORE_CASE)

private fun String.looksLikeQuestionAnswerEvidence(): Boolean =
    QaEvidenceRegex.containsMatchIn(this) || QuestionOnlyEvidenceRegex.containsMatchIn(this)

private fun ReasonAtomPayload.userFacingReasonTitle(): String {
    val dimensionLabel = dimension.userFacingReasonDimension()
    val cleanValue = value
        .withoutMarkdownMarkup()
        .withoutInternalDebugTokens()
        .trim()
    return listOf(dimensionLabel, cleanValue)
        .filter(String::isNotBlank)
        .distinct()
        .joinToString(" · ")
        .ifBlank { "匹配项" }
}

private fun String.userFacingReasonDimension(): String =
    when (withoutMarkdownMarkup().withoutInternalDebugTokens().trim().lowercase()) {
        "product_type", "category", "sub_category", "type" -> "品类"
        "budget", "price", "price_range", "budget_max", "budget_min" -> "预算"
        "scenario", "use_case", "use_scenario" -> "使用场景"
        "brand", "brand_prefer", "brand_avoid" -> "品牌"
        "skin_type", "skin" -> "肤质"
        "ingredient", "ingredient_prefer", "ingredient_avoid" -> "成分"
        "storage", "memory" -> "存储"
        "camera", "photo" -> "影像"
        "battery" -> "续航"
        else -> withoutMarkdownMarkup().withoutInternalDebugTokens().trim()
    }

private fun EvidencePayload.linkKey(): String? =
    evidenceId?.trim()?.takeIf(String::isNotBlank)
        ?: sourceId?.trim()?.takeIf(String::isNotBlank)
