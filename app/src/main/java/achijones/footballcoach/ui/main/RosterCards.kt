package achijones.footballcoach.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import achijones.footballcoach.ui.theme.FcChipPosBg
import achijones.footballcoach.ui.theme.FcChipPosText
import achijones.footballcoach.ui.theme.FcOvrElite
import achijones.footballcoach.ui.theme.FcOvrStarter

internal val StatCardShape = RoundedCornerShape(14.dp)
private val StatTileShape = RoundedCornerShape(12.dp)
private val STAT_SECTION_ORDER = listOf("Offense", "Defense", "Program")
private val STAT_SPOTLIGHT_LABELS = listOf("Points", "Opp Points", "Yards", "TO Diff")

internal fun LazyListScope.rosterItems(
    roster: List<RosterRowUi>,
    onPlayerClick: (Int) -> Unit,
) {
    if (roster.isEmpty()) {
        item(key = "roster-empty") {
            Text(
                "No players on roster.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
        return
    }

    item(key = "roster-hero") {
        RosterSummaryHero(roster)
    }

    var index = 0
    while (index < roster.size) {
        val pos = roster[index].pos
        val sectionStart = index
        while (index < roster.size && roster[index].pos == pos) {
            index++
        }
        val sectionPlayers = roster.slice(sectionStart until index)
        item(key = "roster-section-$pos") {
            Text(
                pos,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            )
        }
        items(
            items = sectionPlayers,
            key = { it.playerKey },
        ) { row ->
            RosterCard(row) { onPlayerClick(row.playerKey) }
        }
    }
}

internal fun LazyListScope.teamStatsItems(
    stats: List<StatRowUi>,
    onStatClick: (Int) -> Unit,
) {
    if (stats.isEmpty()) {
        item {
            Text(
                "No team stats yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
        return
    }

    val byLabel = stats.associateBy { it.label }
    val spotlight = STAT_SPOTLIGHT_LABELS.mapNotNull { byLabel[it] }
    val standing = listOfNotNull(byLabel["Conf W-L"], byLabel["AP Votes"], byLabel["SOS"])

    item(key = "stats-hero") {
        TeamStatsHero(
            standing = standing,
            spotlight = spotlight,
            onStatClick = onStatClick,
        )
    }

    STAT_SECTION_ORDER.forEach { section ->
        val rows = stats.filter { it.category == section }
        if (rows.isEmpty()) return@forEach
        item(key = "section-$section") {
            Text(
                section.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            )
        }
        items(
            items = rows.chunked(2),
            key = { pair -> "row-${section}-${pair.joinToString("-") { it.label }}" },
        ) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { row ->
                    StatMetricTile(
                        row = row,
                        onClick = row.rankingsMode?.let { mode -> { onStatClick(mode) } },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TeamStatsHero(
    standing: List<StatRowUi>,
    spotlight: List<StatRowUi>,
    onStatClick: (Int) -> Unit,
) {
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
                "SEASON SNAPSHOT",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "Tap a stat for rankings",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        if (standing.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                standing.forEach { row ->
                    StatMetricTile(
                        row = row,
                        onClick = row.rankingsMode?.let { mode -> { onStatClick(mode) } },
                        modifier = Modifier.weight(1f),
                        compact = true,
                        emphasize = true,
                    )
                }
            }
        }
        if (spotlight.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                spotlight.forEach { row ->
                    StatMetricTile(
                        row = row,
                        onClick = row.rankingsMode?.let { mode -> { onStatClick(mode) } },
                        modifier = Modifier.weight(1f),
                        compact = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricTile(
    row: StatRowUi,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    emphasize: Boolean = false,
) {
    val rankColors = rankAccent(row.rankNum, MaterialTheme.colorScheme.primary)
    Column(
        modifier = modifier
            .clip(StatTileShape)
            .background(
                if (emphasize) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .border(1.dp, rankColors.border, StatTileShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 10.dp else 12.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = displayStatLabel(row.label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = row.value,
            style = if (compact) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.headlineSmall
            },
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = row.rank,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(rankColors.bg)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = rankColors.fg,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

internal data class RankAccent(val bg: Color, val fg: Color, val border: Color)

internal fun rankAccent(rankNum: Int, brandPrimary: Color): RankAccent = when {
    rankNum <= 3 -> RankAccent(
        bg = Color(0xFF3E2E00),
        fg = FcOvrElite,
        border = FcOvrElite.copy(alpha = 0.55f),
    )
    rankNum <= 10 -> RankAccent(
        bg = Color(0xFF00332E),
        fg = brandPrimary,
        border = brandPrimary.copy(alpha = 0.45f),
    )
    rankNum <= 25 -> RankAccent(
        bg = Color(0xFF1A2A3A),
        fg = Color(0xFF90CAF9),
        border = Color(0xFF90CAF9).copy(alpha = 0.35f),
    )
    else -> RankAccent(
        bg = Color(0xFF2A2A2A),
        fg = Color(0xFFB0B0B0),
        border = Color(0xFF3A3A3A),
    )
}

private fun displayStatLabel(label: String): String = when (label) {
    "Conf W-L" -> "CONF"
    "AP Votes" -> "POLL"
    "Opp Points" -> "OPP PPG"
    "Opp Yards" -> "OPP YPG"
    "Pass Yards" -> "PASS YPG"
    "Rush Yards" -> "RUSH YPG"
    "Points" -> "PPG"
    "Yards" -> "YPG"
    "Off Talent" -> "OFF TAL"
    "Def Talent" -> "DEF TAL"
    "Recruit Class" -> "RECRUIT"
    else -> label.uppercase()
}

@Composable
private fun RosterSummaryHero(roster: List<RosterRowUi>) {
    val starterCount = roster.count { it.starter }
    val injuredCount = roster.count { it.injured }
    val avgOvr = if (roster.isEmpty()) 0 else roster.sumOf { it.ovr } / roster.size
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
                "ROSTER SNAPSHOT",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "Tap a player for details",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RosterMetricTile("PLAYERS", roster.size.toString(), Modifier.weight(1f))
            RosterMetricTile("STARTERS", starterCount.toString(), Modifier.weight(1f))
            RosterMetricTile("AVG OVR", avgOvr.toString(), Modifier.weight(1f))
            RosterMetricTile("INJURED", injuredCount.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
internal fun RosterMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(StatTileShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RosterCard(row: RosterRowUi, onClick: () -> Unit) {
    val cardShape = RoundedCornerShape(12.dp)
    val surfaceAlpha = if (row.starter) 1f else 0.72f
    val borderColor = if (row.starter) {
        FcOvrStarter.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = surfaceAlpha))
            .border(1.dp, borderColor, cardShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.pos,
            color = FcChipPosText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(FcChipPosBg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = row.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RosterMetaChip(row.yearLabel)
                if (row.injured && row.injuryLabel != null) {
                    RosterInjuryChip(row.injuryLabel)
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        OvrPotBadge(ovr = row.ovr, potGrade = row.potGrade)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (row.starter) "Starter" else "Bench",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (row.starter) FcOvrStarter else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (row.starter) FcOvrStarter.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun RosterMetaChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
internal fun RosterInjuryChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onError,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
