package achijones.footballcoach.ui.coach

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import CFBsimPack.engine.AutoSimUntil
import CFBsimPack.engine.DefenseConcept
import CFBsimPack.engine.GameSituation
import CFBsimPack.engine.OffenseConcept
import CFBsimPack.engine.Playbook
import CFBsimPack.engine.TempoCall
import achijones.footballcoach.ui.components.SegmentedControl
private val ScoreboardBg = Color(0xFF0D1117)
private val PanelBg = Color(0xFF121A14)
private val GhostBorder = Color(0xFF3A4A3C)
private val MutedText = Color(0xFF9CA3AF)
private val BallOrange = Color(0xFFF59E0B)
private val PanelShape = RoundedCornerShape(16.dp)
private val PlayButtonShape = RoundedCornerShape(14.dp)

@Composable
fun CoachGameScreen(
    onFinished: () -> Unit,
    viewModel: CoachGameViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    BackHandler {
        viewModel.finishAndClose()
        onFinished()
    }

    if (state.finished) {
        Column(
            Modifier
                .fillMaxSize()
                .background(ScoreboardBg)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                state.error ?: "Final",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text("Head back to the season hub.", color = MutedText)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.finishAndClose()
                    onFinished()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = PlayButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("Back to season", fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    val sit = state.situation
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF071208), Color(0xFF0B1F0B), Color(0xFF0A160C)),
                ),
            ),
    ) {
        if (sit != null) {
            ScoreboardHeader(sit)
            CoachTabBar(state.tab, viewModel::selectTab)

            when (state.tab) {
                CoachTab.CALL_PLAYS -> CallPlaysTab(
                    sit,
                    state,
                    viewModel,
                    Modifier.weight(1f),
                )
                CoachTab.LOG -> Box(Modifier.weight(1f)) { CoachLogTab(sit) }
                CoachTab.BOX_SCORE -> Box(Modifier.weight(1f)) { CoachBoxScoreTab(sit) }
            }
        } else {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Loading game…", color = MutedText)
            }
        }

        TextButton(
            onClick = {
                viewModel.finishAndClose()
                onFinished()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text("Exit · sim rest if needed", color = MutedText)
        }
    }

    if (state.showCoinToss && sit != null) {
        val winnerAbbr = if (sit.homeWonToss) sit.homeAbbr else sit.awayAbbr
        CoinTossSheet(
            winnerAbbr = winnerAbbr,
            onConfirm = viewModel::confirmCoinToss,
        )
    }

    if (state.showTryChoice && sit != null) {
        val scoringAbbr = if (sit.possessionHome) sit.homeAbbr else sit.awayAbbr
        TryChoiceSheet(
            scoringAbbr = scoringAbbr,
            onKickXp = viewModel::chooseKickXp,
            onGoForTwo = viewModel::chooseGoForTwo,
        )
    }

    if (state.showPlayPicker && sit != null && !state.showCoinToss && !state.showTryChoice) {
        PlayPickerSheet(
            userOnOffense = sit.userOnOffense,
            situation = sit,
            selectedFormation = state.playPickerFormation,
            selectedOffenseId = state.selectedOffense.id,
            selectedDefenseId = state.selectedDefense.id,
            onFormationChange = viewModel::setPlayPickerFormation,
            onSelectOffense = viewModel::selectOffenseConcept,
            onSelectDefense = viewModel::selectDefenseConcept,
            onDismiss = viewModel::closePlayPicker,
        )
    }

    if (state.showTimeoutConfirm && sit != null) {
        val userTimeouts = if (sit.userOnOffense == sit.possessionHome) {
            sit.homeTimeouts
        } else {
            sit.awayTimeouts
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissTimeoutConfirm,
            title = { Text("Call timeout?") },
            text = {
                Text("Use one of your timeouts? ($userTimeouts remaining)")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmTimeout) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissTimeoutConfirm) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CoachTabBar(selected: CoachTab, onSelect: (CoachTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(ScoreboardBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CoachTab.entries.forEach { tab ->
            val label = when (tab) {
                CoachTab.CALL_PLAYS -> "Call Plays"
                CoachTab.LOG -> "Log"
                CoachTab.BOX_SCORE -> "Box Score"
            }
            val active = tab == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (active) MaterialTheme.colorScheme.primary else GhostBorder.copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) MaterialTheme.colorScheme.primary else MutedText,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun CallPlaysTab(
    sit: GameSituation,
    state: CoachUiState,
    viewModel: CoachGameViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        SituationStrip(sit)
        Spacer(Modifier.height(8.dp))
        CoachField(
            yardLine = sit.yardLine,
            distance = sit.distance,
            down = sit.down,
            drivePath = sit.drivePath,
            possessionHome = sit.possessionHome,
            homeDefendsLeft = sit.homeDefendsLeft,
            homeName = sit.homeName,
            awayName = sit.awayName,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        )
        Spacer(Modifier.height(8.dp))
        LastPlayBanner(sit)
        Spacer(Modifier.height(10.dp))
        ControlCard(sit, state, viewModel)
        Spacer(Modifier.height(10.dp))
        SelectedPlayCard(
            situation = sit,
            userOnOffense = sit.userOnOffense,
            offense = state.selectedOffense,
            defense = state.selectedDefense,
            onChangePlay = { viewModel.openPlayPicker() },
            onSuggestion = { viewModel.applySuggestion() },
        )
        Spacer(Modifier.height(8.dp))
        OpponentTeaser(sit, state.selectedDefense)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ScoreboardHeader(sit: GameSituation) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ScoreboardBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamScoreBlock(
                rank = sit.awayRank,
                abbr = sit.awayAbbr,
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
                abbr = sit.homeAbbr,
                score = sit.homeScore,
                hasBall = sit.possessionHome,
                modifier = Modifier.weight(1f),
                alignEnd = true,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            sit.downDistanceLabel,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeoutPips(count = sit.awayTimeouts, label = sit.awayAbbr)
            Text("TIMEOUTS", color = Color(0xFF6B7280), style = MaterialTheme.typography.labelSmall)
            TimeoutPips(count = sit.homeTimeouts, label = sit.homeAbbr)
        }
    }
}

@Composable
private fun TeamScoreBlock(
    rank: Int,
    abbr: String,
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
                if (rank in 1..25) "#$rank $abbr" else abbr,
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
private fun TimeoutPips(count: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall)
        repeat(3) { i ->
            Box(
                Modifier
                    .size(width = 14.dp, height = 6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < count) MaterialTheme.colorScheme.primary else Color(0xFF374151)),
            )
        }
    }
}

@Composable
private fun SituationStrip(sit: GameSituation) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF152018))
            .border(1.dp, GhostBorder.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            sit.downDistanceLabel,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            if (sit.userOnOffense) "YOUR BALL" else "DEFENDING",
            color = if (sit.userOnOffense) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun LastPlayBanner(sit: GameSituation) {
    val lastOff = sit.lastOffenseConceptId?.let { Playbook.offenseById(it) }
    val lastDef = sit.lastDefenseConceptId?.let { Playbook.defenseById(it) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A241C))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LAST",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(end = 10.dp),
            )
            Text(
                sit.lastPlay.ifBlank { "Awaiting play call…" },
                color = Color(0xFFE5E7EB),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (lastOff != null || lastDef != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                listOfNotNull(
                    lastOff?.callSheetLine(),
                    lastDef?.let { "vs ${it.displayName}" },
                ).joinToString(" "),
                color = MutedText,
                style = MaterialTheme.typography.labelMedium,
            )
            if (lastOff?.concept?.isNotBlank() == true) {
                Text(lastOff.concept, color = Color(0xFFD1D5DB), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ControlCard(
    sit: GameSituation,
    state: CoachUiState,
    viewModel: CoachGameViewModel,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(PanelBg)
            .border(1.dp, GhostBorder.copy(alpha = 0.6f), PanelShape)
            .padding(14.dp),
    ) {
        Text(
            if (sit.userOnOffense) "Offense pace" else "Defense call",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(8.dp))
        if (sit.userOnOffense) {
            val tempos = TempoCall.entries
            SegmentedControl(
                labels = tempos.map { it.displayLabel() },
                selected = tempos.indexOf(state.selectedTempo).coerceAtLeast(0),
                onSelect = { viewModel.selectTempo(tempos[it]) },
            )
            Spacer(Modifier.height(10.dp))
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = viewModel::simPlay,
                enabled = !sit.awaitingCoinToss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = PlayButtonShape,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Text("Sim play", fontWeight = FontWeight.Bold)
            }
            Box(Modifier.weight(1f)) {
                Button(
                    onClick = { viewModel.showSimUntilMenu(true) },
                    enabled = !sit.awaitingCoinToss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                    shape = PlayButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Sim until…", fontWeight = FontWeight.Bold)
                }
                DropdownMenu(
                    expanded = state.showSimUntilMenu,
                    onDismissRequest = { viewModel.showSimUntilMenu(false) },
                ) {
                    AutoSimUntil.entries.forEach { until ->
                        DropdownMenuItem(
                            text = { Text(until.displayLabel()) },
                            onClick = { viewModel.autoSim(until) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.aiCallMode,
                onCheckedChange = viewModel::setAiCallMode,
            )
            Text("AI call next snap", color = Color(0xFFE5E7EB), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = viewModel::requestTimeout,
                enabled = sit.canCallTimeout,
            ) {
                Text(
                    "Timeout",
                    color = if (sit.canCallTimeout) MaterialTheme.colorScheme.secondary else MutedText,
                )
            }
        }
    }
}

@Composable
private fun SelectedPlayCard(
    situation: GameSituation,
    userOnOffense: Boolean,
    offense: OffenseConcept,
    defense: DefenseConcept,
    onChangePlay: () -> Unit,
    onSuggestion: () -> Unit,
) {
    val stDef = !userOnOffense && situation.specialTeamsDown
    val twoPoint = situation.tryIsTwoPoint
    val title = if (userOnOffense) offense.displayName else defense.displayName
    val meta = when {
        userOnOffense && situation.pendingKickoff -> "Kickoff snap"
        twoPoint && userOnOffense -> "2-point try · ${offense.metaLine()}"
        twoPoint && !userOnOffense -> "Defend the 2-point try"
        userOnOffense -> offense.metaLine()
        stDef && situation.pendingKickoff ->
            "KR ${situation.userKickReturnerName ?: "auto"} · return package"
        stDef ->
            "PR ${situation.userPuntReturnerName ?: "auto"} · ST package"
        else -> "Coverage shell"
    }
    val concept = if (userOnOffense) offense.concept else defense.concept
    val changeLabel = when {
        userOnOffense && situation.pendingKickoff -> "Kickoff"
        userOnOffense -> "Change Play"
        stDef -> "Change Package"
        else -> "Change Coverage"
    }
    val eyebrow = when {
        userOnOffense && situation.pendingKickoff -> "KICKOFF"
        twoPoint -> "2-POINT TRY"
        userOnOffense -> offense.formation.displayName.uppercase()
        stDef -> "SPECIAL TEAMS"
        else -> "COVERAGE"
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(PanelBg)
            .border(1.dp, GhostBorder.copy(alpha = 0.6f), PanelShape)
            .padding(14.dp),
    ) {
        Text(
            eyebrow,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(meta, color = MutedText, style = MaterialTheme.typography.labelLarge)
        if (concept.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(concept, color = Color(0xFFE5E7EB), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onChangePlay,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, GhostBorder, PlayButtonShape),
            ) {
                Text(changeLabel, color = Color.White)
            }
            Button(
                onClick = onSuggestion,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = PlayButtonShape,
                modifier = Modifier.weight(1f),
            ) {
                Text("Suggestion", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun OpponentTeaser(sit: GameSituation, userDefense: DefenseConcept) {
    val label = when {
        sit.userOnOffense -> "Opp defense · Unknown"
        sit.tryIsTwoPoint -> "Your coverage · ${userDefense.displayName}"
        sit.specialTeamsDown -> "Your package · ${userDefense.displayName}"
        else -> "Your coverage · ${userDefense.displayName}"
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF101610))
            .border(1.dp, GhostBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelLarge)
    }
}

private fun TempoCall.displayLabel(): String = when (this) {
    TempoCall.NORMAL -> "Normal"
    TempoCall.HURRY_UP -> "Hurry"
    TempoCall.CHEW_CLOCK -> "Chew"
}

private fun AutoSimUntil.displayLabel(): String = when (this) {
    AutoSimUntil.DRIVE -> "Drive"
    AutoSimUntil.POSSESSION -> "Possession"
    AutoSimUntil.QUARTER -> "Quarter"
    AutoSimUntil.HALF -> "Half"
    AutoSimUntil.GAME -> "Game"
}
