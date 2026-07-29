package achijones.footballcoach.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import achijones.footballcoach.ui.components.SegmentedControl
import achijones.footballcoach.ui.components.TeamLogo
import achijones.footballcoach.ui.components.rememberLogoNeedsContrastBoost
import achijones.footballcoach.ui.components.rememberSheetFlingBlocker
import achijones.footballcoach.ui.components.rememberTeamColors
import achijones.footballcoach.ui.theme.onColorFor

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun ScheduleScreen(
    onNavigateToMain: () -> Unit,
    viewModel: ScheduleViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    BackHandler { viewModel.requestBack() }

    LaunchedEffect(state.navigateToMain) {
        if (state.navigateToMain) {
            viewModel.consumeNavigateToMain()
            onNavigateToMain()
        }
    }
    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        viewModel.clearMessage()
    }
    LaunchedEffect(state.missingLeague) {
        if (state.missingLeague) {
            snackbar.showSnackbar("League session missing.")
            onNavigateToMain()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule & Contracts") },
                navigationIcon = {
                    IconButton(onClick = viewModel::requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp),
        ) {
            item(key = "header") {
                Text(
                    "${state.teamName} — ${state.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.rivalSummary.isNotBlank()) {
                    Text(
                        "Rivals: ${state.rivalSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (state.schedulingActive) {
                        "OOC slate — tap open weeks to sign deals. " +
                            "${state.filledOocSlots} filled · ${state.openOocSlots} open. " +
                            "Leftover weeks fill when you tap Done."
                    } else {
                        "Contract desk — review obligations and sign future deals. " +
                            "Week picks unlock in the scheduling phase."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (state.budgetLabel.isNotBlank()) {
                    Text(
                        state.budgetLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            if (state.schedulingActive) {
                stickyHeader(key = "open-progress") {
                    OpenWeekProgressStrip(
                        state = state,
                        onOpenWeek = viewModel::openOpponentPicker,
                        onPlaceAll = viewModel::placeAllOpenContracts,
                    )
                }
                item(key = "week-board-title") {
                    Text(
                        "This year — week board",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(state.scheduleWeeks, key = { "week-${it.week}" }) { week ->
                    WeekBoardCard(
                        week = week,
                        onClick = {
                            when {
                                week.contractLocked && week.contractId != null ->
                                    viewModel.openRescheduleDialog(week.contractId, state.year)
                                week.open -> viewModel.openOpponentPicker(week.week)
                                !week.locked && !week.open -> viewModel.clearScheduleWeek(week.week)
                            }
                        },
                    )
                }
            }

            item(key = "horizon") {
                Text(
                    "Horizon",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.horizonYears.forEach { year ->
                        FilterChip(
                            selected = state.selectedHorizonYear == year,
                            onClick = { viewModel.selectHorizonYear(year) },
                            label = { Text(year.toString()) },
                        )
                    }
                }
                if (state.oocNetLabel.isNotBlank()) {
                    Text(
                        state.oocNetLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (!state.schedulingActive || state.selectedHorizonYear > state.year) {
                    TextButton(onClick = viewModel::openFutureDealPicker) {
                        Text("Sign deal for ${state.selectedHorizonYear}")
                    }
                }
            }

            item(key = "contract-desk-title") {
                Text(
                    "Contract desk",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (state.filteredContracts.isEmpty()) {
                item(key = "no-contracts") {
                    Text(
                        "No contracts for ${state.selectedHorizonYear}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.filteredContracts, key = { it.id }) { card ->
                    ContractDeskCard(
                        card = card,
                        userAbbr = state.teamAbbr,
                        onCancel = { viewModel.requestCancelContract(card.id) },
                        onChangeDate = { year ->
                            viewModel.openRescheduleDialog(card.id, year)
                        },
                    )
                }
            }

            if (!state.schedulingActive) {
                item(key = "week-board-title-desk") {
                    Text(
                        "This year — week board",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(state.scheduleWeeks, key = { "desk-week-${it.week}" }) { week ->
                    WeekBoardCard(
                        week = week,
                        onClick = {
                            when {
                                week.contractLocked && week.contractId != null ->
                                    viewModel.openRescheduleDialog(week.contractId, state.year)
                                week.open -> viewModel.openOpponentPicker(week.week)
                                !week.locked && !week.open -> viewModel.clearScheduleWeek(week.week)
                            }
                        },
                    )
                }
            }
        }
    }

    if (state.dealTargetYear != null) {
        OpponentDealSheet(state, viewModel)
    }

    if (state.showDonePreview) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDonePreview,
            title = { Text("CPU will fill open weeks") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.donePreviewLines.isEmpty()) {
                        Text("${state.openOocSlots} open weeks will be auto-filled.")
                    } else {
                        state.donePreviewLines.forEach { line ->
                            Text(line.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDonePreview) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDonePreview) {
                    Text("Keep editing")
                }
            },
        )
    }

    state.cancelConfirmLabel?.let { label ->
        AlertDialog(
            onDismissRequest = viewModel::dismissCancelConfirm,
            title = { Text("Cancel deal") },
            text = { Text(label) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmCancelContract) {
                    Text("Cancel deal")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCancelConfirm) {
                    Text("Keep")
                }
            },
        )
    }

    if (state.rescheduleContractId != null) {
        RescheduleDialog(state, viewModel)
    }
}

@Composable
private fun OpenWeekProgressStrip(
    state: ScheduleUiState,
    onOpenWeek: (Int) -> Unit,
    onPlaceAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 6.dp),
    ) {
        Text(
            "${state.openOocSlots} open · Weeks " +
                state.openWeekNumbers.joinToString(", ") { "${it + 1}" }
                    .ifBlank { "—" },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (state.openWeekNumbers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.openWeekNumbers.forEach { week ->
                    FilterChip(
                        selected = false,
                        onClick = { onOpenWeek(week) },
                        label = { Text("W${week + 1}") },
                    )
                }
            }
        }
        if (state.unplacedOpenContractCount > 0) {
            TextButton(
                onClick = onPlaceAll,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text("Place all open contracts (${state.unplacedOpenContractCount})")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContractDeskCard(
    card: ContractCardUi,
    userAbbr: String,
    onCancel: () -> Unit,
    onChangeDate: (Int) -> Unit,
) {
    val opponentAbbr = if (card.teamAAbbr == userAbbr) card.teamBAbbr else card.teamAAbbr
    val opponentName = if (card.teamAAbbr == userAbbr) card.teamBName else card.teamAName
    val colors = rememberTeamColors(opponentName, opponentAbbr)
    val leftColor = colors.primary.copy(alpha = 0.55f)
    val brush = Brush.horizontalGradient(
        listOf(
            leftColor,
            colors.secondary.copy(alpha = 0.35f),
        ),
    )
    val contrastBoost = rememberLogoNeedsContrastBoost(opponentName, leftColor)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamLogo(
                opponentName,
                opponentAbbr,
                size = 36.dp,
                framed = false,
                contrastBoost = contrastBoost,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                opponentName ?: opponentAbbr,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TypeBadge(card.typeLabel)
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            card.games.forEach { g ->
                Text(
                    "${g.year} ${g.role}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .clickable(enabled = g.canReschedule) { onChangeDate(g.year) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Text(
            card.statusLabel,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp),
        )
        card.games.firstOrNull { it.guaranteeLabel != null }?.guaranteeLabel?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Fulfill by ${card.fulfillByYear} · Cancel buyout ${card.buyoutLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 4.dp),
        )
        val canReschedule = card.games.any { it.canReschedule }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (canReschedule) {
                OutlinedButton(
                    onClick = {
                        card.games.firstOrNull { it.canReschedule }?.let { onChangeDate(it.year) }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Change date")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel deal")
                }
            } else {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancel deal")
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(label: String) {
    val color = when (label) {
        "H&H" -> Color(0xFF2A9D8F)
        "Buy" -> Color(0xFFE76F51)
        else -> Color(0xFF457B9D)
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun WeekBoardCard(week: ScheduleWeekUi, onClick: () -> Unit) {
    val colors = if (week.opponentAbbr != null) {
        rememberTeamColors(week.opponentName, week.opponentAbbr)
    } else {
        null
    }
    val leftColor = colors?.primary?.copy(alpha = 0.55f)
    val brush = when {
        week.isBye -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            ),
        )
        colors != null && leftColor != null -> Brush.horizontalGradient(
            listOf(
                leftColor,
                colors.secondary.copy(alpha = 0.35f),
            ),
        )
        week.open -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ),
        )
        else -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        )
    }
    val logoBackground = leftColor ?: MaterialTheme.colorScheme.surface
    val contrastBoost = rememberLogoNeedsContrastBoost(week.opponentName, logoBackground)
    val clickable = week.contractLocked
        || week.open
        || (!week.locked && !week.open && !week.isBye)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (week.opponentAbbr != null) {
            TeamLogo(
                week.opponentName,
                week.opponentAbbr,
                size = 40.dp,
                framed = false,
                contrastBoost = contrastBoost,
            )
            Spacer(modifier = Modifier.width(12.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (week.isBye) "BYE" else "?",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            val rivalry = week.rivalryLabel?.let { " · $it" } ?: ""
            Text(
                "${week.weekLabel} · ${week.status}$rivalry",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(week.detail, style = MaterialTheme.typography.bodySmall)
            week.moneyLabel?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
            when {
                week.contractLocked ->
                    Text(
                        "Tap to change date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                clickable && !week.open ->
                    Text(
                        "Tap to clear",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpponentDealSheet(state: ScheduleUiState, viewModel: ScheduleViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val weekLabel = state.opponentPickerWeek?.let { "Week ${it + 1}" } ?: "Future deal"
    val selected = state.opponentOptions.find {
        it.abbr == state.dealOpponentAbbr && it.openContractId == null
    }
    val fulfillByYear = (state.dealTargetYear ?: 0) + state.hhReturnOffset
    val filteredOptions = state.opponentOptions.filter { opt ->
        state.conferenceFilter == null || opt.conference == state.conferenceFilter
    }
    val sectionOrder = listOf(
        OpponentSection.OPEN_CONTRACT,
        OpponentSection.RIVALRY,
        OpponentSection.BUY,
        OpponentSection.EARN,
        OpponentSection.PEER,
    )
    val hasOpenContracts = state.opponentOptions.any { it.section == OpponentSection.OPEN_CONTRACT }

    ModalBottomSheet(
        onDismissRequest = viewModel::dismissOpponentPicker,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Pick opponent · $weekLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Out of conference · ${state.dealTargetYear}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.budgetLabel.isNotBlank()) {
                        Text(
                            state.budgetLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                TextButton(onClick = viewModel::dismissOpponentPicker) {
                    Text("Close")
                }
            }

            selected?.let { opt ->
                val colors = rememberTeamColors(opt.name, opt.abbr)
                val leftColor = colors.primary.copy(alpha = 0.45f)
                val brush = Brush.horizontalGradient(
                    listOf(leftColor, colors.secondary.copy(alpha = 0.28f)),
                )
                val contrastBoost = rememberLogoNeedsContrastBoost(opt.name, leftColor)
                val twoForOneFulfillBy = fulfillByYear + 1
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        onClick = viewModel::clearDealOpponent,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("← Opponents")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TeamLogo(
                            opt.name,
                            opt.abbr,
                            size = 40.dp,
                            framed = false,
                            contrastBoost = contrastBoost,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                opt.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                buildString {
                                    append(opt.conference)
                                    opt.matchupLabel?.let { append(" · $it") }
                                    opt.rivalryLabel?.let { append(" · $it") }
                                    opt.dealTypeChip?.let { append(" · $it") }
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    SegmentedControl(
                        labels = listOf("@${state.teamName}", "@${opt.name}"),
                        selected = if (state.dealSite == DealSite.HOME) 0 else 1,
                        onSelect = { index ->
                            viewModel.setDealSite(
                                if (index == 0) DealSite.HOME else DealSite.AWAY,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DealQuoteText(state.dealQuote, state.singleGameAllowed)
                    Button(
                        onClick = viewModel::signSingleGame,
                        enabled = state.singleGameAllowed,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sign single game")
                    }
                    Text(
                        "Series · return in +${state.hhReturnOffset} yr",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(1, 2, 3, 4, 5, 6).forEach { offset ->
                            FilterChip(
                                selected = state.hhReturnOffset == offset,
                                onClick = { viewModel.setHhReturnOffset(offset) },
                                label = { Text("+$offset yr") },
                            )
                        }
                    }
                    DealQuoteText(state.hhQuote, state.hhAllowed)
                    OutlinedButton(
                        onClick = viewModel::signHomeAndHomeDeal,
                        enabled = state.hhAllowed,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sign home-and-home (by $fulfillByYear)")
                    }
                    DealQuoteText(state.twoForOneQuote, state.twoForOneAllowed)
                    OutlinedButton(
                        onClick = viewModel::signTwoForOneDeal,
                        enabled = state.twoForOneAllowed,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sign 2-for-1 (by $twoForOneFulfillBy)")
                    }
                }
            }

            if (selected == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = state.siteFilter == null,
                        onClick = { viewModel.setSiteFilter(null) },
                        label = { Text("All sites") },
                    )
                    FilterChip(
                        selected = state.siteFilter == DealSite.HOME,
                        onClick = { viewModel.setSiteFilter(DealSite.HOME) },
                        label = { Text("Home") },
                    )
                    FilterChip(
                        selected = state.siteFilter == DealSite.AWAY,
                        onClick = { viewModel.setSiteFilter(DealSite.AWAY) },
                        label = { Text("Away") },
                    )
                }

                if (state.availableConferences.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = state.conferenceFilter == null,
                            onClick = { viewModel.setConferenceFilter(null) },
                            label = { Text("All") },
                        )
                        state.availableConferences.forEach { conf ->
                            FilterChip(
                                selected = state.conferenceFilter == conf,
                                onClick = { viewModel.setConferenceFilter(conf) },
                                label = { Text(conf) },
                            )
                        }
                    }
                }

                if (hasOpenContracts) {
                    TextButton(
                        onClick = viewModel::placeAllOpenContracts,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text("Place all open contracts")
                    }
                }

                Text(
                    "Choose a team",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(rememberSheetFlingBlocker()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    sectionOrder.forEach { section ->
                        val sectionItems = filteredOptions.filter { it.section == section }
                        if (sectionItems.isEmpty()) return@forEach
                        item(key = "section-${section.name}") {
                            Text(
                                sectionTitle(section),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(
                            sectionItems,
                            key = { "${section.name}-${it.abbr}-${it.openContractId}" },
                        ) { opt ->
                            OpponentPickerRow(
                                opt = opt,
                                showConference = state.conferenceFilter == null,
                                selected = opt.abbr == state.dealOpponentAbbr &&
                                    opt.openContractId == null,
                                onClick = { viewModel.selectDealOpponent(opt.abbr) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DealQuoteText(quote: String, available: Boolean) {
    if (quote.isBlank()) return
    Text(
        quote,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (available) FontWeight.SemiBold else FontWeight.Normal,
        color = if (available) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun OpponentPickerRow(
    opt: OpponentOptionUi,
    showConference: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = rememberTeamColors(opt.name, opt.abbr)
    val leftColor = colors.primary.copy(alpha = if (selected) 0.55f else 0.4f)
    val brush = Brush.horizontalGradient(
        listOf(
            leftColor,
            colors.secondary.copy(alpha = if (selected) 0.35f else 0.22f),
        ),
    )
    val contrastBoost = rememberLogoNeedsContrastBoost(opt.name, leftColor)
    val moneyColor = when (opt.moneyKind) {
        OpponentMoneyKind.PAY -> Color(0xFFE76F51)
        OpponentMoneyKind.EARN -> Color(0xFF2A9D8F)
        OpponentMoneyKind.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (opt.affordable) 1f else 0.45f)
            .clip(RoundedCornerShape(10.dp))
            .background(brush)
            .then(
                if (selected) {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(10.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeamLogo(
            opt.name,
            opt.abbr,
            size = 36.dp,
            framed = false,
            contrastBoost = contrastBoost,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                opt.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = onColorFor(leftColor).copy(alpha = 0.95f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    if (showConference) {
                        append(opt.conference)
                    }
                    opt.matchupLabel?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                    opt.rivalryLabel?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                    opt.dealTypeChip?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                    if (opt.openContractId != null) {
                        if (isNotEmpty()) append(" · ")
                        append("Tap to place")
                    }
                    if (!opt.affordable && opt.openContractId == null) {
                        if (isNotEmpty()) append(" · ")
                        append("Can't afford")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            opt.dealTypeChip?.let { chip ->
                Text(
                    chip,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE76F51),
                )
            }
            Text(
                opt.moneyLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = moneyColor,
            )
        }
    }
}

private fun sectionTitle(section: OpponentSection): String {
    return when (section) {
        OpponentSection.OPEN_CONTRACT -> "Open contracts"
        OpponentSection.RIVALRY -> "Rivalries"
        OpponentSection.BUY -> "Buy games"
        OpponentSection.EARN -> "Road paydays"
        OpponentSection.PEER -> "Peers"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RescheduleDialog(state: ScheduleUiState, viewModel: ScheduleViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismissRescheduleDialog,
        title = { Text("Change date") },
        text = {
            Column {
                Text(
                    state.rescheduleOpponentLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Editable until ${state.rescheduleFulfillByYear}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    "Year",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.rescheduleEligibleYears.forEach { year ->
                        FilterChip(
                            selected = state.rescheduleSelectedYear == year,
                            onClick = { viewModel.selectRescheduleYear(year) },
                            label = { Text(year.toString()) },
                        )
                    }
                }
                Text(
                    "Week",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.rescheduleEligibleWeeks.forEach { week ->
                        FilterChip(
                            selected = state.rescheduleSelectedWeek == week,
                            onClick = { viewModel.selectRescheduleWeek(week) },
                            label = { Text("W${week + 1}") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::confirmReschedule) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissRescheduleDialog) {
                Text("Cancel")
            }
        },
    )
}
