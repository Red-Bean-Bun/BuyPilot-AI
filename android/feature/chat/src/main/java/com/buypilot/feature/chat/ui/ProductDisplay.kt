package com.buypilot.feature.chat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.feature.chat.R

@Composable
internal fun ProductMockImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.product_image_placeholder),
            contentDescription = stringResource(R.string.product_image_desc),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
internal fun ProductImage(
    product: ProductPayload,
    backendBaseUrl: String,
    modifier: Modifier = Modifier,
    decodeSizePx: Int? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val imageUrl = product.imageUrl.resolveProductImageUrl(backendBaseUrl)
    if (imageUrl == null) {
        ProductMockImage(modifier)
        return
    }
    val model = remember(imageUrl, decodeSizePx) {
        if (decodeSizePx == null) {
            imageUrl
        } else {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .size(decodeSizePx)
                .crossfade(false)
                .build()
        }
    }
    AsyncImage(
        model = model,
        contentDescription = product.displayName("商品图片"),
        modifier = modifier,
        contentScale = contentScale,
        error = painterResource(R.drawable.product_image_placeholder),
        fallback = painterResource(R.drawable.product_image_placeholder),
        placeholder = painterResource(R.drawable.product_image_placeholder),
    )
}

internal fun ProductPayload.priceLabel(): String =
    price?.let { "${currency.priceSymbol()}${it.clean()}" } ?: "价格待确认"

internal fun ProductPayload.priceLabelOrNull(): String? =
    price?.let { "${currency.priceSymbol()}${it.clean()}" }

internal fun ProductPayload.displayName(fallback: String = "推荐商品"): String =
    name.withoutInternalDebugTokens().ifBlank { fallback }

private fun String?.priceSymbol(): String =
    when (this?.trim()?.uppercase()) {
        null, "", "CNY", "RMB", "CN¥", "￥", "¥" -> "¥"
        "USD", "$" -> "$"
        else -> this.trim()
    }

internal fun ProductPayload.brandLabel(): String =
    brand?.withoutInternalDebugTokens()?.takeIf { it.isNotBlank() }
        ?: subCategory?.withoutInternalDebugTokens()?.takeIf { it.isNotBlank() }
        ?: category.withoutInternalDebugTokens().takeIf { it.isNotBlank() }
        ?: "BuyPilot 推荐"

internal fun List<String>.userFacingJoinedOrFallback(fallback: String = ""): String =
    map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("、")
        .ifBlank { fallback }

internal fun ProductCardPayload.displayTags(): List<String> =
    (product.ingredientTags + product.skinTypeMatch + product.useScenario)
        .map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)

internal fun String?.resolveProductImageUrl(backendBaseUrl: String): String? {
    val raw = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
    if (raw.startsWith("/assets/products/") || raw.startsWith("/uploads/")) {
        return backendBaseUrl.trimEnd('/') + raw
    }
    return raw
}

@Preview(
    name = "Product Image Placeholder",
    showBackground = true,
    backgroundColor = 0xFFF7F8FA,
)
@Composable
private fun ProductImagePreview() {
    Box(
        modifier = Modifier
            .background(BuyPilotColors.SurfaceBg)
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        ProductImage(
            product = ProductPayload(
                productId = "preview-1",
                name = "示例商品",
                category = "数码电子",
                price = 3999.0,
            ),
            backendBaseUrl = "",
            modifier = Modifier.size(160.dp),
        )
    }
}
