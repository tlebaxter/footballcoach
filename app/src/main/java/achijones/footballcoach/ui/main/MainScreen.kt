package achijones.footballcoach.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import achijones.footballcoach.ui.components.ConferenceLogo
import achijones.footballcoach.ui.components.SegmentedControl
import achijones.footballcoach.ui.components.MainTabContentHost
import achijones.footballcoach.ui.components.TabContentTransition
import achijones.footballcoach.ui.components.TeamLogo
import achijones.footballcoach.ui.components.TeamLogoResolver
import achijones.footballcoach.ui.components.rememberLogoNeedsContrastBoost
import achijones.footballcoach.ui.components.rememberSheetFlingBlocker
import achijones.footballcoach.ui.components.rememberTeamColors
import achijones.footballcoach.ui.theme.FcChipPosBg
import achijones.footballcoach.ui.theme.FcChipPosText
import achijones.footballcoach.ui.theme.FcBye
import achijones.footballcoach.ui.theme.FcLoss
import achijones.footballcoach.ui.theme.FcOvrElite
import achijones.footballcoach.ui.theme.FcOvrStarter
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
    "ALL", "QB", "RB", "FB", "WR", "TE", "OL", "K", "P", "S", "CB", "EDGE", "DL", "LB",
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

    // Resume (including return from coach) refreshes schedule / W-L snapshots.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenEntered()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                AnimatedVisibility(
                    visible = state.selectedTab == MainTab.HOME,
                    enter = expandVertically(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                    ) + fadeIn(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                    ) + fadeOut(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                    ),
                ) {
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
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                )
                NavigationBar {
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.HOME,
                        onClick = { viewModel.selectTab(MainTab.HOME) },
                        icon = { androidx.compose.material3.Icon(Icons.Default.Home, null) },
                        label = { Text("Home") },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.TEAM,
                        onClick = { viewModel.selectTab(MainTab.TEAM) },
                        icon = { androidx.compose.material3.Icon(Icons.Default.Groups, null) },
                        label = { Text("Team") },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.LEAGUE,
                        onClick = { viewModel.selectTab(MainTab.LEAGUE) },
                        icon = { androidx.compose.material3.Icon(Icons.Default.Leaderboard, null) },
                        label = { Text("League") },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.AWARDS,
                        onClick = { viewModel.selectTab(MainTab.AWARDS) },
                        icon = { androidx.compose.material3.Icon(Icons.Default.EmojiEvents, null) },
                        label = { Text("Awards") },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = state.selectedTab == MainTab.MORE,
                        onClick = { viewModel.selectTab(MainTab.MORE) },
                        icon = { androidx.compose.material3.Icon(Icons.Default.MoreHoriz, null) },
                        label = { Text("More") },
                        colors = navItemColors,
                    )
                }
            }
        },
    ) { padding ->
        MainTabContentHost(
            selectedTab = state.selectedTab,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
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
    if (state.showInjuryDialog && state.injuryReport != null) {
        InjuryReportSheet(state.injuryReport!!, viewModel)
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
        val info = state.saveSlotInfos.getOrNull(slot)
        AlertDialog(
            onDismissRequest = viewModel::dismissOverwriteConfirm,
            title = { Text(if (info?.status == achijones.footballcoach.save.SlotStatus.CORRUPT) "Replace damaged save?" else "Overwrite Save?") },
            text = {
                Text(
                    "Are you sure you want to overwrite this save file?\n\n" +
                        (info?.let { achijones.footballcoach.ui.util.SaveSlots.label(it) } ?: ""),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmOverwriteSave) { Text("Yes, Overwrite") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissOverwriteConfirm) { Text("Cancel") }
            },
        )
    }
    state.confirmDeleteSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteSlot,
            title = { Text("Delete slot ${slot + 1}?") },
            text = { Text("This permanently removes the career in this slot.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteSlot) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteSlot) { Text("Cancel") }
            },
        )
    }
    if (state.showChooseSlotForNewCareer) {
        AlertDialog(
            onDismissRequest = viewModel::dismissChooseSlotForNewCareer,
            title = { Text("Choose a save slot") },
            text = {
                Column {
                    Text("All slots are full. Pick one to overwrite for this new career.")
                    state.saveSlotInfos.forEach { info ->
                        TextButton(
                            onClick = { viewModel.chooseSlotForNewCareer(info.index) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(achijones.footballcoach.ui.util.SaveSlots.label(info))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissChooseSlotForNewCareer) { Text("Cancel") }
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
            showConferenceLogos = true,
        )
        SpinnerDropdown(
            label = "Team",
            options = state.browseTeamOptions.map { it.label },
            selectedIndex = state.selectedBrowseTeamIndex,
            onSelect = viewModel::selectBrowseTeamIndex,
            teamLogoOptions = state.browseTeamOptions,
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
                "TE (1 starter)", "OL (5 starters)", "K (1 starter)", "P (1 starter)", "S (1 starter)",
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
                    RosterMetaChip(row.primaryPos)
                    RosterMetaChip(row.yearLabel)
                    if (row.injured && row.injuryLabel != null) {
                        RosterInjuryChip(row.injuryLabel)
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OvrPotBadge(ovr = row.posOvr, potGrade = row.potGrade)
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
                            color = MaterialTheme.colorScheme.primary,
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
                        Text(
                            "QB UNDER PRESSURE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                        Text(
                            "How your QB reacts when the pocket collapses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        state.pressureSlotLabels.forEachIndexed { slotIndex, label ->
                            val selected = state.pressureResponseIndices.getOrElse(slotIndex) { 0 }
                            SpinnerDropdown(
                                label = label,
                                options = state.pressureResponseNames,
                                selectedIndex = selected,
                                onSelect = { responseIndex ->
                                    viewModel.setPressureResponse(slotIndex, responseIndex)
                                },
                            )
                        }
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
                    Column(modifier = Modifier.fillMaxSize()) {
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (!state.potyTeamName.isNullOrBlank() && !state.potyAbbr.isNullOrBlank()) {
                                    TeamLogo(
                                        teamName = state.potyTeamName,
                                        abbr = state.potyAbbr,
                                        size = 48.dp,
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        header,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    state.potySubhead?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    state.potyStats?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            state.awardsSectionLabel,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.awardRows) { row ->
                                AwardCard(row)
                            }
                        }
                    }
                }
                AwardsSegment.BOWLS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SpinnerDropdown(
                            label = "View",
                            options = state.bowlSpinnerOptions,
                            selectedIndex = state.selectedBowlOption,
                            onSelect = viewModel::selectBowlOption,
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.bowlRows) { bowl ->
                                BowlCard(bowl)
                            }
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
        if (state.showReturnToTalentHub) {
            MoreButton("Talent Hub", viewModel::openTalentHub)
        }
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
    showConferenceLogos: Boolean = false,
    teamLogoOptions: List<BrowseTeamOptionUi>? = null,
) {
    if (options.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val idx = selectedIndex.coerceIn(options.indices)
    val selectedTeam = teamLogoOptions?.getOrNull(idx)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options[idx],
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = when {
                showConferenceLogos -> {
                    {
                        ConferenceLogo(
                            conferenceName = options[idx],
                            size = 24.dp,
                        )
                    }
                }
                selectedTeam != null -> {
                    {
                        TeamLogo(
                            teamName = selectedTeam.name,
                            abbr = selectedTeam.abbr,
                            size = 24.dp,
                        )
                    }
                }
                else -> null
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .padding(vertical = 4.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                val team = teamLogoOptions?.getOrNull(i)
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            when {
                                showConferenceLogos -> {
                                    Box(
                                        modifier = Modifier.size(24.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        ConferenceLogo(conferenceName = opt, size = 24.dp)
                                    }
                                }
                                team != null -> {
                                    TeamLogo(
                                        teamName = team.name,
                                        abbr = team.abbr,
                                        size = 24.dp,
                                    )
                                }
                            }
                            Text(opt)
                        }
                    },
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

private data class RankAccent(val bg: Color, val fg: Color, val border: Color)

private fun rankAccent(rankNum: Int, brandPrimary: Color): RankAccent = when {
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
                .background(gradeColorBg(potGrade, MaterialTheme.colorScheme.primaryContainer))
                .border(
                    1.dp,
                    gradeColor(potGrade, MaterialTheme.colorScheme.primary).copy(alpha = 0.45f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = potGrade,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = gradeColor(potGrade, MaterialTheme.colorScheme.primary),
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

@Composable
private fun AwardCard(row: AwardRowUi) {
    if (row.isMessage) {
        Text(
            text = row.title.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        )
        return
    }

    val cardShape = RoundedCornerShape(12.dp)
    val accents = rankAccent(row.rankNum, MaterialTheme.colorScheme.primary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                if (row.highlightUser) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .border(
                1.dp,
                if (row.highlightUser) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                },
                cardShape,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!row.rankLabel.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accents.bg)
                    .border(1.dp, accents.border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.rankLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accents.fg,
                )
            }
        }
        if (!row.teamName.isNullOrBlank() && !row.abbr.isNullOrBlank()) {
            TeamLogo(teamName = row.teamName, abbr = row.abbr, size = 40.dp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val headline = when {
                !row.playerName.isNullOrBlank() -> {
                    listOfNotNull(row.position, row.playerName).joinToString(" ")
                }
                !row.title.isNullOrBlank() -> row.title
                else -> row.abbr.orEmpty()
            }
            Text(
                text = headline,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(
                row.abbr?.let { ab ->
                    buildString {
                        append(ab)
                        row.yearLabel?.let { append(" · $it") }
                    }
                },
                row.metaLine,
                row.subtitle,
            ).filter { it.isNotBlank() }
            if (meta.isNotEmpty()) {
                Text(
                    text = meta.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            row.statsLine?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BowlCard(bowl: BowlRowUi) {
    val cardShape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                if (bowl.isUserInvolved) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .border(
                1.dp,
                if (bowl.isUserInvolved) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                },
                cardShape,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = bowl.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BowlSide(
                name = bowl.awayName,
                abbr = bowl.awayAbbr,
                rank = bowl.awayRank,
                record = bowl.awayRecord,
                modifier = Modifier.weight(1f),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    text = bowl.score,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (bowl.played) "FINAL" else "PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BowlSide(
                name = bowl.homeName,
                abbr = bowl.homeAbbr,
                rank = bowl.homeRank,
                record = bowl.homeRecord,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BowlSide(
    name: String?,
    abbr: String,
    rank: Int?,
    record: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TeamLogo(
            teamName = name,
            abbr = abbr,
            size = 44.dp,
        )
        Text(
            text = abbr,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val meta = buildList {
            rank?.let { add("#$it") }
            record?.let { add("($it)") }
        }.joinToString(" ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TeamPickerStatChip(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .widthIn(min = 76.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.Black.copy(alpha = 0.38f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.72f),
            letterSpacing = 0.8.sp,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamPickerProgramSheet(
    team: TeamPickerTeamUi,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rows = listOf(
        "Program Power" to team.programPower.toString(),
        "Tradition" to team.tradition.toString(),
        "Fans" to team.fanbase.toString(),
        "Donors" to team.donors.toString(),
        "Footprint" to team.footprint.toString(),
        "NFL Pipeline" to team.pipeline.toString(),
        "Momentum" to team.momentum.toString(),
        "Purse" to team.purse,
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = team.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Program attributes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamPickerScreen(
    conferences: List<TeamPickerConfUi>,
    onPick: (String) -> Unit,
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
    var showProgramSheet by remember { mutableStateOf(false) }
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
        val size = selectedConf.teams.size
        if (size == 0) return
        val wrapped = ((nextIndex % size) + size) % size
        if (wrapped == teamIndex) return
        slideForward = forward
        teamIndex = wrapped
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
        val context = LocalContext.current
        val logoSize = 200.dp
        val logoTextGap = 20.dp
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 20.dp, bottom = 16.dp),
        ) {
            val watermarkSize = maxOf(maxWidth, maxHeight) * 0.92f
            AnimatedContent(
                targetState = currentTeam?.name,
                transitionSpec = {
                    fadeIn(tween(320)) togetherWith fadeOut(tween(220))
                },
                label = "teamWatermark",
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { teamName ->
                val watermark = remember(teamName) {
                    if (teamName.isNullOrBlank()) null
                    else TeamLogoResolver.loadTeam(context, teamName)
                }
                if (watermark != null) {
                    Image(
                        bitmap = watermark,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(watermarkSize)
                            .graphicsLayer { alpha = 0.14f },
                    )
                }
            }
            if (selectedConf.teams.isEmpty() || currentTeam == null) {
                Text(
                    text = "No teams in this conference.",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val canCycleTeams = selectedConf.teams.size > 1
                    IconButton(
                        onClick = { goToTeam(teamIndex - 1, forward = false) },
                        enabled = canCycleTeams,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous team",
                            tint = Color.White.copy(
                                alpha = if (canCycleTeams) 0.95f else 0.3f,
                            ),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
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
                            TeamLogo(
                                teamName = team.name,
                                abbr = team.abbr,
                                size = logoSize,
                                framed = false,
                                contrastBoost = needsContrastBoost,
                            )
                        }
                    }
                    IconButton(
                        onClick = { goToTeam(teamIndex + 1, forward = true) },
                        enabled = canCycleTeams,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next team",
                            tint = Color.White.copy(
                                alpha = if (canCycleTeams) 0.95f else 0.3f,
                            ),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                AnimatedContent(
                    targetState = currentTeam,
                    transitionSpec = {
                        fadeIn(tween(240)) togetherWith fadeOut(tween(180))
                    },
                    label = "teamTextSwap",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = maxHeight / 2 + logoSize / 2 + logoTextGap),
                    contentAlignment = Alignment.TopCenter,
                ) { team ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = team.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TeamPickerStatChip(
                                    label = "POWER",
                                    value = team.programPower.toString(),
                                    onClick = { showProgramSheet = true },
                                )
                                TeamPickerStatChip(label = "OFF", value = team.offTalent.toString())
                                TeamPickerStatChip(label = "DEF", value = team.defTalent.toString())
                                TeamPickerStatChip(label = "ST", value = team.stTalent.toString())
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
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
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (currentTeam != null && selectedConf.teams.isNotEmpty()) {
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
                    onClick = { if (currentTeam != null) onPick(currentTeam.abbr) },
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

    if (showProgramSheet && currentTeam != null) {
        TeamPickerProgramSheet(
            team = currentTeam,
            onDismiss = { showProgramSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InjuryReportSheet(report: InjuryReportUi, viewModel: MainViewModel) {
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
private fun SaveDialog(state: MainUiState, viewModel: MainViewModel) {
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
private fun RankingsDialog(state: MainUiState, viewModel: MainViewModel) {
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
private fun LeagueHistoryDialog(state: MainUiState, viewModel: MainViewModel) {
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
private fun TeamHistoryDialog(state: MainUiState, viewModel: MainViewModel) {
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
    val awayScout = dialog.awayScout
    val homeScout = dialog.homeScout
    val awayBox = dialog.awayBox
    val homeBox = dialog.homeBox
    val showScoutUi = !dialog.played && awayScout != null && homeScout != null
    val showResultUi = dialog.played && awayBox != null && homeBox != null
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
            if (!showScoutUi && !showResultUi) {
                Text(
                    text = dialog.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(rememberSheetFlingBlocker())
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    showScoutUi && awayScout != null && homeScout != null -> {
                        GameScoutMatchupHeader(
                            away = awayScout,
                            home = homeScout,
                            gameName = dialog.gameName.ifBlank { dialog.title },
                            rivalryLabel = dialog.rivalryLabel,
                        )
                        Text(
                            "SCOUT REPORT",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GameScoutTeamCard(team = awayScout, modifier = Modifier.weight(1f))
                            GameScoutTeamCard(team = homeScout, modifier = Modifier.weight(1f))
                        }
                    }

                    showResultUi && awayBox != null && homeBox != null -> {
                        GameResultMatchupHeader(
                            away = awayBox,
                            home = homeBox,
                            gameName = dialog.gameName.ifBlank { dialog.title },
                            otLabel = dialog.otLabel,
                            awayWon = dialog.awayWon,
                            rivalryLabel = dialog.rivalryLabel,
                        )
                        Text(
                            "BOX SCORE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GameBoxTeamCard(
                                team = awayBox,
                                won = dialog.awayWon == true,
                                modifier = Modifier.weight(1f),
                            )
                            GameBoxTeamCard(
                                team = homeBox,
                                won = dialog.awayWon == false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    else -> {
                        if (dialog.played) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        cardShape,
                                    )
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
                                    color = MaterialTheme.colorScheme.primary,
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
                            color = MaterialTheme.colorScheme.primary,
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
                    }
                }

                when {
                    showResultUi && awayBox != null && homeBox != null -> {
                        Text(
                            "SCHEMES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        GameSchemesCard(away = awayBox, home = homeBox)
                        if (dialog.gameLogLines.isNotEmpty()) {
                            Text(
                                "GAME LOG",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            GameLogCard(lines = dialog.gameLogLines)
                        }
                    }

                    !showResultUi && dialog.bottom.isNotBlank() -> {
                        Text(
                            if (dialog.played) "GAME LOG" else "NOTES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
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
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    cardShape,
                                )
                                .padding(12.dp),
                        )
                    }
                }

                if (!dialog.played && dialog.canCoach) {
                    Button(
                        onClick = { viewModel.startCoachGame(dialog.gameKey) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            "Coach this game",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GameResultMatchupHeader(
    away: GameBoxTeamUi,
    home: GameBoxTeamUi,
    gameName: String,
    otLabel: String?,
    awayWon: Boolean?,
    rivalryLabel: String?,
) {
    val awayColors = rememberTeamColors(away.name, away.abbr)
    val homeColors = rememberTeamColors(home.name, home.abbr)
    val cardShape = RoundedCornerShape(14.dp)
    val headerBrush = when (awayWon) {
        true -> Brush.horizontalGradient(
            listOf(
                FcWin.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.surfaceVariant,
                FcLoss.copy(alpha = 0.35f),
            ),
        )
        false -> Brush.horizontalGradient(
            listOf(
                FcLoss.copy(alpha = 0.35f),
                MaterialTheme.colorScheme.surfaceVariant,
                FcWin.copy(alpha = 0.55f),
            ),
        )
        null -> Brush.horizontalGradient(
            listOf(
                awayColors.primary.copy(alpha = 0.42f),
                MaterialTheme.colorScheme.surfaceVariant,
                homeColors.primary.copy(alpha = 0.42f),
            ),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(headerBrush)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), cardShape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameResultHeaderTeam(
                team = away,
                won = awayWon == true,
                modifier = Modifier.weight(1f),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    text = otLabel.takeUnless { it.isNullOrBlank() } ?: "@",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "FINAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GameResultHeaderTeam(
                team = home,
                won = awayWon == false,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = gameName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (rivalryLabel != null) {
            Text(
                text = rivalryLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun GameResultHeaderTeam(
    team: GameBoxTeamUi,
    won: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TeamLogo(
            teamName = team.name,
            abbr = team.abbr,
            size = 52.dp,
        )
        Text(
            text = team.abbr,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "(${team.record})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = team.score.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (won) {
                Color(0xFF81C784)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun GameBoxTeamCard(
    team: GameBoxTeamUi,
    won: Boolean,
    modifier: Modifier = Modifier,
) {
    val teamColors = rememberTeamColors(team.name, team.abbr)
    val cardShape = RoundedCornerShape(14.dp)
    val accent = if (won) FcWin else teamColors.primary
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, accent.copy(alpha = 0.45f), cardShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(accent),
        )
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = team.abbr,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (won) {
                    Text(
                        text = "W",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GameScoutMetricTile(
                    label = "PASS",
                    value = team.passYards.toString(),
                    modifier = Modifier.weight(1f),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                )
                GameScoutMetricTile(
                    label = "RUSH",
                    value = team.rushYards.toString(),
                    modifier = Modifier.weight(1f),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                )
                GameScoutMetricTile(
                    label = "TO",
                    value = team.turnovers.toString(),
                    modifier = Modifier.weight(1f),
                    valueColor = if (team.turnovers >= 3) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun GameSchemesCard(
    away: GameBoxTeamUi,
    home: GameBoxTeamUi,
) {
    val cardShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), cardShape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GameSchemeTeamColumn(team = away, modifier = Modifier.weight(1f))
        GameSchemeTeamColumn(team = home, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GameSchemeTeamColumn(
    team: GameBoxTeamUi,
    modifier: Modifier = Modifier,
) {
    val teamColors = rememberTeamColors(team.name, team.abbr)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, teamColors.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = team.abbr,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        GameScoutMetaLine(label = "OFF", value = team.offPhilosophy)
        GameScoutMetaLine(label = "DEF", value = team.defSystem)
    }
}

@Composable
private fun GameLogCard(lines: List<String>) {
    val cardShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), cardShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        lines.forEach { line ->
            val kind = gameLogLineKind(line)
            Text(
                text = line,
                style = when (kind) {
                    GameLogLineKind.MARKER -> MaterialTheme.typography.labelMedium
                    GameLogLineKind.SCORE -> MaterialTheme.typography.bodySmall
                    GameLogLineKind.META -> MaterialTheme.typography.labelSmall
                    GameLogLineKind.NORMAL -> MaterialTheme.typography.bodySmall
                },
                fontWeight = when (kind) {
                    GameLogLineKind.MARKER, GameLogLineKind.SCORE -> FontWeight.SemiBold
                    else -> FontWeight.Normal
                },
                color = when (kind) {
                    GameLogLineKind.MARKER -> MaterialTheme.colorScheme.primary
                    GameLogLineKind.SCORE -> MaterialTheme.colorScheme.onSurface
                    GameLogLineKind.META -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    GameLogLineKind.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

private enum class GameLogLineKind {
    META,
    MARKER,
    SCORE,
    NORMAL,
}

private fun gameLogLineKind(line: String): GameLogLineKind {
    val trimmed = line.trim()
    if (trimmed.startsWith("LOG:") || trimmed.startsWith("---") || trimmed.startsWith("====")) {
        return GameLogLineKind.META
    }
    if (Regex("""^\dQ\s+\d+:\d+""").containsMatchIn(trimmed) ||
        trimmed.equals("FINAL", ignoreCase = true) ||
        trimmed.contains("Time has expired", ignoreCase = true)
    ) {
        return GameLogLineKind.MARKER
    }
    if (trimmed.contains("TOUCHDOWN", ignoreCase = true) ||
        trimmed.contains(" TD", ignoreCase = true) ||
        trimmed.contains("field goal", ignoreCase = true) ||
        trimmed.contains(" FG", ignoreCase = true) ||
        trimmed.contains("safety", ignoreCase = true)
    ) {
        return GameLogLineKind.SCORE
    }
    return GameLogLineKind.NORMAL
}

@Composable
private fun GameScoutMatchupHeader(
    away: GameScoutTeamUi,
    home: GameScoutTeamUi,
    gameName: String,
    rivalryLabel: String?,
) {
    val awayColors = rememberTeamColors(away.name, away.abbr)
    val homeColors = rememberTeamColors(home.name, home.abbr)
    val cardShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        awayColors.primary.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.surfaceVariant,
                        homeColors.primary.copy(alpha = 0.42f),
                    ),
                ),
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), cardShape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameScoutHeaderTeam(
                team = away,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "@",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            GameScoutHeaderTeam(
                team = home,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = gameName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (rivalryLabel != null) {
            Text(
                text = rivalryLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun GameScoutHeaderTeam(
    team: GameScoutTeamUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TeamLogo(
            teamName = team.name,
            abbr = team.abbr,
            size = 56.dp,
        )
        Text(
            text = team.abbr,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "#${team.rank}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GameScoutTeamCard(
    team: GameScoutTeamUi,
    modifier: Modifier = Modifier,
) {
    val teamColors = rememberTeamColors(team.name, team.abbr)
    val cardShape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, teamColors.primary.copy(alpha = 0.45f), cardShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(teamColors.primary),
        )
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = team.abbr,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            GameScoutMetaLine(label = "OFF", value = team.offPhilosophy)
            GameScoutMetaLine(label = "DEF", value = team.defSystem)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GameScoutMetricTile("OFF", team.offTalent.toString(), Modifier.weight(1f))
                GameScoutMetricTile("DEF", team.defTalent.toString(), Modifier.weight(1f))
                GameScoutMetricTile("PWR", team.programPower.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GameScoutMetaLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GameScoutMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = ovrColor(value.toIntOrNull() ?: 0),
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

