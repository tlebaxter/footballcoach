package achijones.footballcoach.ui.talenthub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import CFBsimPack.RosterStatus
import achijones.footballcoach.ui.components.SegmentedControl
import achijones.footballcoach.ui.components.TabContentTransition
import achijones.footballcoach.ui.theme.FcChipMoneyBg
import achijones.footballcoach.ui.theme.FcChipMoneyText
import achijones.footballcoach.ui.theme.FcChipPosBg
import achijones.footballcoach.ui.theme.FcChipPosText
import achijones.footballcoach.ui.theme.ovrColor

private val POSITIONS = listOf(
    "ALL", "QB", "RB", "FB", "WR", "TE", "OL", "K", "S", "CB", "EDGE", "DL", "LB",
)
private val SORTS = listOf("OVR ↓", "Cost ↑", "Name")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TalentHubScreen(
    onNavigateToMain: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: TalentHubViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    BackHandler { viewModel.requestLeave() }

    LaunchedEffect(state.navigateToMain) {
        if (state.navigateToMain) {
            viewModel.consumeNavigateToMain()
            onNavigateToMain()
        }
    }
    LaunchedEffect(state.navigateHome) {
        if (state.navigateHome) {
            viewModel.consumeNavigateHome()
            onNavigateHome()
        }
    }
    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        viewModel.clearMessage()
    }
    LaunchedEffect(state.missingSession) {
        if (state.missingSession) {
            snackbar.showSnackbar("Offseason session missing.")
            onNavigateHome()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Talent Hub") },
                actions = {
                    IconButton(onClick = viewModel::openSaveDialog) {
                        Icon(Icons.Default.Save, contentDescription = "Save League")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            state.primaryLabel?.let { label ->
                Button(
                    onClick = viewModel::onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) { Text(label) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = "${state.teamName} — ${state.phaseLabel}" +
                    if (state.browsing) " (browsing)" else "",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BudgetChip(state.cashLabel)
                BudgetChip(state.y1Label)
                BudgetChip(state.schollyLabel)
                BudgetChip(state.rosterLabel)
            }
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                labels = listOf("Retain", "Portal", "Schedule", "HS", "Money"),
                selected = state.selectedTab.ordinal,
                onSelect = { viewModel.selectTab(HubTab.entries[it]) },
                modifier = Modifier.padding(vertical = 4.dp),
            )
            TabContentTransition(
                targetState = state.selectedTab,
                modifier = Modifier.fillMaxSize(),
                label = "talentHubTabContent",
            ) { tab ->
                if (tab == HubTab.SCHEDULE) {
                    ScheduleTabContent(state, viewModel)
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (tab != HubTab.MONEY) {
                            FiltersRow(state, viewModel)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.rows, key = { it.id }) { row ->
                                TalentRowCard(
                                    row = row,
                                    onClick = { viewModel.onRowTap(row.id) },
                                    onCheck = { viewModel.toggleSuggestion(row.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.opponentPickerWeek != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissOpponentPicker,
            title = { Text("OOC opponent — Week ${(state.opponentPickerWeek ?: 0) + 1}") },
            text = {
                Column {
                    if (state.dealOpponentAbbr != null) {
                        Text(
                            state.dealQuote,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { viewModel.signBuyGameYears(1) }) {
                                Text("Buy 1yr")
                            }
                            TextButton(onClick = { viewModel.signBuyGameYears(2) }) {
                                Text("Buy 2yr")
                            }
                            TextButton(onClick = { viewModel.signBuyGameYears(3) }) {
                                Text("Buy 3yr")
                            }
                        }
                        TextButton(onClick = viewModel::signHomeAndHomeDeal) {
                            Text("Home-and-home (2yr)")
                        }
                        TextButton(
                            onClick = {
                                val idx = state.opponentAbbrs.indexOf(state.dealOpponentAbbr)
                                if (idx >= 0) viewModel.pickOpponent(idx)
                            },
                        ) {
                            Text("One-off game only")
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(state.opponentOptions.size) { index ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = {
                                    viewModel.selectDealOpponent(index)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    state.opponentOptions[index],
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start,
                                )
                            }
                            val abbr = state.opponentAbbrs.getOrNull(index)
                            if (abbr != null) {
                                TextButton(
                                    onClick = { viewModel.declareRival(abbr) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Declare $abbr as rival")
                                }
                            }
                        }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissOpponentPicker) { Text("Close") }
            },
        )
    }

    state.offerSheet?.let { sheet ->
        OfferBottomSheet(
            sheet = sheet,
            onDismiss = viewModel::dismissOfferSheet,
            onStatus = viewModel::updateOfferStatus,
            onYears = viewModel::updateOfferYears,
            onConfirm = viewModel::confirmOffer,
            onBuyout = viewModel::requestBuyout,
        )
    }

    if (state.showSaveDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSaveDialog,
            title = { Text("Choose Save File to Overwrite:") },
            text = {
                Column {
                    state.saveSlotInfos.forEachIndexed { index, info ->
                        TextButton(
                            onClick = { viewModel.pickSaveSlot(index) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "${index + 1}. $info",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissSaveDialog) { Text("Cancel") }
            },
        )
    }

    state.confirmOverwriteIndex?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissOverwrite,
            title = { Text("Overwrite?") },
            text = { Text("Overwrite this save file?\n\n${state.saveSlotInfos[it]}") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmOverwrite) { Text("Overwrite") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissOverwrite) { Text("Cancel") }
            },
        )
    }

    if (state.showBuyoutConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissBuyout,
            title = { Text("Cut / Buy out ${state.buyoutPlayerName}") },
            text = { Text("Cost: ${state.buyoutCostLabel}") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmBuyout) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissBuyout) { Text("Cancel") }
            },
        )
    }

    if (state.showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLeave,
            title = { Text("Leave Talent Hub?") },
            text = {
                Text("Leaving will discard the in-memory offseason session. Save first if you want to keep progress.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmLeave) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLeave) { Text("Stay") }
            },
        )
    }
}

@Composable
private fun BudgetChip(label: String) {
    Text(
        text = label,
        color = FcChipMoneyText,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FcChipMoneyBg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersRow(state: TalentHubUiState, viewModel: TalentHubViewModel) {
    var posExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(
            value = state.search,
            onValueChange = viewModel::setSearch,
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = posExpanded,
                onExpandedChange = { posExpanded = !posExpanded },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = state.positionFilter,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pos") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(posExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = posExpanded, onDismissRequest = { posExpanded = false }) {
                    POSITIONS.forEach { pos ->
                        DropdownMenuItem(
                            text = { Text(pos) },
                            onClick = {
                                viewModel.setPositionFilter(pos)
                                posExpanded = false
                            },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = sortExpanded,
                onExpandedChange = { sortExpanded = !sortExpanded },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = SORTS.getOrElse(state.sortMode) { SORTS[0] },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sort") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sortExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    SORTS.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setSortMode(index)
                                sortExpanded = false
                            },
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.affordableOnly,
                onCheckedChange = viewModel::setAffordableOnly,
            )
            Text("Affordable only")
        }
    }
}

@Composable
private fun TalentRowCard(
    row: TalentRowUi,
    onClick: () -> Unit,
    onCheck: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.showCheck) {
            Checkbox(checked = row.checked, onCheckedChange = { onCheck() })
            Spacer(Modifier.width(4.dp))
        }
        if (!row.moneyRow) {
            Text(
                text = row.position,
                color = FcChipPosText,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(FcChipPosBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(row.primary, fontWeight = FontWeight.SemiBold)
            if (row.secondary.isNotEmpty()) {
                Text(row.secondary, style = MaterialTheme.typography.bodySmall)
            }
            Text(row.costLine, color = FcChipMoneyText, style = MaterialTheme.typography.bodySmall)
        }
        if (!row.moneyRow) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ovrColor(row.ovr))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = row.ovr.toString(),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(row.statusLabel, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferBottomSheet(
    sheet: OfferSheetState,
    onDismiss: () -> Unit,
    onStatus: (RosterStatus) -> Unit,
    onYears: (Int) -> Unit,
    onConfirm: () -> Unit,
    onBuyout: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var yearsExpanded by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    sheet.position,
                    color = FcChipPosText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FcChipPosBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(sheet.playerName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ovrColor(sheet.ovr))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(sheet.ovr.toString(), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            Text("${sheet.yearLine} · ${sheet.secondary}", style = MaterialTheme.typography.bodyMedium)
            if (!sheet.draftStay) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        RosterStatus.PWO to "PWO",
                        RosterStatus.SCHOLARSHIP to "Scholly",
                        RosterStatus.SCHOLARSHIP_PLUS_NIL to "NIL",
                    ).forEach { (status, label) ->
                        FilterChip(
                            selected = sheet.status == status,
                            onClick = { onStatus(status) },
                            label = { Text(label) },
                        )
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = yearsExpanded,
                    onExpandedChange = { yearsExpanded = !yearsExpanded },
                ) {
                    OutlinedTextField(
                        value = "${sheet.years} year" + if (sheet.years == 1) "" else "s",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Years") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(yearsExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = yearsExpanded, onDismissRequest = { yearsExpanded = false }) {
                        (1..sheet.maxYears).forEach { y ->
                            DropdownMenuItem(
                                text = { Text("$y year" + if (y == 1) "" else "s") },
                                onClick = {
                                    onYears(y)
                                    yearsExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            Text(sheet.costPreview, color = FcChipMoneyText)
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text(sheet.confirmLabel)
            }
            if (sheet.showBuyout) {
                OutlinedButton(onClick = onBuyout, modifier = Modifier.fillMaxWidth()) {
                    Text(sheet.buyoutLabel)
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScheduleTabContent(state: TalentHubUiState, viewModel: TalentHubViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (state.rivalSummary.isNotBlank()) {
            Text(
                text = "Rivals: ${state.rivalSummary}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "OOC slate — tap open weeks to pick or sign deals. " +
                    "${state.filledOocSlots} filled · ${state.openOocSlots} open.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = viewModel::resuggestOocSchedule) {
                Text("Resuggest")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            if (state.contractRows.isNotEmpty()) {
                item {
                    Text(
                        "Upcoming OOC contracts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(state.contractRows, key = { it.id }) { row ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(12.dp),
                    ) {
                        Text(row.summary, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { viewModel.cancelContract(row.id) }) {
                            Text("Cancel deal")
                        }
                    }
                }
            }
            items(state.scheduleWeeks, key = { it.week }) { week ->
                val clickable = !week.locked
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .then(
                            if (clickable) Modifier.clickable {
                                if (week.open) viewModel.openOpponentPicker(week.week)
                                else viewModel.clearScheduleWeek(week.week)
                            } else Modifier
                        )
                        .padding(12.dp),
                ) {
                    val rivalry = week.rivalryLabel?.let { " · $it rival" } ?: ""
                    Text(
                        "${week.weekLabel} · ${week.status}$rivalry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(week.detail, style = MaterialTheme.typography.bodySmall)
                    when {
                        week.contractLocked ->
                            Text("Contract locked — cancel deal to change", style = MaterialTheme.typography.labelSmall)
                        !week.locked && !week.open ->
                            Text("Tap to clear and re-pick", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

