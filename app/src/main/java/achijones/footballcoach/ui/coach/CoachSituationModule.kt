package achijones.footballcoach.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import CFBsimPack.engine.GameSituation
import CFBsimPack.engine.OffenseConcept
import CFBsimPack.engine.TempoCall

private val ScoreboardBg = Color(0xFF0D1117)
private val StripBg = Color(0xFF152018)
private val GhostBorder = Color(0xFF3A4A3C)
private val MutedText = Color(0xFF9CA3AF)
private val BallOrange = Color(0xFFF59E0B)
private val DetailChipBg = Color(0xFF1A241C)

/**
 * Sticky coach chrome: scoreboard, ESPN situation strip, clock chip, and field.
 * All labels come from [GameSituation]; tempo only supplies runoff preview.
 * When [selectedOffense] is set, the field shows stacked drive bars + next-play preview.
 */
@Composable
fun CoachSituationModule(
    sit: GameSituation,
    selectedTempo: TempoCall,
    showField: Boolean,
    modifier: Modifier = Modifier,
    selectedOffense: OffenseConcept? = null,
) {
    val driveSegments = remember(sit, selectedOffense) {
        if (selectedOffense != null) buildDriveSegments(sit, selectedOffense) else emptyList()
    }
    var selectedSegmentId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(sit.drivePlayCount, sit.playLog.size) {
        selectedSegmentId = null
    }
    val selectedSegment = driveSegments.firstOrNull {
        it.id == selectedSegmentId && it.kind == DriveSegmentKind.COMPLETED
    }

    Column(modifier.fillMaxWidth()) {
        CompactScoreboard(sit)
        EspnSituationStrip(sit)
        ClockChip(sit, selectedTempo)
        if (showField) {
            Spacer(Modifier.height(8.dp))
            CoachField(
                yardLine = sit.yardLine,
                firstDownYard = sit.firstDownYard,
                possessionHome = sit.possessionHome,
                homeDefendsLeft = sit.homeDefendsLeft,
                homeName = sit.homeName,
                awayName = sit.awayName,
                possessionAbbr = sit.possessionAbbr,
                driveSegments = driveSegments,
                selectedSegmentId = selectedSegmentId,
                onSegmentClick = if (selectedOffense != null) {
                    { id -> selectedSegmentId = id }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .padding(horizontal = 12.dp),
            )
            if (selectedSegment != null) {
                Spacer(Modifier.height(6.dp))
                DrivePlayDetailChip(
                    conceptName = selectedSegment.conceptName,
                    logLine = selectedSegment.logLine,
                    whyBullets = selectedSegment.whyBullets,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun DrivePlayDetailChip(
    conceptName: String,
    logLine: String,
    whyBullets: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DetailChipBg)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "PLAY",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(end = 10.dp),
            )
            Text(
                conceptName.ifBlank { "Play" },
                color = Color(0xFFE5E7EB),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (logLine.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                logLine,
                color = Color(0xFFD1D5DB),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (whyBullets.isNotEmpty()) {
            Spacer(modifier.height(6.dp))
            whyBullets.take(4).forEach { bullet ->
                Text(
                    "- $bullet",
                    color = MutedText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
@Composable
private fun CompactScoreboard(sit: GameSituation) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ScoreboardBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamScoreBlock(
                rank = sit.awayRank,
                name = sit.awayName,
                score = sit.awayScore,
                hasBall = !sit.possessionHome,
                modifier = Modifier.weight(1f),
                alignEnd = false,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 6.dp),
            ) {
                Text(
                    if (sit.playingOT) "OT" else "Q${sit.quarter}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    sit.clock,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            TeamScoreBlock(
                rank = sit.homeRank,
                name = sit.homeName,
                score = sit.homeScore,
                hasBall = sit.possessionHome,
                modifier = Modifier.weight(1f),
                alignEnd = true,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeoutPips(count = sit.awayTimeouts, max = sit.timeoutsMax, label = sit.awayAbbr)
            Text("TIMEOUTS", color = Color(0xFF6B7280), style = MaterialTheme.typography.labelSmall)
            TimeoutPips(count = sit.homeTimeouts, max = sit.timeoutsMax, label = sit.homeAbbr)
        }
    }
}

@Composable
private fun EspnSituationStrip(sit: GameSituation) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(StripBg)
            .border(1.dp, GhostBorder.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StripCell(
            caption = "DOWN",
            value = sit.downLabel,
            modifier = Modifier.weight(1.1f),
        )
        StripCell(
            caption = "BALL ON",
            value = sit.ballOnLabel,
            modifier = Modifier.weight(1f),
        )
        StripCell(
            caption = "DRIVE",
            value = sit.driveSummary,
            modifier = Modifier.weight(1.35f),
        )
    }
}

@Composable
private fun StripCell(
    caption: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            caption,
            color = MutedText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ClockChip(sit: GameSituation, selectedTempo: TempoCall) {
    val statusColor = when (sit.clockStatusLabel) {
        "RUNNING" -> BallOrange
        "10S RUNOFF" -> Color(0xFFEF4444)
        else -> MutedText
    }
    val runoffPreview = clockRunoffPreview(sit, selectedTempo)
    val line = buildString {
        append(if (sit.playingOT) "OT" else "Q${sit.quarter}")
        append(" ")
        append(sit.clock)
        append(" · ")
        append(sit.clockStatusLabel)
        if (runoffPreview != null) {
            append(" · snap −")
            append(selectedTempo.runoffSeconds())
            append("s → ")
            append(runoffPreview)
        }
        if (sit.crowdBand.isNotBlank()) {
            append(" · ")
            append(sit.crowdBand.uppercase())
        }
    }
    Text(
        line,
        color = statusColor,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .background(ScoreboardBg)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

/** Preview game clock after tempo runoff on the next snap, or null if not applicable. */
fun clockRunoffPreview(sit: GameSituation, tempo: TempoCall): String? {
    if (!sit.clockRunning || sit.playingOT || sit.pendingTenSecondRunoff) return null
    if (!sit.userOnOffense) return null
    val runoff = tempo.runoffSeconds()
    if (runoff <= 0) return null
    val rem = (sit.clockInQuarter - runoff).coerceAtLeast(0)
    return "${rem / 60}:${"%02d".format(rem % 60)}"
}

@Composable
private fun TeamScoreBlock(
    rank: Int,
    name: String,
    score: Int,
    hasBall: Boolean,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        ) {
            if (hasBall && !alignEnd) {
                PossessionDot()
                Spacer(Modifier.width(6.dp))
            }
            Text(
                if (rank in 1..25) "#$rank $name" else name,
                color = if (hasBall) Color.White else MutedText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hasBall && alignEnd) {
                Spacer(Modifier.width(6.dp))
                PossessionDot()
            }
        }
        Text(
            "$score",
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PossessionDot() {
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(BallOrange),
    )
}

@Composable
private fun TimeoutPips(count: Int, max: Int, label: String) {
    val slots = max.coerceIn(1, 3)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall)
        repeat(slots) { i ->
            Box(
                Modifier
                    .size(width = 14.dp, height = 6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < count) MaterialTheme.colorScheme.primary else Color(0xFF374151)),
            )
        }
    }
}
