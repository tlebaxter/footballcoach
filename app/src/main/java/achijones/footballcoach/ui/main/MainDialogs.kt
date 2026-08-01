package achijones.footballcoach.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import achijones.footballcoach.ui.components.TeamLogo
import achijones.footballcoach.ui.components.rememberSheetFlingBlocker
import achijones.footballcoach.ui.theme.FcChipPosBg
import achijones.footballcoach.ui.theme.FcChipPosText
import achijones.footballcoach.ui.theme.FcOvrElite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InjuryReportSheet(report: InjuryReportUi, viewModel: MainViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissInjuryDialog,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = "Injury Report",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val subtitle = listOfNotNull(
                report.injured.size.takeIf { it > 0 }?.let { "$it injured" },
                report.recovered.size.takeIf { it > 0 }?.let { "$it recovered" },
            ).joinToString(" \u00B7 ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .nestedScroll(rememberSheetFlingBlocker())
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (report.injured.isEmpty() && report.recovered.isEmpty()) {
                    Text(
                        text = "No injuries this week.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (report.injured.isNotEmpty()) {
                    InjuryReportSectionLabel("INJURED")
                    report.injured.forEach { InjuryReportCard(it) }
                }
                if (report.recovered.isNotEmpty()) {
                    if (report.injured.isNotEmpty()) Spacer(modifier = Modifier.height(4.dp))
                    InjuryReportSectionLabel("RECOVERED")
                    report.recovered.forEach { InjuryReportCard(it) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = report.showReportsEnabled,
                    onCheckedChange = viewModel::setInjuryReportEnabled,
                )
                Text(
                    text = "Show injury reports",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = viewModel::goToLineupFromInjury,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Set Lineup")
                }
                Button(
                    onClick = viewModel::dismissInjuryDialog,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
private fun InjuryReportSectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InjuryReportCard(player: InjuryReportPlayerUi) {
    val cardShape = RoundedCornerShape(12.dp)
    val injured = player.injuryLabel != null
    val borderColor = if (injured) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }
    val surfaceColor = if (injured) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(surfaceColor)
            .border(1.dp, borderColor, cardShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = player.pos,
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
                text = player.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RosterMetaChip(player.yearLabel)
                if (player.injuryLabel != null) {
                    RosterInjuryChip(player.injuryLabel)
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        OvrPotBadge(ovr = player.ovr, potGrade = player.potGrade)
    }
}

@Composable
fun PlayersLeavingDialog(dialog: PlayersLeavingDialogUi, viewModel: MainViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismissPlayersLeavingDialog,
        title = { Text(dialog.title) },
        text = {
            Column {
                SegmentRow(
                    labels = listOf("Players Leaving", "Mock Draft"),
                    selected = dialog.tab.ordinal,
                    onSelect = {
                        viewModel.setPlayersLeavingTab(PlayersLeavingTab.entries[it])
                    },
                )
                val lines = if (dialog.tab == PlayersLeavingTab.GRADUATES) {
                    dialog.gradLines
                } else {
                    dialog.mockDraftLines
                }
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(lines) { Text(it, modifier = Modifier.padding(vertical = 2.dp)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::confirmBeginRetention) { Text("Begin Retention") }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissPlayersLeavingDialog) { Text("Cancel") }
        },
    )
}

@Composable
fun RecruitingClassDialog(rows: List<RankingRowUi>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Recruiting Class Rankings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows) { row ->
                    RankingRowCard(row = row, onClick = null)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
fun SaveDialog(state: MainUiState, viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = viewModel::dismissSaveDialog,
        title = { Text("Choose Save File:") },
        text = {
            LazyColumn {
                items(state.saveSlotInfos.size) { i ->
                    val info = state.saveSlotInfos[i]
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { viewModel.saveToSlot(i) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(achijones.footballcoach.ui.util.SaveSlots.label(info))
                        }
                        if (info.status == achijones.footballcoach.save.SlotStatus.OK) {
                            TextButton(onClick = {
                                viewModel.exportActiveOrSlot(i) { json ->
                                    achijones.footballcoach.ui.util.SaveExportShare.shareJson(context, i, json)
                                }
                            }) { Text("Export") }
                        }
                        if (info.status != achijones.footballcoach.save.SlotStatus.EMPTY) {
                            TextButton(onClick = { viewModel.requestDeleteSlot(i) }) {
                                Text("Del")
                            }
                        }
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissSaveDialog) { Text("Cancel") } },
        confirmButton = {},
    )
}

@Composable
fun RankingsDialog(state: MainUiState, viewModel: MainViewModel) {
    val modeLabels = arrayOf(
        "Poll Votes", "Conference Standings", "Strength of Sched", "Points Per Game",
        "Opp Points Per Game", "Yards Per Game", "Opp Yards Per Game", "Pass Yards Per Game",
        "Rush Yards Per Game", "Opp Pass YPG", "Opp Rush YPG", "TO Differential",
        "Off Talent", "Def Talent", "Program Power", "Recruiting Class",
    )
    AlertDialog(
        onDismissRequest = viewModel::dismissRankingsDialog,
        title = {
            Text(
                "Team Rankings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SpinnerDropdown(
                    label = "View",
                    options = modeLabels.toList(),
                    selectedIndex = state.rankingsModeIndex,
                    onSelect = viewModel::setRankingsMode,
                )
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = state.rankingsRows,
                        key = { row ->
                            if (row.isSectionHeader) {
                                "hdr-${row.sectionTitle}"
                            } else {
                                "${row.rankLabel}-${row.abbr}-${row.statValue}-${row.line}"
                            }
                        },
                    ) { row ->
                        RankingRowCard(
                            row = row,
                            onClick = if (row.teamName != null) {
                                { viewModel.examineTeamFromRankingRow(row) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::dismissRankingsDialog) { Text("OK") }
        },
    )
}

@Composable
private fun RankingRowCard(
    row: RankingRowUi,
    onClick: (() -> Unit)?,
) {
    if (row.isSectionHeader) {
        Text(
            text = row.sectionTitle.orEmpty().uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
        )
        return
    }

    val cardShape = RoundedCornerShape(12.dp)
    val brandPrimary = MaterialTheme.colorScheme.primary
    val accents = rankAccent(row.rankNum, brandPrimary)
    val bg = if (row.isUserTeam) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val border = if (row.isUserTeam) {
        brandPrimary.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(bg)
            .border(1.dp, border, cardShape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accents.bg)
                .border(1.dp, accents.border, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = row.rankLabel.ifBlank { row.rankNum.toString() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accents.fg,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
        if (!row.teamName.isNullOrBlank() && !row.abbr.isNullOrBlank()) {
            TeamLogo(
                teamName = row.teamName,
                abbr = row.abbr,
                size = 36.dp,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = row.abbr ?: row.teamName.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = buildList {
                row.pollRank?.let { add("#$it") }
                row.record?.let { add("($it)") }
            }.joinToString(" ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = row.statValue,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
fun LeagueHistoryDialog(state: MainUiState, viewModel: MainViewModel) {
    HistoryListDialog(
        title = "League History / Records",
        modeLabels = arrayOf("League History", "League Records"),
        modeIndex = state.leagueHistoryModeIndex,
        onModeChange = viewModel::setLeagueHistoryMode,
        rows = state.leagueHistoryRows,
        onDismiss = viewModel::dismissLeagueHistoryDialog,
    )
}

@Composable
fun TeamHistoryDialog(state: MainUiState, viewModel: MainViewModel) {
    HistoryListDialog(
        title = "Team History",
        modeLabels = arrayOf("Team History", "Team Records", "Hall of Fame"),
        modeIndex = state.teamHistoryModeIndex,
        onModeChange = viewModel::setTeamHistoryMode,
        rows = state.teamHistoryRows,
        onDismiss = viewModel::dismissTeamHistoryDialog,
    )
}

@Composable
private fun HistoryListDialog(
    title: String,
    modeLabels: Array<String>,
    modeIndex: Int,
    onModeChange: (Int) -> Unit,
    rows: List<HistoryRowUi>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SpinnerDropdown(
                    label = "View",
                    options = modeLabels.toList(),
                    selectedIndex = modeIndex,
                    onSelect = onModeChange,
                )
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(rows) { row ->
                        HistoryRowCard(row)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
private fun HistoryRowCard(row: HistoryRowUi) {
    when (row.kind) {
        HistoryRowKind.SECTION -> {
            Text(
                text = row.title.orEmpty().uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
            )
        }

        HistoryRowKind.SUMMARY_STAT -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.title.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = row.value.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        HistoryRowKind.RECORD -> {
            val cardShape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(
                        if (row.isUserRelated) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .border(
                        1.dp,
                        if (row.isUserRelated) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        },
                        cardShape,
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!row.teamName.isNullOrBlank() && !row.abbr.isNullOrBlank()) {
                    TeamLogo(teamName = row.teamName, abbr = row.abbr, size = 36.dp)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = row.title.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(row.holder, row.yearLabel).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = row.value.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        HistoryRowKind.YEAR -> {
            val cardShape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(
                        if (row.isUserRelated) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                        cardShape,
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!row.teamName.isNullOrBlank() && !row.abbr.isNullOrBlank()) {
                    TeamLogo(teamName = row.teamName, abbr = row.abbr, size = 40.dp)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = row.title.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    row.value?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    row.holder?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        HistoryRowKind.HOF -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = row.title.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (row.text.isNotBlank()) {
                    Text(
                        text = row.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        HistoryRowKind.TEXT -> {
            Text(
                text = row.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleListDialog(
    title: String,
    modeLabels: Array<String>,
    modeIndex: Int,
    onModeChange: (Int) -> Unit,
    lines: List<String>,
    onLineClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                SpinnerDropdown(
                    label = "View",
                    options = modeLabels.toList(),
                    selectedIndex = modeIndex,
                    onSelect = onModeChange,
                )
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(lines) { line ->
                        Text(
                            line,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLineClick(line) }
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
fun RenameDialog(dialog: RenameDialogUi, viewModel: MainViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::cancelRename,
        title = { Text("Settings / Change Name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dialog.name,
                    onValueChange = viewModel::updateRenameName,
                    label = { Text("Team Name") },
                    isError = dialog.nameError.isNotEmpty(),
                    supportingText = { if (dialog.nameError.isNotEmpty()) Text(dialog.nameError) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dialog.abbr,
                    onValueChange = viewModel::updateRenameAbbr,
                    label = { Text("Abbreviation") },
                    isError = dialog.abbrError.isNotEmpty(),
                    supportingText = { if (dialog.abbrError.isNotEmpty()) Text(dialog.abbrError) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(dialog.showPopups, viewModel::updateRenameShowPopups)
                    Text("Show popups")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(dialog.showInjuryReport, viewModel::updateRenameShowInjury)
                    Text("Show injury report")
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::confirmRename) { Text("OK") } },
        dismissButton = { TextButton(onClick = viewModel::cancelRename) { Text("Cancel") } },
    )
}
