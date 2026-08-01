package achijones.footballcoach.ui.talenthub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.VisualTransformation
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
    "ALL", "QB", "RB", "FB", "WR", "TE", "OL", "K", "P", "S", "CB", "EDGE", "DL", "LB",
)
private val SORTS = listOf("OVR ↓", "Cost ↑", "Name")

private val CompactFieldContentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalentHubScreen(
    onNavigateToMain: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToSchedule: () -> Unit = {},
    viewModel: TalentHubViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    BackHandler { viewModel.requestBackToMain() }

    LaunchedEffect(state.navigateToMain) {
        if (state.navigateToMain) {
            viewModel.consumeNavigateToMain()
            onNavigateToMain()
        }
    }
    LaunchedEffect(state.navigateToSchedule) {
        if (state.navigateToSchedule) {
            viewModel.consumeNavigateToSchedule()
            onNavigateToSchedule()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::requestBackToMain) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to team",
                    )
                }
                Text(
                    text = "${state.teamName} — ${state.phaseLabel}" +
                        if (state.browsing) " (browsing)" else "",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::openSaveDialog) {
                    Icon(Icons.Default.Save, contentDescription = "Save League")
                }
            }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BudgetChip(state.cashLabel)
                BudgetChip(state.purseLabel)
                BudgetChip(state.y1Label)
                BudgetChip(state.schollyLabel)
                BudgetChip(state.rosterLabel)
            }
            Spacer(Modifier.height(4.dp))
            SegmentedControl(
                labels = listOf("Retain", "Portal", "HS", "Money"),
                selected = state.selectedTab.ordinal,
                onSelect = { viewModel.selectTab(HubTab.entries[it]) },
                modifier = Modifier.padding(vertical = 4.dp),
            )
            TabContentTransition(
                targetState = state.selectedTab,
                modifier = Modifier.fillMaxSize(),
                label = "talentHubTabContent",
            ) { tab ->
                Column(modifier = Modifier.fillMaxSize()) {
                    if (tab != HubTab.MONEY) {
                        FiltersRow(state, viewModel)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
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
            title = { Text("Choose Save File:") },
            text = {
                Column {
                    state.saveSlotInfos.forEach { info ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = { viewModel.pickSaveSlot(info.index) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    achijones.footballcoach.ui.util.SaveSlots.label(info),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start,
                                )
                            }
                            if (info.status != achijones.footballcoach.save.SlotStatus.EMPTY) {
                                TextButton(onClick = { viewModel.requestDeleteSlot(info.index) }) {
                                    Text("Del")
                                }
                            }
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
        val info = state.saveSlotInfos.getOrNull(it)
        AlertDialog(
            onDismissRequest = viewModel::dismissOverwrite,
            title = { Text("Overwrite?") },
            text = {
                Text(
                    "Overwrite this save file?\n\n" +
                        (info?.let { s -> achijones.footballcoach.ui.util.SaveSlots.label(s) } ?: ""),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmOverwrite) { Text("Overwrite") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissOverwrite) { Text("Cancel") }
            },
        )
    }

    state.confirmDeleteIndex?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete slot ${it + 1}?") },
            text = { Text("This permanently removes the career in this slot.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text("Cancel") }
            },
        )
    }

    if (state.showBuyoutConfirm) {
        val hasBuyout = state.buyoutCostLabel != null
        AlertDialog(
            onDismissRequest = viewModel::dismissBuyout,
            title = {
                Text(
                    if (hasBuyout) {
                        "Cut / Buy out ${state.buyoutPlayerName}"
                    } else {
                        "Cut ${state.buyoutPlayerName}"
                    },
                )
            },
            text = {
                Text(
                    if (hasBuyout) {
                        "Cost: ${state.buyoutCostLabel}"
                    } else {
                        "Release with no buyout."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmBuyout) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissBuyout) { Text("Cancel") }
            },
        )
    }

}

@Composable
private fun BudgetChip(label: String) {
    Text(
        text = label,
        color = FcChipMoneyText,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(FcChipMoneyBg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    readOnly: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()
    val textStyle = MaterialTheme.typography.bodyMedium.merge(
        color = MaterialTheme.colorScheme.onSurface,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(40.dp),
        singleLine = true,
        readOnly = readOnly,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = placeholder?.let { { Text(it, style = textStyle) } },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                colors = colors,
                contentPadding = CompactFieldContentPadding,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersRow(state: TalentHubUiState, viewModel: TalentHubViewModel) {
    var posExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        CompactOutlinedField(
            value = state.search,
            onValueChange = viewModel::setSearch,
            placeholder = "Search",
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExposedDropdownMenuBox(
                expanded = posExpanded,
                onExpandedChange = { posExpanded = !posExpanded },
                modifier = Modifier.weight(1f),
            ) {
                CompactOutlinedField(
                    value = state.positionFilter,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = "Pos",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(posExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
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
                CompactOutlinedField(
                    value = SORTS.getOrElse(state.sortMode) { SORTS[0] },
                    onValueChange = {},
                    readOnly = true,
                    placeholder = "Sort",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sortExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
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
            FilterChip(
                selected = state.affordableOnly,
                onClick = { viewModel.setAffordableOnly(!state.affordableOnly) },
                label = { Text("Affordable") },
            )
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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.showCheck) {
            Checkbox(checked = row.checked, onCheckedChange = { onCheck() })
            Spacer(Modifier.width(4.dp))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
