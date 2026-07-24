package achijones.footballcoach.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import achijones.footballcoach.ui.components.SegmentedControl
import achijones.footballcoach.ui.theme.FcChipMoneyBg
import achijones.footballcoach.ui.theme.FcChipMoneyText
import achijones.footballcoach.ui.theme.FcChipPosBg
import achijones.footballcoach.ui.theme.FcChipPosText
import achijones.footballcoach.ui.theme.FcOvrElite
import achijones.footballcoach.ui.theme.FcPhasePortal
import achijones.footballcoach.ui.theme.FcPrimary
import achijones.footballcoach.ui.theme.gradeColor
import achijones.footballcoach.ui.theme.gradeColorBg
import achijones.footballcoach.ui.theme.ovrColor

private val CareerSegmentLabels = listOf("Season", "Career", "History")
private val CardShape = RoundedCornerShape(16.dp)
private val CellShape = RoundedCornerShape(10.dp)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerCareerSheet(
    career: PlayerCareerUi,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var segment by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            PlayerCareerHeader(career)
            Spacer(modifier = Modifier.height(14.dp))
            SegmentedControl(
                labels = CareerSegmentLabels,
                selected = segment,
                onSelect = { segment = it },
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Fixed pane so tab switches don't resize the sheet.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (segment) {
                        0 -> SeasonTab(career)
                        1 -> CareerTab(career)
                        else -> TimelineTab(career.timeline)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerCareerHeader(career: PlayerCareerUi) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = career.position,
                color = FcChipPosText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(FcChipPosBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = career.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = career.teamName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ovrColor(career.ovr))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = career.ovr.toString(),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetaChip(career.yearLabel)
            MetaChip("Pot ${career.potGrade}")
            MetaChip(career.rosterStatus)
            MetaChip(career.teamAbbr)
            if (career.nilLabel != null) {
                MoneyChip(career.nilLabel)
            }
            for (pos in career.secondaryPosOvrs) {
                MetaChip(pos)
            }
        }
    }
}

@Composable
private fun SeasonTab(career: PlayerCareerUi) {
    if (career.attrChips.isEmpty()
        && career.seasonRatings.isEmpty()
        && career.seasonStats.isEmpty()
    ) {
        EmptyPane("No season stats yet.")
        return
    }
    if (career.attrChips.isNotEmpty()) {
        SectionLabel("Attributes")
        RatingsGrid(career.attrChips)
    }
    if (career.seasonRatings.isNotEmpty()) {
        SectionLabel("Ratings")
        RatingsGrid(career.seasonRatings)
    }
    if (career.seasonStats.isNotEmpty()) {
        SectionLabel("Season Card")
        FootballStatCard(career.seasonStats)
    }
}

@Composable
private fun CareerTab(career: PlayerCareerUi) {
    if (career.careerTotals.isEmpty() && career.seasonYears.isEmpty()) {
        EmptyPane("No career history yet.")
        return
    }
    if (career.careerTotals.isNotEmpty()) {
        SectionLabel("Career Totals")
        CareerTotalsTable(career.careerTotals)
    }
    if (career.seasonYears.isNotEmpty()) {
        SectionLabel("Year by Year")
        YearByYearTable(career.seasonYears)
    }
}

@Composable
private fun TimelineTab(events: List<TimelineEventUi>) {
    if (events.isEmpty()) {
        EmptyPane("No timeline events yet.")
        return
    }
    SectionLabel("Timeline")
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        events.forEachIndexed { index, event ->
            TimelineRow(
                event = event,
                isLast = index == events.lastIndex,
            )
        }
    }
}

@Composable
private fun RatingsGrid(ratings: List<StatChipUi>) {
    val columns = 2
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ratings.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { chip ->
                    RatingCell(
                        chip = chip,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RatingCell(chip: StatChipUi, modifier: Modifier = Modifier) {
    val accent = gradeColor(chip.value)
    val bg = gradeColorBg(chip.value)
    Column(
        modifier = modifier
            .clip(CellShape)
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.45f), CellShape)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = chip.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = chip.value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FootballStatCard(stats: List<StatChipUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, FcPrimary.copy(alpha = 0.35f), CardShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(FcPrimary),
        )
        val columns = 3
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            stats.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { chip ->
                        StatCardCell(
                            chip = chip,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCardCell(chip: StatChipUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(CellShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = chip.value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = chip.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CareerTotalsTable(stats: List<StatChipUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, FcPrimary.copy(alpha = 0.35f), CardShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(FcPrimary),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "STAT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "TOTAL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        stats.forEachIndexed { index, chip ->
            val stripe = index % 2 == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (stripe) Color.Transparent
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chip.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = chip.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun YearByYearTable(years: List<SeasonYearUi>) {
    val statColumns = remember(years) {
        years.flatMap { y -> y.stats.map { it.label } }.distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, FcPrimary.copy(alpha = 0.35f), CardShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(FcPrimary),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
        ) {
            Column {
                YearTableHeaderRow(statColumns)
                years.forEachIndexed { index, year ->
                    YearTableDataRow(
                        year = year,
                        statColumns = statColumns,
                        striped = index % 2 == 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun YearTableHeaderRow(statColumns: List<String>) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableHeaderCell("YEAR", width = 52.dp)
        TableHeaderCell("TM", width = 48.dp)
        TableHeaderCell("CL", width = 36.dp)
        TableHeaderCell("G", width = 72.dp)
        statColumns.forEach { label ->
            TableHeaderCell(shortStatHeader(label), width = 56.dp)
        }
    }
}

@Composable
private fun YearTableDataRow(
    year: SeasonYearUi,
    statColumns: List<String>,
    striped: Boolean,
) {
    val valuesByLabel = remember(year) { year.stats.associate { it.label to it.value } }
    Row(
        modifier = Modifier
            .background(
                if (striped) MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                else Color.Transparent,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableValueCell(
            text = buildString {
                append(year.year)
                if (year.isCurrent) append("*")
            },
            width = 52.dp,
            emphasize = true,
        )
        TableValueCell(year.teamAbbr, width = 48.dp, emphasize = true)
        TableValueCell(year.classLabel, width = 36.dp)
        TableValueCell(compactRecord(year.recordLine), width = 72.dp)
        statColumns.forEach { label ->
            TableValueCell(valuesByLabel[label] ?: "—", width = 56.dp)
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, width: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width),
    )
}

@Composable
private fun TableValueCell(
    text: String,
    width: Dp,
    emphasize: Boolean = false,
) {
    Text(
        text = text,
        style = if (emphasize) {
            MaterialTheme.typography.bodyMedium
        } else {
            MaterialTheme.typography.bodySmall
        },
        fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width),
    )
}

private fun shortStatHeader(label: String): String = when (label) {
    "Pass Yds", "Rush Yds", "Yards" -> "YDS"
    "Comp" -> "CMP"
    "Sacks" -> "SK"
    "Att" -> "ATT"
    "Fum" -> "FUM"
    "Tgts" -> "TGT"
    "Drops" -> "DRP"
    "Rec" -> "REC"
    else -> label.uppercase().take(4)
}

private fun compactRecord(recordLine: String): String {
    // "12G (8-4)" -> "8-4"
    val open = recordLine.indexOf('(')
    val close = recordLine.indexOf(')')
    return if (open >= 0 && close > open) {
        recordLine.substring(open + 1, close)
    } else {
        recordLine
    }
}

@Composable
private fun TimelineRow(
    event: TimelineEventUi,
    isLast: Boolean,
) {
    val accent = timelineAccent(event.kind)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(28.dp)
                .padding(top = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = if (isLast) 0.dp else 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.yearLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (event.amountLabel != null) {
                    Text(
                        text = event.amountLabel,
                        color = FcChipMoneyText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(FcChipMoneyBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            if (!event.detail.isNullOrBlank()) {
                Text(
                    text = event.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun timelineAccent(kind: TimelineKind): Color = when (kind) {
    TimelineKind.DEAL -> FcChipMoneyText
    TimelineKind.TRANSFER -> FcPhasePortal
    TimelineKind.SCHOOL -> FcPrimary
    TimelineKind.AWARD -> FcOvrElite
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyPane(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MetaChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun MoneyChip(label: String) {
    Text(
        text = label,
        color = FcChipMoneyText,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FcChipMoneyBg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
