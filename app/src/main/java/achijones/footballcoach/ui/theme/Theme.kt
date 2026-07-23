package achijones.footballcoach.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun FcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FcDarkColors,
        content = content,
    )
}

fun ovrColor(ovr: Int): Color = when {
    ovr >= 85 -> FcOvrElite
    ovr >= 75 -> FcOvrStarter
    else -> FcOvrDepth
}

/** Letter-grade accent for rating chips (A gold → F red). */
fun gradeColor(grade: String): Color = when {
    grade.startsWith("A", ignoreCase = true) -> FcOvrElite
    grade.startsWith("B", ignoreCase = true) -> FcPrimary
    grade.startsWith("C", ignoreCase = true) -> Color(0xFFFFB74D)
    grade.startsWith("D", ignoreCase = true) -> FcAccent
    else -> Color(0xFFEF5350)
}

fun gradeColorBg(grade: String): Color = when {
    grade.startsWith("A", ignoreCase = true) -> Color(0xFF3E2E00)
    grade.startsWith("B", ignoreCase = true) -> Color(0xFF00332E)
    grade.startsWith("C", ignoreCase = true) -> Color(0xFF4E2600)
    grade.startsWith("D", ignoreCase = true) -> Color(0xFF3E1400)
    else -> Color(0xFF4A1515)
}
