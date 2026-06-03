package com.buypilot.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.buypilot.core.model.EvidencePayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.ReasonAtomPayload
import com.buypilot.feature.chat.R
import com.buypilot.feature.chat.state.ChatUiState

private const val ProductEvidenceEnterMs = 560
private val EvidenceBackdropTop = Color(0xFF171A22)
private val EvidenceBackdropMiddle = Color(0xFF10141B)
private val EvidenceBackdropBottom = Color(0xFF0C0F14)
private val EvidencePaper = Color(0xFFFFFEFA)
private val EvidencePaperSoft = Color(0xFFF7F4EF)
private val EvidencePaperBorder = Color(0xFFECE4DA)
private val EvidenceInk = Color(0xFF20242B)
private val EvidenceInkSecondary = Color(0xFF686E77)
private val EvidenceInkMuted = Color(0xFF969DA8)
private val EvidenceQuoteBg = Color(0xFFF4F6F8)

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
        Surface(color = EvidenceBackdropTop, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ProductPageTopBar(title = "推荐证据", onBack = onBack)
                ExpiredRecommendationState(onBack = onBack)
            }
        }
        return
    }

    val product = payload.product
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
    val routeProgressState = rememberRouteEnterProgress(
        key = "evidence_${deckId}_${deckNodeKey.orEmpty()}_${productId}",
        durationMillis = ProductEvidenceEnterMs,
    )
    val progress = routeProgressState.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EvidenceBackdropTop,
                        EvidenceBackdropMiddle,
                        EvidenceBackdropBottom,
                    ),
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 96.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item("lead") {
                EvidenceMotionBlock(progress = progress, start = 0.08f, end = 0.58f, offsetY = 22f) {
                    EvidenceLead()
                }
            }
            item("product") {
                EvidenceMotionBlock(progress = progress, start = 0.18f, end = 0.72f, offsetY = 26f) {
                    EvidenceProductStrip(
                        product = product,
                        backendBaseUrl = state.backendBaseUrl,
                    )
                }
            }
            item("report") {
                EvidenceMotionBlock(progress = progress, start = 0.28f, end = 1f, offsetY = 34f) {
                    EvidenceReportSurface(
                        reason = reason,
                        reasonAtoms = reasonAtoms,
                        evidenceByKey = evidenceByKey,
                        remainingEvidence = remainingEvidence,
                        riskNotes = payload.riskNotes,
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
                .size(46.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White.copy(alpha = 0.92f),
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_24),
                contentDescription = "返回",
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
private fun EvidenceLead() {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = "为什么推荐它",
            color = Color.White,
            fontSize = 26.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "基于当前筛选条件和商品资料",
            color = Color.White.copy(alpha = 0.52f),
            fontSize = BuyPilotType.Label,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EvidenceProductStrip(
    product: ProductPayload,
    backendBaseUrl: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color.White.copy(alpha = 0.86f)),
                contentAlignment = Alignment.Center,
            ) {
                ProductImage(
                    product = product,
                    backendBaseUrl = backendBaseUrl,
                    decodeSizePx = 180,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = product.displayName(),
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = BuyPilotType.LargeBody,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.brandLabel(),
                        color = Color.White.copy(alpha = 0.52f),
                        fontSize = BuyPilotType.Label,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = product.priceLabel(),
                        color = BuyPilotColors.Primary,
                        fontSize = BuyPilotType.LargeBody,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun EvidenceReportSurface(
    reason: String,
    reasonAtoms: List<ReasonAtomPayload>,
    evidenceByKey: Map<String, EvidencePayload>,
    remainingEvidence: List<EvidencePayload>,
    riskNotes: List<String>,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.1f),
            ),
        color = EvidencePaper,
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EvidencePaperBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (reason.isNotBlank()) {
                EvidenceConclusion(reason = reason)
            }
            if (reasonAtoms.isNotEmpty()) {
                EvidenceSoftDivider()
                EvidenceSectionTitle(title = "匹配理由", subtitle = "每条理由都有对应来源")
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    reasonAtoms.forEachIndexed { index, atom ->
                        val evidence = atom.evidenceId
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                            ?.let(evidenceByKey::get)
                        EvidenceReasonRow(index = index, atom = atom, evidence = evidence)
                    }
                }
            }
            val cleanRiskNotes = riskNotes.map { it.withoutInternalDebugTokens().trim() }.filter(String::isNotBlank)
            if (cleanRiskNotes.isNotEmpty()) {
                EvidenceSoftDivider()
                EvidenceRiskCard(notes = cleanRiskNotes)
            }
            if (remainingEvidence.isNotEmpty()) {
                EvidenceSoftDivider()
                EvidenceSectionTitle(title = "证据来源", subtitle = "商品资料、官方问答和用户评价")
                Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    remainingEvidence.take(5).forEach { evidence ->
                        EvidenceSnippetCard(evidence = evidence)
                    }
                }
            }
        }
    }
}

@Composable
private fun EvidenceConclusion(reason: String) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        EvidenceSectionTitle(title = "推荐判断", subtitle = null)
        Text(
            text = reason,
            color = EvidenceInk,
            fontSize = 20.sp,
            lineHeight = 29.sp,
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
    val title = listOf(atom.dimension, atom.value)
        .map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }
        .filter(String::isNotBlank)
        .distinct()
        .joinToString(" · ")
        .ifBlank { "匹配项" }
    val body = atom.text.withoutInternalDebugTokens().trim()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.72f), CircleShape)
                .border(1.dp, BuyPilotColors.Primary.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (index + 1).toString(),
                color = BuyPilotColors.PrimaryDark,
                fontSize = BuyPilotType.Tiny,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = title,
                color = EvidenceInk,
                fontSize = BuyPilotType.LargeBody,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
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
            evidence?.let {
                EvidenceInlineQuote(evidence = it)
            }
        }
    }
}

@Composable
private fun EvidenceInlineQuote(evidence: EvidencePayload) {
    val snippet = evidence.cleanSnippet()
    if (snippet.isBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EvidenceQuoteBg, RoundedCornerShape(14.dp))
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        EvidenceSourceLabel(evidence = evidence)
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

@Composable
private fun EvidenceRiskCard(notes: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.WarningSoft.copy(alpha = 0.86f), RoundedCornerShape(18.dp))
            .border(1.dp, BuyPilotColors.Warning.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "需要留意",
            color = Color(0xFF8F5A00),
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        notes.forEach { note ->
            Text(
                text = note,
                color = EvidenceInkSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun EvidenceSnippetCard(evidence: EvidencePayload) {
    val snippet = evidence.cleanSnippet()
    if (snippet.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EvidenceSourceLabel(evidence = evidence)
        Text(
            text = snippet,
            color = EvidenceInkSecondary,
            fontSize = BuyPilotType.Body,
            lineHeight = 22.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = BuyPilotColors.Border.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun EvidenceSourceLabel(evidence: EvidencePayload) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EvidencePill(evidence.sourceType.userFacingEvidenceSourceLabel("商品资料"))
        evidence.trustLabel?.withoutInternalDebugTokens()?.trim()?.takeIf(String::isNotBlank)?.let {
            EvidencePill(it, muted = true)
        }
    }
}

@Composable
private fun EvidencePill(label: String, muted: Boolean = false) {
    Text(
        text = label,
        color = if (muted) EvidenceInkMuted else BuyPilotColors.PrimaryDark,
        fontSize = BuyPilotType.Tiny,
        lineHeight = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(
                if (muted) BuyPilotColors.SurfaceMuted.copy(alpha = 0.78f) else BuyPilotColors.PrimarySoft.copy(alpha = 0.72f),
                CircleShape,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun EvidenceSectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = EvidenceInk,
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        subtitle?.takeIf(String::isNotBlank)?.let {
            Text(
                text = it,
                color = EvidenceInkMuted,
                fontSize = BuyPilotType.Tiny,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun EvidenceSoftDivider() {
    Spacer(Modifier.height(2.dp))
    HorizontalDivider(
        thickness = 1.dp,
        color = EvidencePaperBorder.copy(alpha = 0.86f),
    )
}

private fun EvidencePayload.cleanSnippet(): String =
    snippet.withoutInternalDebugTokens().trim()

private fun EvidencePayload.linkKey(): String? =
    evidenceId?.trim()?.takeIf(String::isNotBlank)
        ?: sourceId?.trim()?.takeIf(String::isNotBlank)
