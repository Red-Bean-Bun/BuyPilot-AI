package com.buypilot.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.CriteriaPayload
import com.buypilot.feature.chat.R
import kotlin.math.abs
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private const val CriteriaCardEnterMs = 560
private val BudgetBasePresets = listOf(50, 100, 150, 200, 300, 500, 800, 1000)
private val BudgetHighPresets = listOf(1500, 2000, 3000, 5000, 8000, 10000)
internal const val DefaultBudgetPreset = 200

private data class CriteriaReceiptProperty(
    val label: String,
    val value: String,
)

@Immutable
internal data class CriteriaLabels(
    val summary: String,
    val category: String,
    val coreNeed: String,
    val targetUser: String,
    val budget: String,
    val scenario: String,
    val exclusions: String,
    val avoidPrefix: String,
)

@Composable
internal fun rememberCriteriaLabels(): CriteriaLabels =
    CriteriaLabels(
        summary = stringResource(R.string.criteria_label_summary),
        category = stringResource(R.string.criteria_label_category),
        coreNeed = stringResource(R.string.criteria_label_core_need),
        targetUser = stringResource(R.string.criteria_label_target_user),
        budget = stringResource(R.string.criteria_label_budget),
        scenario = stringResource(R.string.criteria_label_scenario),
        exclusions = stringResource(R.string.criteria_label_exclusions),
        avoidPrefix = stringResource(R.string.criteria_avoid_prefix),
    )

@Composable
internal fun CriteriaSummaryCard(
    motionKey: String,
    payload: CriteriaCardPayload,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    onEntered: () -> Unit,
    onEdit: () -> Unit,
) {
    val criteria = payload.criteria
    val summary = criteria.summary.withoutMarkdownMarkup().trim()
    val labels = rememberCriteriaLabels()
    val editLabel = stringResource(R.string.criteria_edit_title)
    val properties = criteria.receiptProperties(labels)
    val headline = criteria.criteriaReceiptHeadline().ifBlank { summary }

    StructuredCardMotion(
        key = motionKey,
        motionEnabled = motionEnabled,
        alreadyEntered = alreadyEntered,
        durationMillis = CriteriaCardEnterMs,
        initialOffsetY = 8.dp,
        initialScale = 1f,
        onEntered = onEntered,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.04f),
                    spotColor = Color.Black.copy(alpha = 0.06f),
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BuyPilotColors.CriteriaCardTop,
                            BuyPilotColors.CriteriaCardBottom,
                        ),
                    ),
                    RoundedCornerShape(16.dp),
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            BuyPilotColors.CriteriaCardBorderTop,
                            BuyPilotColors.CriteriaCardBorderBottom,
                        ),
                    ),
                    RoundedCornerShape(16.dp),
                ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StaggeredRevealMotion(
                    key = "${motionKey}_criteria_header",
                    motionEnabled = motionEnabled,
                    alreadyEntered = alreadyEntered,
                    delayMillis = 0,
                    durationMillis = 240,
                    initialOffsetY = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(BuyPilotColors.Primary, CircleShape),
                            )
                            Text(
                                text = "筛选条件",
                                color = BuyPilotColors.TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                            )
                        }
                        Text(
                            text = editLabel,
                            color = BuyPilotColors.Primary.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button, onClick = onEdit)
                                .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
                if (headline.isNotBlank()) {
                    StaggeredRevealMotion(
                        key = "${motionKey}_criteria_headline",
                        motionEnabled = motionEnabled,
                        alreadyEntered = alreadyEntered,
                        delayMillis = 70,
                        durationMillis = 260,
                        initialOffsetY = 5.dp,
                    ) {
                        Text(
                            text = headline,
                            color = BuyPilotColors.TextPrimary,
                            fontSize = 17.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (properties.isNotEmpty()) {
                    CriteriaReceiptTags(
                        properties = properties,
                        motionKey = motionKey,
                        motionEnabled = motionEnabled,
                        alreadyEntered = alreadyEntered,
                    )
                }
            }
        }
    }
}

@Composable
private fun CriteriaReceiptTags(
    properties: List<CriteriaReceiptProperty>,
    motionKey: String,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(end = 4.dp),
    ) {
        itemsIndexed(
            items = properties,
            key = { _, property -> "${property.label}:${property.value}" },
        ) { index, property ->
            StaggeredRevealMotion(
                key = "${motionKey}_criteria_tag_${property.label}_${property.value}",
                motionEnabled = motionEnabled,
                alreadyEntered = alreadyEntered,
                delayMillis = 120 + index.coerceAtMost(3) * 34,
                durationMillis = 240,
                initialOffsetY = 4.dp,
            ) {
                CriteriaReceiptTag(property = property)
            }
        }
    }
}

@Composable
private fun CriteriaReceiptTag(
    property: CriteriaReceiptProperty,
) {
    Text(
        text = property.value,
        modifier = Modifier
            .background(BuyPilotColors.CriteriaTagBackground, RoundedCornerShape(10.dp))
            .border(1.dp, BuyPilotColors.CriteriaTagBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = BuyPilotColors.TextPrimary.copy(alpha = 0.78f),
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
internal fun CriteriaPayload.budgetLabel(): String {
    val max = budgetMax ?: constraints?.budgetMax
    val min = budgetMin ?: constraints?.budgetMin
    return when {
        min != null && max != null -> "¥${min.clean()}-${max.clean()}"
        max != null -> "${max.clean()}元以内"
        min != null -> "¥${min.clean()}以上"
        else -> ""
    }
}

private fun CriteriaPayload.criteriaReceiptHeadline(): String {
    val categoryLabel = category.withoutMarkdownMarkup().trim()
    val productType = productTypeLabel().withoutMarkdownMarkup().trim()
    return listOf(categoryLabel, productType)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("  /  ")
}

private fun CriteriaPayload.receiptProperties(
    labels: CriteriaLabels,
): List<CriteriaReceiptProperty> {
    val headline = criteriaReceiptHeadline()
    val skinType = skinTypeLabel().withoutMarkdownMarkup().trim()
    val budget = budgetLabel().withoutMarkdownMarkup().trim()
    val scenario = useScenarioLabel().withoutMarkdownMarkup().trim()
    val properties = buildList {
        chips.forEach { chip ->
            add(CriteriaReceiptProperty(label = labels.summary, value = chip))
        }
        if (skinType.isNotBlank()) {
            add(CriteriaReceiptProperty(label = labels.targetUser, value = skinType))
        }
        if (budget.isNotBlank()) {
            add(CriteriaReceiptProperty(label = labels.budget, value = budget))
        }
        if (scenario.isNotBlank()) {
            add(CriteriaReceiptProperty(label = labels.scenario, value = scenario))
        }
        productSpecificReceiptValues().forEach { value ->
            add(CriteriaReceiptProperty(label = labels.coreNeed, value = value))
        }
        exclusionLabels().forEach { value ->
            add(CriteriaReceiptProperty(label = labels.exclusions, value = "${labels.avoidPrefix}$value"))
        }
    }
    return properties
        .mapNotNull { it.compactReceiptPropertyOrNull(headline) }
        .distinctBy { it.value }
        .take(4)
}

private fun CriteriaReceiptProperty.compactReceiptPropertyOrNull(
    headline: String,
): CriteriaReceiptProperty? {
    val compactValue = value
        .withoutMarkdownMarkup()
        .replace(Regex("\\s+"), " ")
        .trim()
    return copy(value = compactValue).takeIf {
        compactValue.isNotBlank() &&
            compactValue.length <= 14 &&
            !headline.contains(compactValue)
    }
}

private fun CriteriaPayload.productSpecificReceiptValues(): List<String> =
    buildList {
        addAll(ingredientPrefer)
        addAll(constraints?.ingredientPrefer.orEmpty())
        storage.orEmpty().ifBlank { constraints?.storage.orEmpty() }.takeIf { it.isNotBlank() }?.let { add(it) }
        screenSize.orEmpty().ifBlank { constraints?.screenSize.orEmpty() }.takeIf { it.isNotBlank() }?.let { add(it) }
        sportType.orEmpty().ifBlank { constraints?.sportType.orEmpty() }.takeIf { it.isNotBlank() }?.let { add(it) }
        season.orEmpty().ifBlank { constraints?.season.orEmpty() }.takeIf { it.isNotBlank() }?.let { add(it) }
        addAll(dietary)
        addAll(constraints?.dietary.orEmpty())
    }

internal fun CriteriaPayload.productTypeLabel(): String =
    productType.orEmpty().ifBlank { constraints?.productType.orEmpty() }

internal fun CriteriaPayload.skinTypeLabel(): String =
    skinType.orEmpty().ifBlank { constraints?.skinType.orEmpty() }

internal fun CriteriaPayload.useScenarioLabel(): String =
    useScenario.firstOrNull().orEmpty().ifBlank { constraints?.useScenario.orEmpty() }

internal fun CriteriaPayload.budgetMaxLabel(): String {
    val max = budgetMax ?: constraints?.budgetMax
    return max?.clean().orEmpty()
}

internal fun CriteriaPayload.exclusionLabels(): List<String> =
    (
        ingredientAvoid + constraints?.ingredientAvoid.orEmpty() +
            brandAvoid + constraints?.brandAvoid.orEmpty() +
            originAvoid + constraints?.originAvoid.orEmpty()
        )
        .map { it.withoutAvoidPrefix().trim() }
        .filter { it.isNotBlank() }
        .distinct()

internal fun buildCriteriaPatch(
    productType: String,
    budgetMax: String,
    skinType: String,
    useScenario: String,
    exclusions: String,
): JsonObject {
    val exclusionItems = exclusions.split('、', ',', '，', ';', '；', '\n')
        .map { it.withoutAvoidPrefix().trim() }
        .filter { it.isNotBlank() }
        .distinct()
    val originAvoid = exclusionItems.filter { it.looksLikeOriginAvoidance() }
    val ingredientAvoid = exclusionItems.filterNot { it.looksLikeOriginAvoidance() || it.looksLikeBrandAvoidance() }
    val brandAvoid = exclusionItems.filter { it.looksLikeBrandAvoidance() }
    return buildJsonObject {
        putJsonObject("constraints") {
            productType.trim().takeIf { it.isNotBlank() }?.let { put("product_type", it) }
            budgetMax.extractFirstNumber()?.let { put("budget_max", it) }
            skinType.trim().withoutSkinSuffix().takeIf { it.isNotBlank() }?.let { put("skin_type", it) }
            useScenario.trim().takeIf { it.isNotBlank() }?.let { put("use_scenario", it) }
            if (ingredientAvoid.isNotEmpty()) {
                put(
                    "ingredient_avoid",
                    buildJsonArray {
                        ingredientAvoid.forEach { add(JsonPrimitive(it)) }
                    },
                )
            }
            if (brandAvoid.isNotEmpty()) {
                put(
                    "brand_avoid",
                    buildJsonArray {
                        brandAvoid.forEach { add(JsonPrimitive(it)) }
                    },
                )
            }
            if (originAvoid.isNotEmpty()) {
                put(
                    "origin_avoid",
                    buildJsonArray {
                        originAvoid.forEach { add(JsonPrimitive(it)) }
                    },
                )
            }
        }
    }
}

internal fun String.withoutAvoidPrefix(): String =
    trim()
        .removePrefix("不要含")
        .removePrefix("不要")
        .removePrefix("排除")

internal fun String.withoutSkinSuffix(): String =
    trim()
        .removeSuffix("肌肤")
        .removeSuffix("肤质")
        .removeSuffix("肌")

internal fun String.extractFirstNumber(): Double? =
    Regex("""\d+(?:\.\d+)?""").find(this)?.value?.toDoubleOrNull()

internal fun budgetSliderOptions(currentBudget: Int?): List<Int> {
    val positiveBudget = currentBudget?.takeIf { it > 0 }
    val ceiling = when {
        positiveBudget == null -> BudgetBasePresets.last()
        positiveBudget <= BudgetBasePresets.last() -> BudgetBasePresets.last()
        else -> BudgetHighPresets.firstOrNull { it >= positiveBudget } ?: positiveBudget.roundUpBudgetCeiling()
    }
    return (BudgetBasePresets + BudgetHighPresets.filter { it <= ceiling } + listOfNotNull(positiveBudget))
        .distinct()
        .sorted()
}

internal fun Int.nearestBudgetOption(options: List<Int>): Int =
    options.minByOrNull { abs(it - this) } ?: this

private fun Int.roundUpBudgetCeiling(): Int {
    val step = 5000
    return ((this + step - 1) / step) * step
}

internal fun List<Int>.midBudgetLabel(): String {
    val midBudget = firstOrNull { it >= 500 } ?: get(size / 2)
    return "¥$midBudget"
}

private fun String.looksLikeOriginAvoidance(): Boolean =
    listOf("日系", "日本", "韩系", "韩国", "欧美", "国产", "进口").any { it in this }

private fun String.looksLikeBrandAvoidance(): Boolean =
    any { it in 'A'..'Z' || it in 'a'..'z' } || contains("-") || contains("·")
