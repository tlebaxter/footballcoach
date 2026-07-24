package achijones.footballcoach.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import achijones.footballcoach.ui.components.TeamColors
import achijones.footballcoach.ui.components.rememberTeamColors
import kotlin.math.pow

val FcPrimary = Color(0xFF26A69A)
val FcPrimaryDark = Color(0xFF00897B)
val FcOnPrimary = Color(0xFF00332E)
val FcAccent = Color(0xFFFF8A50)
val FcOnAccent = Color(0xFF3E1400)
val FcSurface = Color(0xFF1E1E1E)
val FcOnSurface = Color(0xFFE6E1E5)
val FcOnSurfaceVariant = Color(0xFFCAC4D0)
val FcOutline = Color(0xFF938F99)
val FcSurfaceVariant = Color(0xFF2A2A2A)
val FcBackground = Color(0xFF121212)
val FcWin = Color(0xFF1B5E20)
val FcLoss = Color(0xFF7F1D1D)
val FcBye = Color(0xFF3D4A5C)
val FcOvrElite = Color(0xFFFFB300)
val FcOvrStarter = Color(0xFF26A69A)
val FcOvrDepth = Color(0xFF9E9E9E)
val FcChipMoneyBg = Color(0xFF4E2600)
val FcChipMoneyText = Color(0xFFFFB74D)
val FcChipPosBg = Color(0xFF0D47A1)
val FcChipPosText = Color(0xFF90CAF9)
val FcPhaseRetention = Color(0xFF26A69A)
val FcPhasePortal = Color(0xFF64B5F6)
val FcPhaseHs = Color(0xFFFF8A50)
val FcSegmentTrack = Color(0xFF3A3A3C)
val FcSegmentBorder = Color(0xFF525254)
val FcSegmentSelected = Color(0xFF00897B)
val FcSegmentSelectedText = Color(0xFFFFFFFF)
val FcSegmentUnselectedText = Color(0xFFB0B0B0)

private val FcDarkColors = darkColorScheme(
    primary = FcPrimary,
    onPrimary = FcOnPrimary,
    primaryContainer = FcPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = FcAccent,
    onSecondary = FcOnAccent,
    secondaryContainer = Color(0xFF4E2600),
    onSecondaryContainer = FcChipMoneyText,
    background = FcBackground,
    onBackground = FcOnSurface,
    surface = FcSurface,
    onSurface = FcOnSurface,
    surfaceVariant = FcSurfaceVariant,
    onSurfaceVariant = FcOnSurfaceVariant,
    outline = FcOutline,
    error = Color(0xFFEF5350),
    onError = Color.White,
)

private val BrandAnimSpec = tween<Color>(durationMillis = 350)

@Composable
fun FcTheme(content: @Composable () -> Unit) {
    val brandName = UserBrandTheme.teamName
    val brandAbbr = UserBrandTheme.abbr
    val brand = if (!brandName.isNullOrBlank() && !brandAbbr.isNullOrBlank()) {
        rememberTeamColors(brandName, brandAbbr)
    } else {
        null
    }
    val targets = brandSchemeTargets(brand)

    val primary by animateColorAsState(targets.primary, BrandAnimSpec, label = "brandPrimary")
    val onPrimary by animateColorAsState(targets.onPrimary, BrandAnimSpec, label = "brandOnPrimary")
    val primaryContainer by animateColorAsState(
        targets.primaryContainer,
        BrandAnimSpec,
        label = "brandPrimaryContainer",
    )
    val onPrimaryContainer by animateColorAsState(
        targets.onPrimaryContainer,
        BrandAnimSpec,
        label = "brandOnPrimaryContainer",
    )
    val secondary by animateColorAsState(targets.secondary, BrandAnimSpec, label = "brandSecondary")
    val onSecondary by animateColorAsState(targets.onSecondary, BrandAnimSpec, label = "brandOnSecondary")
    val secondaryContainer by animateColorAsState(
        targets.secondaryContainer,
        BrandAnimSpec,
        label = "brandSecondaryContainer",
    )
    val onSecondaryContainer by animateColorAsState(
        targets.onSecondaryContainer,
        BrandAnimSpec,
        label = "brandOnSecondaryContainer",
    )

    MaterialTheme(
        colorScheme = FcDarkColors.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
        ),
        content = content,
    )
}

private data class BrandSchemeTargets(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
)

private fun brandSchemeTargets(brand: TeamColors?): BrandSchemeTargets {
    if (brand == null) {
        return BrandSchemeTargets(
            primary = FcPrimary,
            onPrimary = FcOnPrimary,
            primaryContainer = FcPrimaryDark,
            onPrimaryContainer = Color.White,
            secondary = FcAccent,
            onSecondary = FcOnAccent,
            secondaryContainer = Color(0xFF4E2600),
            onSecondaryContainer = FcChipMoneyText,
        )
    }
    val primary = brand.primary
    val primaryContainer = darkenBrand(primary)
    val secondary = brand.secondary
    val secondaryContainer = darkenBrand(secondary, factor = 0.35f)
    return BrandSchemeTargets(
        primary = primary,
        onPrimary = onColorFor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = onColorFor(primaryContainer),
        secondary = secondary,
        onSecondary = onColorFor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onColorFor(secondaryContainer),
    )
}

/** Contrast-safe content color for a filled brand surface. */
fun onColorFor(background: Color): Color {
    return if (relativeLuminance(background) > 0.45) {
        Color(0xFF121212)
    } else {
        Color.White
    }
}

fun darkenBrand(color: Color, factor: Float = 0.45f): Color {
    return Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = 1f,
    )
}

private fun relativeLuminance(color: Color): Double {
    fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) +
        0.7152 * channel(color.green) +
        0.0722 * channel(color.blue)
}

fun ovrColor(ovr: Int): Color = when {
    ovr >= 85 -> FcOvrElite
    ovr >= 75 -> FcOvrStarter
    else -> FcOvrDepth
}

/** Accent for 0-100 attribute meters (elite gold → weak red). */
fun attrScoreColor(score: Int): Color = when {
    score >= 90 -> FcOvrElite
    score >= 80 -> FcOvrStarter
    score >= 70 -> Color(0xFFFFB74D)
    score >= 60 -> FcAccent
    else -> Color(0xFFEF5350)
}

/** Letter-grade accent for rating chips (A gold → F red). */
fun gradeColor(grade: String, primary: Color = FcPrimary): Color = when {
    grade.startsWith("A", ignoreCase = true) -> FcOvrElite
    grade.startsWith("B", ignoreCase = true) -> primary
    grade.startsWith("C", ignoreCase = true) -> Color(0xFFFFB74D)
    grade.startsWith("D", ignoreCase = true) -> FcAccent
    else -> Color(0xFFEF5350)
}

fun gradeColorBg(grade: String, primaryContainer: Color = Color(0xFF00332E)): Color = when {
    grade.startsWith("A", ignoreCase = true) -> Color(0xFF3E2E00)
    grade.startsWith("B", ignoreCase = true) -> primaryContainer
    grade.startsWith("C", ignoreCase = true) -> Color(0xFF4E2600)
    grade.startsWith("D", ignoreCase = true) -> Color(0xFF3E1400)
    else -> Color(0xFF4A1515)
}
