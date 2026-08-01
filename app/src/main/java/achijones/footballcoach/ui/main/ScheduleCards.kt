package achijones.footballcoach.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import achijones.footballcoach.ui.components.TeamLogo
import achijones.footballcoach.ui.components.rememberLogoNeedsContrastBoost
import achijones.footballcoach.ui.components.rememberTeamColors
import achijones.footballcoach.ui.theme.FcBye
import achijones.footballcoach.ui.theme.FcLoss
import achijones.footballcoach.ui.theme.FcWin

internal fun LazyListScope.scheduleItems(
    schedule: List<ScheduleRowUi>,
    onGameClick: (Int) -> Unit,
    onOpponentClick: (String?) -> Unit,
) {
    if (schedule.isEmpty()) {
        item(key = "schedule-empty") {
            Text(
                "No games scheduled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
        return
    }

    item(key = "schedule-hero") {
        ScheduleSnapshotHero(schedule)
    }

    items(
        items = schedule,
        key = { it.gameKey },
    ) { row ->
        ScheduleCard(row, onGameClick, onOpponentClick)
    }
}

@Composable
private fun ScheduleSnapshotHero(schedule: List<ScheduleRowUi>) {
    val playedRows = schedule.filter { it.played && !it.isBye }
    val wins = playedRows.count { it.isWin == true }
    val losses = playedRows.count { it.isLoss == true }
    val remaining = schedule.count { !it.played && !it.isBye }
    val nextOpponent = schedule.firstOrNull { !it.played && !it.isBye }?.opponentLabel ?: "Season done"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StatCardShape)
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), MaterialTheme.colorScheme.surfaceVariant),
                ),
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), StatCardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "SCHEDULE SNAPSHOT",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "Tap a game for details",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RosterMetricTile("RECORD", "$wins-$losses", Modifier.weight(1f))
            RosterMetricTile("PLAYED", playedRows.size.toString(), Modifier.weight(1f))
            RosterMetricTile("LEFT", remaining.toString(), Modifier.weight(1f))
            RosterMetricTile("NEXT", nextOpponent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ScheduleCard(
    row: ScheduleRowUi,
    onGameClick: (Int) -> Unit,
    onOpponentClick: (String?) -> Unit,
) {
    val cardShape = RoundedCornerShape(12.dp)
    if (row.isBye) {
        ByeWeekCard(row, cardShape)
        return
    }
    val opponentColors = rememberTeamColors(row.opponentTeamName, row.opponentAbbr)
    val opponentPrimary = if (row.opponentTeamName != null) opponentColors.primary else null
    val rowBrush = when {
        row.isWin == true -> Brush.horizontalGradient(
            listOf(
                FcWin.copy(alpha = 0.72f),
                FcWin.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        row.isLoss == true -> Brush.horizontalGradient(
            listOf(
                FcLoss.copy(alpha = 0.72f),
                FcLoss.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        opponentPrimary != null -> Brush.horizontalGradient(
            listOf(
                opponentPrimary.copy(alpha = 0.72f),
                opponentPrimary.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        else -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
    val borderColor = when {
        row.isWin == true -> FcWin.copy(alpha = 0.55f)
        row.isLoss == true -> FcLoss.copy(alpha = 0.55f)
        opponentPrimary != null -> opponentPrimary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }
    val logoBackground = when {
        row.isWin == true -> FcWin.copy(alpha = 0.72f)
        row.isLoss == true -> FcLoss.copy(alpha = 0.72f)
        opponentPrimary != null -> opponentPrimary.copy(alpha = 0.72f)
        else -> MaterialTheme.colorScheme.surface
    }
    val contrastBoost = rememberLogoNeedsContrastBoost(row.opponentTeamName, logoBackground)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(rowBrush)
            .border(1.dp, borderColor, cardShape)
            .clickable { onGameClick(row.gameKey) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = row.gameName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (row.played) row.scoreLine else "Upcoming",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = row.homeAway,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(72.dp)
                .clickable { onOpponentClick(row.opponentTeamName) },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TeamLogo(
                teamName = row.opponentTeamName,
                abbr = row.opponentAbbr,
                size = 44.dp,
                framed = false,
                contrastBoost = contrastBoost,
            )
            Text(
                text = "#${row.opponentRank}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ByeWeekCard(row: ScheduleRowUi, cardShape: RoundedCornerShape) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        FcBye.copy(alpha = 0.55f),
                        FcBye.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ),
            )
            .border(1.dp, FcBye.copy(alpha = 0.45f), cardShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = row.weekLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = if (row.played) "Bye week complete" else "Bye week",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "REST",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

