package com.buypilot.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.buypilot.core.model.EvidencePayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.feature.chat.R
import com.buypilot.feature.chat.state.ChatUiState

private const val ProductEvidenceEnterMs = 520

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEvidenceOverlayScreen(
    state: ChatUiState,
    deckId: String,
    productId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val payload = state.findProduct(deckId, productId)

    if (payload == null) {
        Surface(color = BuyPilotColors.EvidenceBackdropTop, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ProductPageTopBar(title = "推荐证据", onBack = onBack)
                ExpiredRecommendationState(onBack = onBack)
            }
        }
        return
    }

    val product = payload.product
    val evidenceItems = payload.evidence
    val highlightTags = payload.displayTags()
    val routeProgressState = rememberRouteEnterProgress(
        key = "evidence_${deckId}_${productId}",
        durationMillis = ProductEvidenceEnterMs,
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BuyPilotColors.EvidenceBackdropTop,
                        BuyPilotColors.EvidenceBackdropMiddle,
                        BuyPilotColors.EvidenceBackdropBottom,
                    ),
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val contentEnter = segmentProgress(routeProgressState.value, 0.18f, 1f)
                    alpha = contentEnter
                    translationY = (1f - contentEnter) * 36f
                }
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 132.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            item("header") {
                MagazineProductHeader(product = product)
            }
            payload.reason.withoutInternalDebugTokens().trim().takeIf { it.isNotBlank() }?.let { reason ->
                item("reason") {
                    Text(
                        text = reason,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 22.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
            if (highlightTags.isNotEmpty()) {
                item("highlights") {
                    MagazineHighlightChips(tags = highlightTags)
                }
            }
            if (payload.riskNotes.isNotEmpty()) {
                item("risks") {
                    MagazineRiskCard(notes = payload.riskNotes)
                }
            }
            if (evidenceItems.isNotEmpty()) {
                item("evidence_header") {
                    Text(
                        text = "推荐依据",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                }
                itemsIndexed(
                    evidenceItems,
                    key = { index, item -> item.evidenceId ?: item.sourceId ?: "${item.sourceType}_$index" },
                ) { _, evidence ->
                    MagazineEvidenceQuote(evidence = evidence)
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp)
                .graphicsLayer {
                    val chromeEnter = segmentProgress(routeProgressState.value, 0.12f, 0.72f)
                    alpha = chromeEnter
                    translationY = (1f - chromeEnter) * -12f
                }
                .zIndex(2f)
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White.copy(alpha = 0.9f),
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
private fun MagazineProductHeader(product: ProductPayload) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = product.brandLabel(),
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
        Text(
            text = product.displayName(),
            color = Color.White,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = product.priceLabel(),
            color = BuyPilotColors.Primary,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MagazineHighlightChips(tags: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Text(
                text = tag,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun MagazineRiskCard(notes: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.EvidenceRiskBackground, RoundedCornerShape(16.dp))
            .border(1.dp, BuyPilotColors.EvidenceRiskAccent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "注意事项",
            color = BuyPilotColors.EvidenceRiskAccent,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        notes.forEach { note ->
            val displayNote = note.withoutInternalDebugTokens().trim()
            if (displayNote.isNotBlank()) {
                Text(
                    text = displayNote,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}

@Composable
private fun MagazineEvidenceQuote(evidence: EvidencePayload) {
    val snippet = evidence.snippet.withoutInternalDebugTokens().trim()
    if (snippet.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = evidence.sourceType.userFacingEvidenceSourceLabel("商品资料"),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "“$snippet”",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Normal,
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.08f),
        )
    }
}
