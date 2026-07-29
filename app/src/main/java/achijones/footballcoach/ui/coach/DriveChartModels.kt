package achijones.footballcoach.ui.coach

import CFBsimPack.engine.ConceptFamily
import CFBsimPack.engine.DepthBand
import CFBsimPack.engine.GameSituation
import CFBsimPack.engine.OffenseConcept
import CFBsimPack.engine.PlayLogEntry
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

enum class DriveSegmentKind {
    COMPLETED,
    PREVIEW,
}

data class DriveSegment(
    /** Index within the current-drive play list; [PREVIEW_ID] for preview. */
    val id: Int,
    val startYard: Int,
    val endYard: Int,
    val kind: DriveSegmentKind,
    val family: ConceptFamily?,
    val yardsGained: Int,
    val logLine: String,
    val conceptName: String,
    /** Coach why bullets from snap trace; empty for preview / specials. */
    val whyBullets: List<String> = emptyList(),
)

const val PREVIEW_SEGMENT_ID = -1

/** Minimum absolute field fraction span for zero-yard / very short segments (~1.5 yd). */
const val MIN_SEGMENT_FRACTION = 0.015f

private val ColorRun = Color(0xFF22C55E)
private val ColorPass = Color(0xFF3B82F6)
private val ColorLoss = Color(0xFFEF4444)
private val ColorSpecial = Color(0xFF9CA3AF)

fun previewDepthYards(depth: DepthBand): Int = when (depth) {
    DepthBand.SHORT -> 6
    DepthBand.MEDIUM -> 12
    DepthBand.DEEP -> 22
    DepthBand.NONE -> 4
}

fun shouldShowDrivePreview(sit: GameSituation): Boolean {
    if (sit.gameOver || sit.awaitingCoinToss) return false
    if (sit.pendingKickoff || sit.freeKick || sit.specialTeamsDown) return false
    if (sit.pendingTry || sit.tryAwaitingChoice) return false
    if (!sit.userOnOffense) return false
    return true
}

fun buildDriveSegments(
    sit: GameSituation,
    selectedOffense: OffenseConcept,
): List<DriveSegment> {
    val count = sit.drivePlayCount.coerceAtLeast(0)
    val log = sit.playLog
    val drivePlays: List<PlayLogEntry> = if (count <= 0 || log.isEmpty()) {
        emptyList()
    } else {
        val from = (log.size - count).coerceAtLeast(0)
        log.subList(from, log.size)
    }
    return buildDriveSegments(
        drivePlays = drivePlays,
        yardLine = sit.yardLine,
        selectedOffense = selectedOffense,
        showPreview = shouldShowDrivePreview(sit),
    )
}

fun buildDriveSegments(
    drivePlays: List<PlayLogEntry>,
    yardLine: Int,
    selectedOffense: OffenseConcept,
    showPreview: Boolean,
): List<DriveSegment> {
    val segments = ArrayList<DriveSegment>(drivePlays.size + 1)
    drivePlays.forEachIndexed { index, entry ->
        segments.add(
            DriveSegment(
                id = index,
                startYard = entry.yardLineBefore.coerceIn(0, 100),
                endYard = entry.yardLineAfter.coerceIn(0, 100),
                kind = DriveSegmentKind.COMPLETED,
                family = entry.offenseFamily,
                yardsGained = entry.yardsGained,
                logLine = entry.logLine,
                conceptName = entry.offenseConceptName,
                whyBullets = entry.snapTrace?.summaryBullets?.toList().orEmpty(),
            ),
        )
    }

    if (showPreview) {
        val start = yardLine.coerceIn(0, 100)
        val end = (start + previewDepthYards(selectedOffense.depth)).coerceIn(0, 100)
        segments.add(
            DriveSegment(
                id = PREVIEW_SEGMENT_ID,
                startYard = start,
                endYard = end,
                kind = DriveSegmentKind.PREVIEW,
                family = selectedOffense.family,
                yardsGained = end - start,
                logLine = "",
                conceptName = selectedOffense.displayName,
            ),
        )
    }
    return segments
}

/**
 * Color for a segment. Negative yards override family; preview uses family color
 * (caller applies alpha / dashed stroke).
 */
fun driveSegmentColor(segment: DriveSegment): Color {
    if (segment.kind == DriveSegmentKind.COMPLETED && segment.yardsGained < 0) {
        return ColorLoss
    }
    return when (segment.family) {
        ConceptFamily.RUN -> ColorRun
        ConceptFamily.PASS, ConceptFamily.RPO -> ColorPass
        ConceptFamily.SPECIAL, null -> ColorSpecial
    }
}

/**
 * Absolute field fractions (0–1 playable) for drawing/hit-testing, with a minimum
 * span so zero-yard plays remain visible and tappable.
 */
fun segmentAbsoluteFractions(
    segment: DriveSegment,
    possessionHome: Boolean,
    homeDefendsLeft: Boolean,
): Pair<Float, Float> {
    var f0 = offenseYardToAbsolute(segment.startYard, possessionHome, homeDefendsLeft)
    var f1 = offenseYardToAbsolute(segment.endYard, possessionHome, homeDefendsLeft)
    if (abs(f1 - f0) < MIN_SEGMENT_FRACTION) {
        val mid = (f0 + f1) / 2f
        f0 = (mid - MIN_SEGMENT_FRACTION / 2f).coerceIn(0f, 1f)
        f1 = (mid + MIN_SEGMENT_FRACTION / 2f).coerceIn(0f, 1f)
        if (abs(f1 - f0) < MIN_SEGMENT_FRACTION) {
            // Near end zone edge — bias inward
            if (f0 <= 0f) {
                f0 = 0f
                f1 = MIN_SEGMENT_FRACTION
            } else {
                f1 = 1f
                f0 = 1f - MIN_SEGMENT_FRACTION
            }
        }
    }
    return if (f0 <= f1) f0 to f1 else f1 to f0
}

fun hitTestDriveSegment(
    segments: List<DriveSegment>,
    xFraction: Float,
    possessionHome: Boolean,
    homeDefendsLeft: Boolean,
): DriveSegment? {
    // Prefer later (more recent) completed plays when overlapping; ignore preview for taps.
    for (i in segments.indices.reversed()) {
        val seg = segments[i]
        if (seg.kind != DriveSegmentKind.COMPLETED) continue
        val (left, right) = segmentAbsoluteFractions(seg, possessionHome, homeDefendsLeft)
        if (xFraction in left..right) return seg
    }
    return null
}
