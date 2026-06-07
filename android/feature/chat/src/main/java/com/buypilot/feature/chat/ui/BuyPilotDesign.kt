package com.buypilot.feature.chat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
internal data class BuyPilotColorScheme(
    val Primary: Color,
    val PrimarySoft: Color,
    val PrimaryDark: Color,
    val OnPrimary: Color,
    val SurfaceBg: Color,
    val SurfaceCard: Color,
    val SurfaceMuted: Color,
    val SurfaceImage: Color,
    val SurfaceImageAlt: Color,
    val SurfaceSubtle: Color,
    val Border: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextMuted: Color,
    val Info: Color,
    val InfoSoft: Color,
    val Success: Color,
    val SuccessSoft: Color,
    val Warning: Color,
    val WarningSoft: Color,
    val Attention: Color,
    val Danger: Color,
    val ShadowNeutral: Color,
    val ThinkingShimmer: Color,
    val ClarificationChipBackground: Color,
    val ClarificationChipPressed: Color,
    val ClarificationChipBorder: Color,
    val ClarificationChipPressedBorder: Color,
    val ClarificationManualBackground: Color,
    val ClarificationManualBorder: Color,
    val CriteriaCardTop: Color,
    val CriteriaCardBottom: Color,
    val CriteriaCardBorderTop: Color,
    val CriteriaCardBorderBottom: Color,
    val CriteriaTagBackground: Color,
    val CriteriaTagBorder: Color,
    val DecisionReasonWarmBackground: Color,
    val DecisionReasonWarmBorder: Color,
    val DecisionReasonWarmText: Color,
    val DecisionReasonBlueBorder: Color,
    val DecisionReasonBlueText: Color,
    val DecisionReasonGreenBackground: Color,
    val DecisionReasonGreenBorder: Color,
    val DecisionReasonGreenText: Color,
    val DecisionReasonPinkBackground: Color,
    val DecisionReasonPinkBorder: Color,
    val DecisionReasonPinkText: Color,
    val ProductMiniTagBackground: Color,
    val ProductMiniTagBorder: Color,
    val ProductSelectionLine: Color,
    val EvidenceBackdropTop: Color,
    val EvidenceBackdropMiddle: Color,
    val EvidenceBackdropBottom: Color,
    val EvidenceRiskBackground: Color,
    val EvidenceRiskAccent: Color,
    val MarkdownSoftBlock: Color,
)

@Immutable
internal data class BuyPilotDimensionScheme(
    val RadiusSm: Dp,
    val RadiusMd: Dp,
    val RadiusLg: Dp,
    val RadiusXl: Dp,
    val PagePadding: Dp,
    val TopBarHeight: Dp,
    val ComposerHeight: Dp,
)

@Immutable
internal data class BuyPilotTypographyScheme(
    val Display: TextUnit,
    val Hero: TextUnit,
    val Title: TextUnit,
    val Body: TextUnit,
    val LargeBody: TextUnit,
    val Label: TextUnit,
    val Tiny: TextUnit,
)

private val BuyPilotLightColors = BuyPilotColorScheme(
    Primary = Color(0xFFFF6A3D),
    PrimarySoft = Color(0xFFFFE5DA),
    PrimaryDark = Color(0xFFAE3104),
    OnPrimary = Color(0xFFFFFBFF),
    SurfaceBg = Color(0xFFF7F8FA),
    SurfaceCard = Color(0xFFFFFEFC),
    SurfaceMuted = Color(0xFFF1F3FC),
    SurfaceImage = Color(0xFFF6F8FA),
    SurfaceImageAlt = Color(0xFFF6F8FB),
    SurfaceSubtle = Color(0xFFF3F5F8),
    Border = Color(0xFFE2E7EE),
    TextPrimary = Color(0xFF181C22),
    TextSecondary = Color(0xFF646A73),
    TextMuted = Color(0xFF8A919F),
    Info = Color(0xFF3B82F6),
    InfoSoft = Color(0xFFEDF5FF),
    Success = Color(0xFF22C55E),
    SuccessSoft = Color(0xFFF0FBF4),
    Warning = Color(0xFFF5A524),
    WarningSoft = Color(0xFFFFF7E8),
    Attention = Color(0xFFFFF0F2),
    Danger = Color(0xFFBA1A1A),
    ShadowNeutral = Color(0xFF8E97A4),
    ThinkingShimmer = Color(0xFFA4AAB3),
    ClarificationChipBackground = Color(0xFFFFF8F6),
    ClarificationChipPressed = Color(0xFFEDE3DF),
    ClarificationChipBorder = Color(0xFFEDE0DC),
    ClarificationChipPressedBorder = Color(0xFFE0C9C2),
    ClarificationManualBackground = Color(0xFFF5F7FA),
    ClarificationManualBorder = Color(0xFFE8ECF2),
    CriteriaCardTop = Color(0xFFFFFEFD),
    CriteriaCardBottom = Color(0xFFF9FAFB),
    CriteriaCardBorderTop = Color(0xFFE8ECF0),
    CriteriaCardBorderBottom = Color(0xFFDFE3E8),
    CriteriaTagBackground = Color(0xFFF0F2F5),
    CriteriaTagBorder = Color(0xFFE4E8ED),
    DecisionReasonWarmBackground = Color(0xFFFFF2EA),
    DecisionReasonWarmBorder = Color(0xFFFFCFBA),
    DecisionReasonWarmText = Color(0xFFB24617),
    DecisionReasonBlueBorder = Color(0xFFCFE3FF),
    DecisionReasonBlueText = Color(0xFF245CBA),
    DecisionReasonGreenBackground = Color(0xFFEFFBF8),
    DecisionReasonGreenBorder = Color(0xFFCBEFE7),
    DecisionReasonGreenText = Color(0xFF16745F),
    DecisionReasonPinkBackground = Color(0xFFFFF1F6),
    DecisionReasonPinkBorder = Color(0xFFFAD4E1),
    DecisionReasonPinkText = Color(0xFF9C315F),
    ProductMiniTagBackground = Color(0xFFF2F7FA),
    ProductMiniTagBorder = Color(0xFFDCE8F0),
    ProductSelectionLine = Color(0xFFFFC4B0),
    EvidenceBackdropTop = Color(0xFF1A1D23),
    EvidenceBackdropMiddle = Color(0xFF13151A),
    EvidenceBackdropBottom = Color(0xFF0F1115),
    EvidenceRiskBackground = Color(0xFF2A1A1A),
    EvidenceRiskAccent = Color(0xFFFF6B6B),
    MarkdownSoftBlock = Color(0xFFEFF2F5),
)

private val BuyPilotDefaultDimens = BuyPilotDimensionScheme(
    RadiusSm = 8.dp,
    RadiusMd = 12.dp,
    RadiusLg = 16.dp,
    RadiusXl = 24.dp,
    PagePadding = 16.dp,
    TopBarHeight = 64.dp,
    ComposerHeight = 91.dp,
)

private val BuyPilotDefaultType = BuyPilotTypographyScheme(
    Display = 30.sp,
    Hero = 48.sp,
    Title = 18.sp,
    Body = 14.sp,
    LargeBody = 16.sp,
    Label = 12.sp,
    Tiny = 10.sp,
)

private val LocalBuyPilotColors = staticCompositionLocalOf { BuyPilotLightColors }
private val LocalBuyPilotDimens = staticCompositionLocalOf { BuyPilotDefaultDimens }
private val LocalBuyPilotType = staticCompositionLocalOf { BuyPilotDefaultType }

@Composable
internal fun BuyPilotTheme(
    colors: BuyPilotColorScheme = BuyPilotLightColors,
    dimens: BuyPilotDimensionScheme = BuyPilotDefaultDimens,
    type: BuyPilotTypographyScheme = BuyPilotDefaultType,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBuyPilotColors provides colors,
        LocalBuyPilotDimens provides dimens,
        LocalBuyPilotType provides type,
        content = content,
    )
}

internal object BuyPilotThemeTokens {
    val colors: BuyPilotColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalBuyPilotColors.current

    val dimens: BuyPilotDimensionScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalBuyPilotDimens.current

    val type: BuyPilotTypographyScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalBuyPilotType.current
}

internal object BuyPilotColors {
    val Primary = BuyPilotLightColors.Primary
    val PrimarySoft = BuyPilotLightColors.PrimarySoft
    val PrimaryDark = BuyPilotLightColors.PrimaryDark
    val OnPrimary = BuyPilotLightColors.OnPrimary
    val SurfaceBg = BuyPilotLightColors.SurfaceBg
    val SurfaceCard = BuyPilotLightColors.SurfaceCard
    val SurfaceMuted = BuyPilotLightColors.SurfaceMuted
    val SurfaceImage = BuyPilotLightColors.SurfaceImage
    val SurfaceImageAlt = BuyPilotLightColors.SurfaceImageAlt
    val SurfaceSubtle = BuyPilotLightColors.SurfaceSubtle
    val Border = BuyPilotLightColors.Border
    val TextPrimary = BuyPilotLightColors.TextPrimary
    val TextSecondary = BuyPilotLightColors.TextSecondary
    val TextMuted = BuyPilotLightColors.TextMuted
    val Info = BuyPilotLightColors.Info
    val InfoSoft = BuyPilotLightColors.InfoSoft
    val Success = BuyPilotLightColors.Success
    val SuccessSoft = BuyPilotLightColors.SuccessSoft
    val Warning = BuyPilotLightColors.Warning
    val WarningSoft = BuyPilotLightColors.WarningSoft
    val Attention = BuyPilotLightColors.Attention
    val Danger = BuyPilotLightColors.Danger
    val ShadowNeutral = BuyPilotLightColors.ShadowNeutral
    val ThinkingShimmer = BuyPilotLightColors.ThinkingShimmer
    val ClarificationChipBackground = BuyPilotLightColors.ClarificationChipBackground
    val ClarificationChipPressed = BuyPilotLightColors.ClarificationChipPressed
    val ClarificationChipBorder = BuyPilotLightColors.ClarificationChipBorder
    val ClarificationChipPressedBorder = BuyPilotLightColors.ClarificationChipPressedBorder
    val ClarificationManualBackground = BuyPilotLightColors.ClarificationManualBackground
    val ClarificationManualBorder = BuyPilotLightColors.ClarificationManualBorder
    val CriteriaCardTop = BuyPilotLightColors.CriteriaCardTop
    val CriteriaCardBottom = BuyPilotLightColors.CriteriaCardBottom
    val CriteriaCardBorderTop = BuyPilotLightColors.CriteriaCardBorderTop
    val CriteriaCardBorderBottom = BuyPilotLightColors.CriteriaCardBorderBottom
    val CriteriaTagBackground = BuyPilotLightColors.CriteriaTagBackground
    val CriteriaTagBorder = BuyPilotLightColors.CriteriaTagBorder
    val DecisionReasonWarmBackground = BuyPilotLightColors.DecisionReasonWarmBackground
    val DecisionReasonWarmBorder = BuyPilotLightColors.DecisionReasonWarmBorder
    val DecisionReasonWarmText = BuyPilotLightColors.DecisionReasonWarmText
    val DecisionReasonBlueBorder = BuyPilotLightColors.DecisionReasonBlueBorder
    val DecisionReasonBlueText = BuyPilotLightColors.DecisionReasonBlueText
    val DecisionReasonGreenBackground = BuyPilotLightColors.DecisionReasonGreenBackground
    val DecisionReasonGreenBorder = BuyPilotLightColors.DecisionReasonGreenBorder
    val DecisionReasonGreenText = BuyPilotLightColors.DecisionReasonGreenText
    val DecisionReasonPinkBackground = BuyPilotLightColors.DecisionReasonPinkBackground
    val DecisionReasonPinkBorder = BuyPilotLightColors.DecisionReasonPinkBorder
    val DecisionReasonPinkText = BuyPilotLightColors.DecisionReasonPinkText
    val ProductMiniTagBackground = BuyPilotLightColors.ProductMiniTagBackground
    val ProductMiniTagBorder = BuyPilotLightColors.ProductMiniTagBorder
    val ProductSelectionLine = BuyPilotLightColors.ProductSelectionLine
    val EvidenceBackdropTop = BuyPilotLightColors.EvidenceBackdropTop
    val EvidenceBackdropMiddle = BuyPilotLightColors.EvidenceBackdropMiddle
    val EvidenceBackdropBottom = BuyPilotLightColors.EvidenceBackdropBottom
    val EvidenceRiskBackground = BuyPilotLightColors.EvidenceRiskBackground
    val EvidenceRiskAccent = BuyPilotLightColors.EvidenceRiskAccent
    val MarkdownSoftBlock = BuyPilotLightColors.MarkdownSoftBlock
}

internal object BuyPilotDimens {
    val RadiusSm = BuyPilotDefaultDimens.RadiusSm
    val RadiusMd = BuyPilotDefaultDimens.RadiusMd
    val RadiusLg = BuyPilotDefaultDimens.RadiusLg
    val RadiusXl = BuyPilotDefaultDimens.RadiusXl
    val PagePadding = BuyPilotDefaultDimens.PagePadding
    val TopBarHeight = BuyPilotDefaultDimens.TopBarHeight
    val ComposerHeight = BuyPilotDefaultDimens.ComposerHeight
}

internal object BuyPilotType {
    val Display = BuyPilotDefaultType.Display
    val Hero = BuyPilotDefaultType.Hero
    val Title = BuyPilotDefaultType.Title
    val Body = BuyPilotDefaultType.Body
    val LargeBody = BuyPilotDefaultType.LargeBody
    val Label = BuyPilotDefaultType.Label
    val Tiny = BuyPilotDefaultType.Tiny
}
