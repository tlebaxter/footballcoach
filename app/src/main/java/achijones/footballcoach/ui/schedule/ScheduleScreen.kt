package achijones.footballcoach.ui.schedule

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import achijones.footballcoach.ui.components.TeamLogo
import achijones.footballcoach.ui.components.rememberLogoNeedsContrastBoost
import achijones.footballcoach.ui.components.rememberTeamColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
            item {
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
                        "OOC slate — tap open weeks to pick or sign deals. " +
                            "${state.filledOocSlots} filled · ${state.openOocSlots} open."
                    } else {
                        "Contract desk — review obligations and sign future deals. " +
                            "Week picks unlock in the scheduling phase."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            item {
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
                if (!state.schedulingActive || state.selectedHorizonYear > state.year) {
                    TextButton(onClick = viewModel::openFutureDealPicker) {
                        Text("Sign deal for ${state.selectedHorizonYear}")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Contract desk",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (state.schedulingActive) {
                        TextButton(onClick = viewModel::resuggestOocSchedule) {
                            Text("Resuggest")
                        }
                    }
                }
            }

            if (state.filteredContracts.isEmpty()) {
                item {
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

            item {
                Text(
                    "This year — week board",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            items(state.scheduleWeeks, key = { it.week }) { week ->
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

    if (state.dealTargetYear != null) {
        OpponentDealDialog(state, viewModel)
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
        if (card.games.any { it.canReschedule }) {
            TextButton(onClick = {
                card.games.firstOrNull { it.canReschedule }?.let { onChangeDate(it.year) }
            }) {
                Text("Change date")
            }
        }
        TextButton(onClick = onCancel) {
            Text("Cancel deal")
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
                        "Tap to change week or year",
                        style = MaterialTheme.typography.labelSmall,
                    )
                clickable && !week.open ->
                    Text("Tap to clear and re-pick", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpponentDealDialog(state: ScheduleUiState, viewModel: ScheduleViewModel) {
    val weekLabel = state.opponentPickerWeek?.let { "Week ${it + 1}" } ?: "Future deal"
    AlertDialog(
        onDismissRequest = viewModel::dismissOpponentPicker,
        title = { Text("OOC — $weekLabel (${state.dealTargetYear})") },
        text = {
            Column {
                if (state.dealOpponentAbbr != null) {
                    Text(
                        state.dealQuote,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { viewModel.signSingleGame(false) }) {
                            Text("Single")
                        }
                        TextButton(onClick = { viewModel.signSingleGame(true) }) {
                            Text("Single + $")
                        }
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
                    Text(
                        "H&H return in +${state.hhReturnOffset} yr (fulfill by ${(state.dealTargetYear ?: 0) + state.hhReturnOffset})",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(1, 2, 3, 4, 5, 6).forEach { offset ->
                            FilterChip(
                                selected = state.hhReturnOffset == offset,
                                onClick = { viewModel.setHhReturnOffset(offset) },
                                label = { Text("+$offset") },
                            )
                        }
                    }
                    TextButton(onClick = viewModel::signHomeAndHomeDeal) {
                        Text("Sign home-and-home")
                    }
                }
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(state.opponentOptions, key = { it.abbr }) { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectDealOpponent(opt.abbr) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TeamLogo(opt.name, opt.abbr, size = 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${opt.name} (${opt.abbr})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${opt.conference}" +
                                        (opt.rivalryLabel?.let { " · $it" } ?: "") +
                                        " · ${opt.buyHint}",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            OutlinedButton(onClick = { viewModel.declareRival(opt.abbr) }) {
                                Text("Rival")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::dismissOpponentPicker) {
                Text("Close")
            }
        },
    )
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
                    fontWeight = FontWeight.SemiBold,
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Week",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.rescheduleEligibleWeeks.isEmpty()) {
                    Text(
                        "No open weeks available for that year yet.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::confirmReschedule,
                enabled = state.rescheduleSelectedYear != null,
            ) {
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

