package achijones.footballcoach.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import achijones.footballcoach.ui.components.ConferenceLogo
import achijones.footballcoach.ui.components.SegmentedControl
import achijones.footballcoach.ui.components.TabContentTransition
import achijones.footballcoach.ui.components.TeamLogo
import achijones.footballcoach.ui.components.rememberLogoNeedsContrastBoost
import achijones.footballcoach.ui.components.rememberTeamColors
import achijones.footballcoach.ui.theme.FcChipPosBg
import achijones.footballcoach.ui.theme.FcChipPosText
import achijones.footballcoach.ui.theme.FcLoss
import achijones.footballcoach.ui.theme.FcOvrElite
import achijones.footballcoach.ui.theme.FcOvrStarter
import achijones.footballcoach.ui.theme.FcPrimary
import achijones.footballcoach.ui.theme.FcPrimaryDark
import achijones.footballcoach.ui.theme.FcWin
import achijones.footballcoach.ui.theme.gradeColor
import achijones.footballcoach.ui.theme.gradeColorBg
import achijones.footballcoach.ui.theme.ovrColor
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val StatCardShape = RoundedCornerShape(14.dp)
private val StatTileShape = RoundedCornerShape(12.dp)
private val STAT_SECTION_ORDER = listOf("Offense", "Defense", "Program")
private val STAT_SPOTLIGHT_LABELS = listOf("Points", "Opp Points", "Yards", "TO Diff")

private val ROSTER_FILTERS = listOf(
    "ALL", "QB", "RB", "FB", "WR", "TE", "OL", "K", "S", "CB", "EDGE", "DL", "LB",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    onNavigateHome: () -> Unit,
    onNavigateTalentHub: () -> Unit,
    onNavigateCoach: () -> Unit = {},
    onNavigateSchedule: () -> Unit = {},
    viewModel: MainViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    // Hold a blank screen after team pick / offseason handoff so the roster
    // never flashes for a frame before NavHost reaches Talent Hub / Schedule.
    var suppressMainContent by remember { mutableStateOf(false) }

    BackHandler { viewModel.requestExit() }

    LaunchedEffect(Unit) {
        viewModel.onScreenEntered()
    }

    // Returning from Talent Hub / Schedule reuses this composition; clear the
    // outbound blanking flag so Main is visible again.
    LaunchedEffect(state.ready, state.navigateToTalentHub, state.navigateToSchedule) {
        if (state.ready && !state.navigateToTalentHub && !state.navigateToSchedule) {
            suppressMainContent = false
        }
    }

    LaunchedEffect(state.navigateHome) {
        if (state.navigateHome) {
            viewModel.consumeNavigateHome()
            onNavigateHome()
        }
    }
    LaunchedEffect(state.navigateToTalentHub) {
        if (state.navigateToTalentHub) {
            suppressMainContent = true
            viewModel.consumeNavigateToTalentHub()
            onNavigateTalentHub()
        }
    }
    LaunchedEffect(state.navigateToSchedule) {
        if (state.navigateToSchedule) {
            suppressMainContent = true
            viewModel.consumeNavigateToSchedule()
            onNavigateSchedule()
        }
    }
    LaunchedEffect(state.navigateToCoach) {
        if (state.navigateToCoach) {
            viewModel.consumeNavigateToCoach()
            onNavigateCoach()
        }
    }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeSnackbar()
        }
    }

    if (!state.ready && !state.navigateHome && !state.navigateToTalentHub && !state.navigateToSchedule) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.showTeamPicker) {
        BackHandler { /* Must pick a team to continue */ }
        TeamPickerScreen(
            conferences = state.teamPickerConferences,
            onPick = viewModel::pickTeam,
        )
        return
    }

    if (suppressMainContent || state.navigateToTalentHub || state.navigateToSchedule) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                if (state.selectedTab == MainTab.HOME) {
                    Button(
                        onClick = viewModel::playWeek,
                        enabled = state.playWeekEnabled && !state.playingWeek,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        if (state.playingWeek) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(state.playWeekLabel)
                    }
                }
                NavigationBar {
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.HOME,
                        onClick = { viewModel.selectTab(MainTab.HOME) },
                        icon = { androidx.compose.material3.Icon(Icons.Default.Home, null) },
                        label = { Text("Home") },
                    )
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.TEAM,
                        onClick = { viewModel.selectTab(MainTab.TEAM) },
                        icon = { androidx.compose.material3.Icon(Icons.Default.Groups, null) },
                        label = { Text("Team") },
                    )
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.LEAGUE,
                        onClick = { viewModel.selectTab(MainTab.LEAGUE) },
                        icon = { androidx.compose.material3.Icon(Icons.Default.Leaderboard, null) },
                        label = { Text("League") },
                    )
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.AWARDS,
                        onClick = { viewModel.selectTab(MainTab.AWARDS) },
                        icon = { androidx.compose.material3.Icon(Icons.Default.EmojiEvents, null) },
                        label = { Text("Awards") },
                    )
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.MORE,
                        onClick = { viewModel.selectTab(MainTab.MORE) },
                        icon = { androidx.compose.material3.Icon(Icons.Default.MoreHoriz, null) },
                        label = { Text("More") },
                    )
                }
            }
        },
    ) { padding ->
        TabContentTransition(
            targetState = state.selectedTab,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            label = "mainTabContent",
        ) { tab ->
            when (tab) {
                MainTab.HOME -> HomePanel(state, viewModel, Modifier.fillMaxSize())
                MainTab.TEAM -> TeamPanel(state, viewModel, Modifier.fillMaxSize())
                MainTab.LEAGUE -> LeaguePanel(state, viewModel, Modifier.fillMaxSize())
                MainTab.AWARDS -> AwardsPanel(state, viewModel, Modifier.fillMaxSize())
                MainTab.MORE -> MorePanel(state, viewModel, Modifier.fillMaxSize())
            }
        }
    }

    if (state.showExitConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissExitConfirm,
            title = { Text("Exit to Main Menu?") },
            text = {
                Text(
                    "Are you sure you want to return to main menu? Any progress from the " +
                        "beginning of the season will be lost.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmExit) { Text("Yes, Exit") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExitConfirm) { Text("Cancel") }
            },
        )
    }
    if (state.showSeasonSummary) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSeasonSummary,
            title = { Text(state.seasonSummaryTitle) },
            text = {
                Text(
                    state.seasonSummaryMessage,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissSeasonSummary) { Text("OK") }
            },
        )
    }
    if (state.showInjuryDialog) {
        InjuryDialog(state.injuryLines, viewModel)
    }
    if (state.showPlayersLeavingDialog && state.playersLeavingDialog != null) {
        PlayersLeavingDialog(state.playersLeavingDialog!!, viewModel)
    }
    if (state.showRecruitingClassDialog) {
        RecruitingClassDialog(state.recruitingClassRows, viewModel::dismissRecruitingClassDialog)
    }
    if (state.showSaveDialog) {
        SaveDialog(state, viewModel)
    }
    if (state.confirmOverwriteSlot != null) {
        val slot = state.confirmOverwriteSlot!!
        AlertDialog(
            onDismissRequest = viewModel::dismissOverwriteConfirm,
            title = { Text("Overwrite Save?") },
            text = { Text("Are you sure you want to overwrite this save file?\n\n${state.saveSlotInfos[slot]}") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmOverwriteSave) { Text("Yes, Overwrite") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissOverwriteConfirm) { Text("Cancel") }
            },
        )
    }
    if (state.showRankingsDialog) {
        RankingsDialog(state, viewModel)
    }
    if (state.showLeagueHistoryDialog) {
        LeagueHistoryDialog(state, viewModel)
    }
    if (state.showTeamHistoryDialog) {
        TeamHistoryDialog(state, viewModel)
    }
    if (state.showRenameDialog && state.renameDialog != null) {
        RenameDialog(state.renameDialog!!, viewModel)
    }
    if (state.showPlayerCareer && state.playerCareer != null) {
        PlayerCareerSheet(state.playerCareer!!, viewModel::dismissPlayerCareer)
    }
    if (state.showGameDialog && state.gameDialog != null) {
        GameDetailSheet(state.gameDialog!!, viewModel)
    }
}

@Composable
private fun HomePanel(state: MainUiState, viewModel: MainViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SegmentRow(
            labels = listOf("Stats", "Roster", "Games"),
            selected = state.homeSegment.ordinal,
            onSelect = { viewModel.selectHomeSegment(HomeSegment.entries[it]) },
        )
        TabContentTransition(
            targetState = state.homeSegment,
            modifier = Modifier.fillMaxSize(),
            label = "homeSegmentContent",
        ) { segment ->
                ContentList(
                    state = state,
                    segment = segment,
                    stats = state.homeStats,
                    roster = state.homeRoster,
                    schedule = state.homeSchedule,
                    scrollToIndex = state.scrollScheduleToIndex,
                    onConsumeScroll = viewModel::consumeScrollSchedule,
                    onPlayerClick = viewModel::openPlayerCareer,
                    onGameClick = viewModel::openGameDialog,
                    onOpponentClick = { it?.let(viewModel::examineTeam) },
                    onStatClick = { mode -> viewModel.openRankingsDialog(mode) },
                )
        }
    }
}

@Composable
private fun LeaguePanel(state: MainUiState, viewModel: MainViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SpinnerDropdown(
            label = "Conference",
            options = state.confNames,
            selectedIndex = state.selectedConfIndex,
            onSelect = viewModel::selectConfIndex,
        )
        SpinnerDropdown(
            label = "Team",
            options = state.browseTeamLabels,
            selectedIndex = state.selectedBrowseTeamIndex,
            onSelect = viewModel::selectBrowseTeamIndex,
        )
        SegmentRow(
            labels = listOf("Stats", "Roster", "Games"),
            selected = state.browseSegment.ordinal,
            onSelect = { viewModel.selectBrowseSegment(BrowseSegment.entries[it]) },
        )
        TabContentTransition(
            targetState = state.browseSegment,
            modifier = Modifier.fillMaxSize(),
            label = "browseSegmentContent",
        ) { segment ->
            Column(Modifier.fillMaxSize()) {
                if (segment == BrowseSegment.ROSTER) {
                    PosFilterDropdown(state.browseRosterPosFilter, viewModel::setBrowseRosterFilter)
                }
                ContentList(
                    state = state,
                    segment = when (segment) {
                        BrowseSegment.STATS -> HomeSegment.STATS
                        BrowseSegment.ROSTER -> HomeSegment.ROSTER
                        BrowseSegment.GAMES -> HomeSegment.GAMES
                    },
                    stats = state.browseStats,
                    roster = state.browseRoster,
                    schedule = state.browseSchedule,
                    scrollToIndex = null,
                    onConsumeScroll = {},
                    onPlayerClick = viewModel::openPlayerCareer,
                    onGameClick = viewModel::openGameDialog,
                    onOpponentClick = { it?.let(viewModel::examineTeam) },
                    onStatClick = { mode -> viewModel.openRankingsDialog(mode) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepthChartPanel(state: MainUiState, viewModel: MainViewModel) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveLineupPlayer(from.index, to.index)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SpinnerDropdown(
            label = "Position",
            options = listOf(
                "QB (1 starter)", "RB (2 starters)", "FB (1 starter)", "WR (3 starters)",
                "TE (1 starter)", "OL (5 starters)", "K (1 starter)", "S (1 starter)",
                "CB (3 starters)", "EDGE (2 starters)", "DL (3 starters)", "LB (3 starters)",
                "PR (punt return)", "KR (kick return)", "Gunner 1", "Gunner 2", "LS (long snap)",
            ),
            selectedIndex = state.lineupPositionIndex,
            onSelect = viewModel::selectLineupPosition,
        )
        DepthChartToolbar(
            starterEnabled = state.lineupStarterCount > 0,
            benchEnabled = state.lineupBenchCount > 0,
            onLockStarters = { viewModel.setLineupSectionLocks(starters = true, locked = true) },
            onUnlockStarters = { viewModel.setLineupSectionLocks(starters = true, locked = false) },
            onLockBench = { viewModel.setLineupSectionLocks(starters = false, locked = true) },
            onUnlockBench = { viewModel.setLineupSectionLocks(starters = false, locked = false) },
            onSortPosition = viewModel::autoSortUnlockedLineup,
            onSortAll = viewModel::autoSortAllLineups,
            modifier = Modifier.padding(top = 8.dp),
        )
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.lineupRows, key = { it.playerKey }) { row ->
                ReorderableItem(reorderableState, key = row.playerKey) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp, label = "depthDrag")
                    DepthChartCard(
                        row = row,
                        elevation = elevation,
                        dragHandleModifier = Modifier.draggableHandle(),
                        onPlayerClick = { viewModel.openPlayerCareer(row.playerKey) },
                        onToggleLock = { viewModel.toggleLineupLock(row.playerKey) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
        Text(
            "Starters: first ${state.lineupRequired} · ${state.lineupStarterCount} starters · ${state.lineupBenchCount} backups",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun DepthChartToolbar(
    starterEnabled: Boolean,
    benchEnabled: Boolean,
    onLockStarters: () -> Unit,
    onUnlockStarters: () -> Unit,
    onLockBench: () -> Unit,
    onUnlockBench: () -> Unit,
    onSortPosition: () -> Unit,
    onSortAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(
            onClick = onSortPosition,
            modifier = Modifier.weight(1f),
        ) {
            Text("Sort Position", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalButton(
            onClick = onSortAll,
            modifier = Modifier.weight(1f),
        ) {
            Text("Sort All", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Bulk lock options",
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Lock Starters") },
                    enabled = starterEnabled,
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onLockStarters()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Unlock Starters") },
                    enabled = starterEnabled,
                    leadingIcon = {
                        Icon(Icons.Filled.LockOpen, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onUnlockStarters()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Lock Bench") },
                    enabled = benchEnabled,
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onLockBench()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Unlock Bench") },
                    enabled = benchEnabled,
                    leadingIcon = {
                        Icon(Icons.Filled.LockOpen, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onUnlockBench()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepthChartCard(
    row: LineupRowUi,
    elevation: Dp,
    dragHandleModifier: Modifier,
    onPlayerClick: () -> Unit,
    onToggleLock: () -> Unit,
) {
    val cardShape = RoundedCornerShape(12.dp)
    val surfaceAlpha = when {
        row.injured -> 1f
        row.starter -> 1f
        else -> 0.72f
    }
    val borderColor = when {
        row.injured -> MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
        row.starter -> FcOvrStarter.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }
    val surfaceColor = when {
        row.injured -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = surfaceAlpha)
    }
    Surface(
        shadowElevation = elevation,
        shape = cardShape,
        color = surfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, cardShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlayerClick)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.depthLabel,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (row.starter) FcOvrStarter else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (row.starter) FcOvrStarter.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    .padding(vertical = 6.dp),
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
            IconButton(onClick = onToggleLock) {
                Icon(
                    imageVector = if (row.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (row.locked) "Unlock player" else "Lock player",
                    tint = if (row.locked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(
                modifier = dragHandleModifier,
                onClick = {},
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


@Composable
private fun TeamPanel(state: MainUiState, viewModel: MainViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SegmentRow(
            labels = listOf("Depth Chart", "Strategy"),
            selected = state.teamSegment.ordinal,
            onSelect = { viewModel.selectTeamSegment(TeamPanelSegment.entries[it]) },
        )
        TabContentTransition(
            targetState = state.teamSegment,
            modifier = Modifier.fillMaxSize(),
            label = "teamSegmentContent",
        ) { segment ->
            when (segment) {
                TeamPanelSegment.DEPTH_CHART -> {
                    DepthChartPanel(state = state, viewModel = viewModel)
                }
                TeamPanelSegment.STRATEGY -> {
                    Column(modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "SYSTEM",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = FcPrimary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                        SpinnerDropdown(
                            label = "Offense Philosophy",
                            options = state.offPhilosophyNames,
                            selectedIndex = state.offPhilosophyIndex,
                            onSelect = viewModel::setOffPhilosophy,
                        )
                        SpinnerDropdown(
                            label = "Defense System",
                            options = state.defSystemNames,
                            selectedIndex = state.defSystemIndex,
                            onSelect = viewModel::setDefSystem,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AwardsPanel(state: MainUiState, viewModel: MainViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SegmentRow(
            labels = buildList {
                add("Honors")
                if (state.awardsBowlsUnlocked) add("Bowls")
            },
            selected = if (state.awardsSegment == AwardsSegment.HONORS) 0 else 1,
            onSelect = {
                viewModel.selectAwardsSegment(
                    if (it == 0) AwardsSegment.HONORS else AwardsSegment.BOWLS,
                )
            },
        )
        TabContentTransition(
            targetState = state.awardsSegment,
            modifier = Modifier.fillMaxSize(),
            label = "awardsSegmentContent",
        ) { segment ->
            when (segment) {
                AwardsSegment.HONORS -> {
                    if (state.awardCategories.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.awardCategories.forEachIndexed { i, cat ->
                                FilterChip(
                                    selected = state.selectedAwardCategory == i,
                                    onClick = { viewModel.selectAwardCategory(i) },
                                    label = { Text(cat) },
                                )
                            }
                        }
                    }
                    state.potyHeader?.let { header ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp),
                        ) {
                            Text(header, fontWeight = FontWeight.Bold)
                            state.potySubhead?.let { Text(it) }
                            state.potyStats?.let { Text(it) }
                        }
                    }
                    Text(
                        state.awardsSectionLabel,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.awardRows) { row ->
                            AwardCard(row)
                        }
                    }
                }
                AwardsSegment.BOWLS -> {
                    SpinnerDropdown(
                        label = "View",
                        options = state.bowlSpinnerOptions,
                        selectedIndex = state.selectedBowlOption,
                        onSelect = viewModel::selectBowlOption,
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.bowlRows) { bowl ->
                            BowlCard(bowl)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MorePanel(state: MainUiState, viewModel: MainViewModel, modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MoreButton("Save League", viewModel::openSaveDialog)
        MoreButton("Schedule & Contracts", viewModel::openScheduleScreen)
        MoreButton("League History / Records", viewModel::openLeagueHistoryDialog)
        MoreButton("Team History", viewModel::openTeamHistoryDialog)
        MoreButton("Team Rankings", viewModel::openRankingsDialog)
        MoreButton("Settings / Change Name", viewModel::openRenameDialog)
        MoreButton("Exit to Main Menu", viewModel::requestExit)
    }
}

@Composable
private fun MoreButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

@Composable
private fun SegmentRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SegmentedControl(
        labels = labels,
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpinnerDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    if (options.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val idx = selectedIndex.coerceIn(options.indices)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options[idx],
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .padding(vertical = 4.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(i)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosFilterDropdown(filter: String, onSelect: (String) -> Unit) {
    SpinnerDropdown(
        label = "Position",
        options = ROSTER_FILTERS,
        selectedIndex = ROSTER_FILTERS.indexOf(filter).coerceAtLeast(0),
        onSelect = { onSelect(ROSTER_FILTERS[it]) },
    )
}

@Composable
private fun ContentList(
    state: MainUiState,
    segment: HomeSegment,
    stats: List<StatRowUi>,
    roster: List<RosterRowUi>,
    schedule: List<ScheduleRowUi>,
    scrollToIndex: Int?,
    onConsumeScroll: () -> Unit,
    onPlayerClick: (Int) -> Unit,
    onGameClick: (Int) -> Unit,
    onOpponentClick: (String?) -> Unit,
    onStatClick: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToIndex, segment, schedule.size) {
        scrollToIndex?.let { scheduleIndex ->
            if (segment == HomeSegment.GAMES && schedule.isNotEmpty()) {
                // Hero occupies index 0; schedule rows start at 1.
                listState.scrollToItem(scheduleIndex + 1)
            } else {
                listState.scrollToItem(scheduleIndex)
            }
            onConsumeScroll()
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (segment) {
            HomeSegment.STATS -> teamStatsItems(stats, onStatClick)
            HomeSegment.ROSTER -> rosterItems(roster, onPlayerClick)
            HomeSegment.GAMES -> scheduleItems(schedule, onGameClick, onOpponentClick)
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

private fun LazyListScope.scheduleItems(
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

private fun LazyListScope.rosterItems(
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
                color = FcPrimary,
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

private fun LazyListScope.teamStatsItems(
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
                color = FcPrimary,
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
                    listOf(FcPrimaryDark.copy(alpha = 0.55f), MaterialTheme.colorScheme.surfaceVariant),
                ),
            )
            .border(1.dp, FcPrimary.copy(alpha = 0.4f), StatCardShape)
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
    val rankColors = rankAccent(row.rankNum)
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

private data class RankAccent(val bg: Color, val fg: Color, val border: Color)

private fun rankAccent(rankNum: Int): RankAccent = when {
    rankNum <= 3 -> RankAccent(
        bg = Color(0xFF3E2E00),
        fg = FcOvrElite,
        border = FcOvrElite.copy(alpha = 0.55f),
    )
    rankNum <= 10 -> RankAccent(
        bg = Color(0xFF00332E),
        fg = FcPrimary,
        border = FcPrimary.copy(alpha = 0.45f),
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
                    listOf(FcPrimaryDark.copy(alpha = 0.55f), MaterialTheme.colorScheme.surfaceVariant),
                ),
            )
            .border(1.dp, FcPrimary.copy(alpha = 0.4f), StatCardShape)
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
private fun RosterMetricTile(
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
private fun OvrPotBadge(
    ovr: Int,
    potGrade: String,
    modifier: Modifier = Modifier,
) {
    val circleSize = 36.dp
    // Light overlap (~22%) so the pot letter stays fully readable/centered.
    val overlapOffset = 28.dp
    Box(modifier = modifier.size(width = circleSize + overlapOffset, height = circleSize)) {
        // Potential sits behind and peeks out to the right.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = overlapOffset)
                .size(circleSize)
                .clip(CircleShape)
                .background(gradeColorBg(potGrade))
                .border(1.dp, gradeColor(potGrade).copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = potGrade,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = gradeColor(potGrade),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        // Overall sits in front on the left.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(circleSize)
                .clip(CircleShape)
                .background(ovrColor(ovr))
                .border(1.5.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = ovr.toString(),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RosterMetaChip(label: String) {
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
private fun RosterInjuryChip(label: String) {
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

@Composable
private fun ScheduleSnapshotHero(schedule: List<ScheduleRowUi>) {
    val playedRows = schedule.filter { it.played }
    val wins = playedRows.count { it.isWin == true }
    val losses = playedRows.count { it.isLoss == true }
    val remaining = schedule.count { !it.played }
    val nextOpponent = schedule.firstOrNull { !it.played }?.opponentLabel ?: "Season done"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StatCardShape)
            .background(
                Brush.verticalGradient(
                    listOf(FcPrimaryDark.copy(alpha = 0.55f), MaterialTheme.colorScheme.surfaceVariant),
                ),
            )
            .border(1.dp, FcPrimary.copy(alpha = 0.4f), StatCardShape)
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
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(rowBrush)
            .border(1.dp, borderColor, cardShape)
            .clickable { onGameClick(row.gameKey) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
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
private fun AwardCard(row: AwardRowUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (row.highlightUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .padding(12.dp),
    ) {
        row.lines.forEach { Text(it) }
    }
}

@Composable
private fun BowlCard(bowl: BowlRowUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        Text(bowl.name, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(bowl.away)
            Text(bowl.score, fontWeight = FontWeight.Bold)
            Text(bowl.home)
        }
    }
}

@Composable
private fun TeamPickerScreen(
    conferences: List<TeamPickerConfUi>,
    onPick: (Int) -> Unit,
) {
    if (conferences.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    var selectedConfIndex by remember { mutableIntStateOf(0) }
    var teamIndex by remember { mutableIntStateOf(0) }
    var slideForward by remember { mutableStateOf(true) }
    val confIndex = selectedConfIndex.coerceIn(conferences.indices)
    val selectedConf = conferences[confIndex]
    val currentTeam = selectedConf.teams.getOrNull(teamIndex.coerceIn(0, (selectedConf.teams.size - 1).coerceAtLeast(0)))
    val teamColors = rememberTeamColors(currentTeam?.name, currentTeam?.abbr ?: "TEAM")
    val animPrimary by animateColorAsState(
        targetValue = teamColors.primary,
        animationSpec = tween(350),
        label = "pickerPrimary",
    )
    val animSecondary by animateColorAsState(
        targetValue = teamColors.secondary,
        animationSpec = tween(350),
        label = "pickerSecondary",
    )
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            animPrimary,
            animSecondary.copy(alpha = 0.95f),
            Color(0xFF0A0A0A),
        ),
    )
    val swipeThresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    fun goToTeam(nextIndex: Int, forward: Boolean) {
        if (nextIndex !in selectedConf.teams.indices || nextIndex == teamIndex) return
        slideForward = forward
        teamIndex = nextIndex
    }

    LaunchedEffect(confIndex) {
        slideForward = true
        teamIndex = 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Choose your team",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(conferences) { index, conf ->
                    val selected = index == confIndex
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (selected) Color.White.copy(alpha = 0.24f)
                                else Color.Black.copy(alpha = 0.32f),
                            )
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) Color.White.copy(alpha = 0.9f)
                                else Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(22.dp),
                            )
                            .clickable { selectedConfIndex = index }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ConferenceLogo(conferenceName = conf.name, size = 24.dp)
                        Text(
                            text = conf.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (selectedConf.teams.isEmpty() || currentTeam == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No teams in this conference.", color = Color.White)
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { goToTeam(teamIndex - 1, forward = false) },
                        enabled = teamIndex > 0,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous team",
                            tint = Color.White.copy(
                                alpha = if (teamIndex > 0) 0.95f else 0.3f,
                            ),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(confIndex, selectedConf.teams.size, teamIndex) {
                                detectHorizontalDragGestures(
                                    onDragStart = { dragAccum = 0f },
                                    onDragEnd = {
                                        when {
                                            dragAccum <= -swipeThresholdPx ->
                                                goToTeam(teamIndex + 1, forward = true)
                                            dragAccum >= swipeThresholdPx ->
                                                goToTeam(teamIndex - 1, forward = false)
                                        }
                                        dragAccum = 0f
                                    },
                                    onDragCancel = { dragAccum = 0f },
                                    onHorizontalDrag = { _, amount -> dragAccum += amount },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = currentTeam,
                            transitionSpec = {
                                val enter = fadeIn(tween(240)) + slideInHorizontally(tween(280)) {
                                    if (slideForward) it / 4 else -it / 4
                                }
                                val exit = fadeOut(tween(180)) + slideOutHorizontally(tween(220)) {
                                    if (slideForward) -it / 4 else it / 4
                                }
                                (enter togetherWith exit).using(SizeTransform(clip = false))
                            },
                            label = "teamLogoSwap",
                            contentAlignment = Alignment.Center,
                        ) { team ->
                            val teamBoostBg = rememberTeamColors(team.name, team.abbr).primary
                            val needsContrastBoost = rememberLogoNeedsContrastBoost(
                                teamName = team.name,
                                background = teamBoostBg,
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                TeamLogo(
                                    teamName = team.name,
                                    abbr = team.abbr,
                                    size = 200.dp,
                                    framed = false,
                                    contrastBoost = needsContrastBoost,
                                )
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = team.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${team.abbr} · Prestige ${team.prestige}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { goToTeam(teamIndex + 1, forward = true) },
                        enabled = teamIndex < selectedConf.teams.lastIndex,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next team",
                            tint = Color.White.copy(
                                alpha = if (teamIndex < selectedConf.teams.lastIndex) {
                                    0.95f
                                } else {
                                    0.3f
                                },
                            ),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val pageCount = selectedConf.teams.size
                    val maxDots = 11
                    val current = teamIndex
                    val windowStart = when {
                        pageCount <= maxDots -> 0
                        current <= maxDots / 2 -> 0
                        current >= pageCount - (maxDots / 2) -> pageCount - maxDots
                        else -> current - maxDots / 2
                    }
                    val windowEnd = (windowStart + maxDots).coerceAtMost(pageCount)
                    for (i in windowStart until windowEnd) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (i == current) 9.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == current) Color.White
                                    else Color.White.copy(alpha = 0.35f),
                                ),
                        )
                    }
                }
            }

            Button(
                onClick = { if (currentTeam != null) onPick(currentTeam.teamListIndex) },
                enabled = currentTeam != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.4f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Select", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun InjuryDialog(lines: List<String>, viewModel: MainViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismissInjuryDialog,
        title = { Text("Injury Report") },
        text = {
            Column {
                lines.forEach { Text(it) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = true, onCheckedChange = viewModel::setInjuryReportEnabled)
                    Text("Show injury reports")
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::dismissInjuryDialog) { Text("OK") } },
        dismissButton = { TextButton(onClick = viewModel::goToLineupFromInjury) { Text("Set Lineup") } },
    )
}

@Composable
private fun PlayersLeavingDialog(dialog: PlayersLeavingDialogUi, viewModel: MainViewModel) {
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
private fun RecruitingClassDialog(rows: List<RankingRowUi>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recruiting Class Rankings") },
        text = {
            LazyColumn(modifier = Modifier.height(280.dp)) {
                items(rows) { row -> Text(row.line, modifier = Modifier.padding(vertical = 2.dp)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
private fun SaveDialog(state: MainUiState, viewModel: MainViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismissSaveDialog,
        title = { Text("Choose Save File to Overwrite:") },
        text = {
            LazyColumn {
                items(state.saveSlotInfos.size) { i ->
                    val info = state.saveSlotInfos[i]
                    TextButton(
                        onClick = { viewModel.saveToSlot(i) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (info == "EMPTY") "Slot $i — EMPTY" else info) }
                }
            }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissSaveDialog) { Text("Cancel") } },
        confirmButton = {},
    )
}

@Composable
private fun RankingsDialog(state: MainUiState, viewModel: MainViewModel) {
    SimpleListDialog(
        title = "Team Rankings",
        modeLabels = arrayOf(
            "Poll Votes", "Conference Standings", "Strength of Sched", "Points Per Game",
            "Opp Points Per Game", "Yards Per Game", "Opp Yards Per Game", "Pass Yards Per Game",
            "Rush Yards Per Game", "Opp Pass YPG", "Opp Rush YPG", "TO Differential",
            "Off Talent", "Def Talent", "Prestige", "Recruiting Class",
        ),
        modeIndex = state.rankingsModeIndex,
        onModeChange = viewModel::setRankingsMode,
        lines = state.rankingsRows.map { it.line },
        onLineClick = viewModel::examineTeamFromRankingLine,
        onDismiss = viewModel::dismissRankingsDialog,
    )
}

@Composable
private fun LeagueHistoryDialog(state: MainUiState, viewModel: MainViewModel) {
    SimpleListDialog(
        title = "League History / Records",
        modeLabels = arrayOf("League History", "League Records"),
        modeIndex = state.leagueHistoryModeIndex,
        onModeChange = viewModel::setLeagueHistoryMode,
        lines = state.leagueHistoryRows.map { it.text },
        onLineClick = {},
        onDismiss = viewModel::dismissLeagueHistoryDialog,
    )
}

@Composable
private fun TeamHistoryDialog(state: MainUiState, viewModel: MainViewModel) {
    SimpleListDialog(
        title = "Team History",
        modeLabels = arrayOf("Team History", "Team Records", "Hall of Fame"),
        modeIndex = state.teamHistoryModeIndex,
        onModeChange = viewModel::setTeamHistoryMode,
        lines = state.teamHistoryRows.map { it.text },
        onLineClick = {},
        onDismiss = viewModel::dismissTeamHistoryDialog,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleListDialog(
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
private fun RenameDialog(dialog: RenameDialogUi, viewModel: MainViewModel) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameDetailSheet(dialog: GameDialogUi, viewModel: MainViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cardShape = RoundedCornerShape(14.dp)
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissGameDialog,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = dialog.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (dialog.played) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, FcPrimary.copy(alpha = 0.35f), cardShape)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                dialog.awayName.orEmpty(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                dialog.awayScore.orEmpty(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                        Text(
                            dialog.otLabel.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = FcPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                dialog.homeName.orEmpty(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                dialog.homeScore.orEmpty(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    }
                }

                Text(
                    if (dialog.played) "BOX SCORE" else "SCOUT REPORT",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = FcPrimary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            cardShape,
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        dialog.left.trimEnd(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        dialog.center.trimEnd(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        dialog.right.trimEnd(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (dialog.bottom.isNotBlank()) {
                    Text(
                        if (dialog.played) "GAME LOG" else "NOTES",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = FcPrimary,
                    )
                    Text(
                        text = dialog.bottom.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                cardShape,
                            )
                            .padding(12.dp),
                    )
                }

                if (!dialog.played && dialog.canCoach) {
                    Button(
                        onClick = { viewModel.startCoachGame(dialog.gameKey) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Coach this game")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

